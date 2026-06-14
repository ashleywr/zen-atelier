package com.sanhiruzu.atelier.space;

import com.sanhiruzu.atelier.Config;
import com.sanhiruzu.atelier.space.analyze.AnalysisResult;
import com.sanhiruzu.atelier.space.analyze.CandidateAssembler;
import com.sanhiruzu.atelier.space.analyze.CandidateDecision;
import com.sanhiruzu.atelier.space.analyze.CandidateResolver;
import com.sanhiruzu.atelier.space.analyze.ChunkAnalyzer;
import com.sanhiruzu.atelier.space.analyze.ChunkSnapshotBundle;
import com.sanhiruzu.atelier.space.analyze.EvidenceScore;
import com.sanhiruzu.atelier.space.analyze.EvidenceScorer;
import com.sanhiruzu.atelier.space.analyze.MicroRegion;
import com.sanhiruzu.atelier.space.analyze.ZoneCandidate;
import com.sanhiruzu.atelier.space.commit.ZoneCommitter;
import com.sanhiruzu.atelier.space.commit.ZoneStore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Priority work queue for chunk classification.  Player-nearby chunks are
 * processed first.  All zone lifecycle logic lives in the new pipeline
 * ({@link ZoneCommitter} / {@link ZoneStore}); this class only owns scheduling —
 * what to classify and when.
 *
 * <h3>Async pipeline</h3>
 * <ol>
 *   <li>Snapshot chunk block data on the server thread (fast, ~1 ms).</li>
 *   <li>Submit the flood-fill to a background thread via {@code analyzePool};
 *       it runs on a {@link ChunkSnapshotBundle} and produces a
 *       {@link AnalysisResult} with no Minecraft state access.</li>
 *   <li>On the next server tick, drain completed results and commit via
 *       {@link ZoneCommitter} on the server thread.</li>
 * </ol>
 *
 * If a chunk is re-dirtied while its async task is in-flight the result is
 * discarded (snapshot version mismatch) and the chunk is re-queued.
 */
public class ClassificationScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ZenAtelier/Scheduler");

    private static final int CHUNKS_PER_TICK = 2;
    private static final int APPLY_PER_TICK = 2;
    private static final int MAX_CONCURRENT_ASYNC = 8;
    private static final int NEARBY_CHUNK_RADIUS = 4;
    private static final int DEFERRED_CHECK_INTERVAL = 20;
    private static final int QUEUE_BACKLOG_THRESHOLD = 20;
    private static final int QUEUE_CRITICAL_THRESHOLD = 50;

    private final ServerLevel level;
    private final PriorityQueue<ChunkWork> workQueue;
    private final Set<Long> queuedChunks = new HashSet<>();
    private final Set<Long> deferredChunks = new HashSet<>();
    private final Set<Long> inFlightChunks = new HashSet<>();
    private final ExecutorService analyzePool;
    private final ConcurrentLinkedQueue<PendingAnalysis> pendingAnalyses = new ConcurrentLinkedQueue<>();
    private final Map<Long, AnalysisResult> analysisCache = new HashMap<>();

    private int deferredTickCounter = 0;
    private long lastBacklogWarning = 0;
    private int lastLoggedQueueSize = 0;

    public ClassificationScheduler(ServerLevel level) {
        this.level = level;
        this.workQueue = new PriorityQueue<>();
        this.analyzePool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "ZenAtelier-ZoneAnalyzer");
            t.setDaemon(true);
            return t;
        });
    }

    public void tick() {
        if (Config.DISABLE_ZONE_SCANNING.get()) return;

        if (++deferredTickCounter >= DEFERRED_CHECK_INTERVAL) {
            deferredTickCounter = 0;
            promoteDeferredChunks();
        }

        // Always drain completed async results regardless of server load
        drainPendingAnalyses();

        if (isServerUnderLoad()) return;

        // Submit new async analysis tasks
        int submitted = 0;
        while (!workQueue.isEmpty()
                && submitted < CHUNKS_PER_TICK
                && inFlightChunks.size() < MAX_CONCURRENT_ASYNC) {

            ChunkWork work = workQueue.poll();
            long chunkKey = ChunkPos.asLong(work.chunkX, work.chunkZ);
            queuedChunks.remove(chunkKey);
            if (work.isExpired()) continue;

            ChunkAccess chunk = level.getChunkSource().getChunkNow(work.chunkX, work.chunkZ);
            if (chunk == null) {
                deferredChunks.add(chunkKey);
                continue;
            }

            ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
            if (!data.isDirty()) continue;

            submitAnalysis(work, chunk, chunkKey, data);
            submitted++;
        }
    }

    private void submitAnalysis(ChunkWork work, ChunkAccess chunk, long chunkKey,
                                ChunkClassificationData data) {
        data.setDirty(false);
        inFlightChunks.add(chunkKey);
        long snapVersion = data.getSnapshotVersion();
        ChunkSnapshotBundle bundle = ChunkSnapshotBundle.of(chunk, snapVersion);
        analyzePool.submit(() -> {
            List<MicroRegion> regions = ChunkAnalyzer.analyze(bundle);
            AnalysisResult result = new AnalysisResult(work.chunkX, work.chunkZ, bundle.snapshotVersion, regions);
            pendingAnalyses.offer(new PendingAnalysis(work.chunkX, work.chunkZ, chunkKey, result));
        });
    }

    public void scheduleChunk(int chunkX, int chunkZ) {
        long key = ChunkPos.asLong(chunkX, chunkZ);
        if (inFlightChunks.contains(key)) return; // already being processed
        if (!queuedChunks.add(key)) return;
        double priority = getPlayerDistance(chunkX, chunkZ);
        workQueue.offer(new ChunkWork(chunkX, chunkZ, priority));
    }

    public void deferChunk(int chunkX, int chunkZ) {
        deferredChunks.add(ChunkPos.asLong(chunkX, chunkZ));
    }

    public void removeChunk(int chunkX, int chunkZ) {
        long key = ChunkPos.asLong(chunkX, chunkZ);
        if (queuedChunks.remove(key)) {
            workQueue.removeIf(w -> ChunkPos.asLong(w.chunkX, w.chunkZ) == key);
        }
        deferredChunks.remove(key);
    }

    public boolean isNearAnyPlayer(int chunkX, int chunkZ) {
        return getPlayerDistance(chunkX, chunkZ) <= NEARBY_CHUNK_RADIUS;
    }

    public int getQueueSize() {
        return workQueue.size();
    }

    public int getDeferredCount() {
        return deferredChunks.size();
    }

    public void shutdown() {
        analyzePool.shutdownNow();
    }

    private void scheduleLoadedDirtyNeighbors(int chunkX, int chunkZ) {
        scheduleLoadedDirtyChunk(chunkX - 1, chunkZ);
        scheduleLoadedDirtyChunk(chunkX + 1, chunkZ);
        scheduleLoadedDirtyChunk(chunkX, chunkZ - 1);
        scheduleLoadedDirtyChunk(chunkX, chunkZ + 1);
    }

    private void scheduleLoadedDirtyChunk(int chunkX, int chunkZ) {
        if (!isNearAnyPlayer(chunkX, chunkZ)) return;
        ChunkAccess neighbor = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (neighbor == null) return;
        ChunkClassificationData data = ChunkClassificationAttachment.get(neighbor);
        if (data.isDirty()) {
            scheduleChunk(chunkX, chunkZ);
        }
    }

    private void promoteDeferredChunks() {
        if (deferredChunks.isEmpty() || level.players().isEmpty()) return;
        Iterator<Long> it = deferredChunks.iterator();
        while (it.hasNext()) {
            long key = it.next();
            int cx = ChunkPos.getX(key);
            int cz = ChunkPos.getZ(key);
            if (isNearAnyPlayer(cx, cz)) {
                it.remove();
                scheduleChunk(cx, cz);
            }
        }
    }

    private double getPlayerDistance(int chunkX, int chunkZ) {
        double minDist = Double.MAX_VALUE;
        for (Player player : level.players()) {
            int pChunkX = (int) player.getX() >> 4;
            int pChunkZ = (int) player.getZ() >> 4;
            double dist = Math.hypot(chunkX - pChunkX, chunkZ - pChunkZ);
            minDist = Math.min(minDist, dist);
        }
        return minDist;
    }

    private boolean isServerUnderLoad() {
        int queueSize = workQueue.size();

        if (queueSize > QUEUE_CRITICAL_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastBacklogWarning > 30000) {
                LOGGER.warn("Zone classification queue critical: {} chunks queued, {} deferred. "
                        + "This is normal during world load; if it persists during normal play, "
                        + "check for a chunk management mod conflict or very large structures.",
                        queueSize, deferredChunks.size());
                lastBacklogWarning = now;
            }
        } else if (queueSize > QUEUE_BACKLOG_THRESHOLD && queueSize != lastLoggedQueueSize) {
            if (queueSize % 10 == 0) {
                LOGGER.debug("Zone classification queue backlog: {} chunks", queueSize);
            }
            lastLoggedQueueSize = queueSize;
        } else if (queueSize < QUEUE_BACKLOG_THRESHOLD) {
            lastLoggedQueueSize = 0;
        }

        return level.getServer().getCurrentSmoothedTickTime() > 45.0f;
    }

    private void drainPendingAnalyses() {
        ZoneCommitter committer = ClassificationTickHandler.getCommitter(level);
        ZoneStore zoneStore = ClassificationTickHandler.getZoneStore(level);
        int applied = 0;
        while (applied < APPLY_PER_TICK) {
            PendingAnalysis pa = pendingAnalyses.poll();
            if (pa == null) break;

            inFlightChunks.remove(pa.chunkKey());

            LevelChunk chunk = level.getChunkSource().getChunkNow(pa.chunkX(), pa.chunkZ());
            if (chunk == null) continue;

            ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
            if (data.getSnapshotVersion() != pa.result().snapshotVersion()) {
                scheduleChunk(pa.chunkX(), pa.chunkZ());
                continue;
            }

            analysisCache.put(pa.chunkKey(), pa.result());

            Map<ChunkPos, List<MicroRegion>> inputs = collectNeighborInputs(pa.chunkX(), pa.chunkZ());
            if (inputs.isEmpty()) continue;

            List<ZoneCandidate> candidates = CandidateAssembler.assemble(inputs);

            for (ZoneCandidate candidate : candidates) {
                EvidenceScore score = EvidenceScorer.score(candidate);
                CandidateDecision decision = CandidateResolver.resolve(score, candidate);
                if (decision == CandidateDecision.PENDING_NEIGHBOR) continue;

                long[] walkablePositions = collectWalkablePositions(candidate, inputs);
                Map<ChunkPos, ChunkClassificationData> chunkData = collectChunkData(candidate.chunkPositions());
                UUID existingId = zoneStore.getByHash(candidate.candidateHash());

                committer.commitAccepted(candidate, decision, existingId, walkablePositions, chunkData);
            }

            scheduleLoadedDirtyNeighbors(pa.chunkX(), pa.chunkZ());
            applied++;
        }
    }

    private Map<ChunkPos, List<MicroRegion>> collectNeighborInputs(int chunkX, int chunkZ) {
        Map<ChunkPos, List<MicroRegion>> inputs = new HashMap<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 && dz != 0) continue;
                long key = ChunkPos.asLong(chunkX + dx, chunkZ + dz);
                AnalysisResult cached = analysisCache.get(key);
                if (cached != null) {
                    inputs.put(new ChunkPos(chunkX + dx, chunkZ + dz), cached.microRegions());
                }
            }
        }
        return inputs;
    }

    private long[] collectWalkablePositions(ZoneCandidate candidate,
                                            Map<ChunkPos, List<MicroRegion>> inputs) {
        List<Long> all = new ArrayList<>();
        for (List<MicroRegion> regions : inputs.values()) {
            for (MicroRegion r : regions) {
                if (candidate.memberKeys().contains(r.key())) {
                    for (long pos : r.walkablePositions()) all.add(pos);
                }
            }
        }
        return all.stream().mapToLong(Long::longValue).toArray();
    }

    private Map<ChunkPos, ChunkClassificationData> collectChunkData(Set<ChunkPos> positions) {
        Map<ChunkPos, ChunkClassificationData> result = new HashMap<>();
        for (ChunkPos pos : positions) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null) result.put(pos, ChunkClassificationAttachment.get(chunk));
        }
        return result;
    }

    private record PendingAnalysis(int chunkX, int chunkZ, long chunkKey, AnalysisResult result) {}

    private static class ChunkWork implements Comparable<ChunkWork> {
        final int chunkX;
        final int chunkZ;
        final double priority;
        final long createdAt;

        ChunkWork(int chunkX, int chunkZ, double priority) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.priority = priority;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > 60000;
        }

        @Override
        public int compareTo(ChunkWork other) {
            return Double.compare(this.priority, other.priority);
        }
    }
}
