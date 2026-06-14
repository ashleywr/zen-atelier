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
import com.sanhiruzu.atelier.space.commit.CommittedZone;
import com.sanhiruzu.atelier.space.commit.ZoneCommitter;
import com.sanhiruzu.atelier.space.commit.ZoneStore;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;

/**
 * Priority work queue for chunk classification.  Player-nearby chunks are
 * processed first.  All zone lifecycle logic lives in {@link ZoneRegistry};
 * this class only owns scheduling — what to classify and when.
 *
 * <h3>Async pipeline</h3>
 * <ol>
 *   <li>Snapshot chunk block data on the server thread (fast, ~1 ms).</li>
 *   <li>Submit the flood-fill to a single background thread; it runs on a
 *       {@link ChunkSnapshot} and produces a {@link ClassificationOutput} with
 *       no Minecraft state access.</li>
 *   <li>On the next server tick, drain completed results and call
 *       {@link ZoneRegistry#applyBootstrap} on the server thread.</li>
 * </ol>
 *
 * If a chunk is re-dirtied while its async task is in-flight the result is
 * discarded and the chunk is re-queued.
 */
public class ClassificationScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ZenAtelier/Scheduler");

    private static final int CHUNKS_PER_TICK = 2;
    private static final int APPLY_PER_TICK = 2;
    private static final int MAX_CONCURRENT_ASYNC = 8;
    private static final int NEARBY_CHUNK_RADIUS = 1;
    private static final int DEFERRED_CHECK_INTERVAL = 20;
    private static final int QUEUE_BACKLOG_THRESHOLD = 20;
    private static final int QUEUE_CRITICAL_THRESHOLD = 50;
    private static final long BOOTSTRAP_TIME_WARN_MS = 20L;

    private final ServerLevel level;
    private final PriorityQueue<ChunkWork> workQueue;
    private final Set<Long> queuedChunks = new HashSet<>();
    private final Set<Long> deferredChunks = new HashSet<>();
    private final Set<Long> inFlightChunks = new HashSet<>();
    private final ConcurrentLinkedQueue<PendingResult> pendingResults = new ConcurrentLinkedQueue<>();
    private final ExecutorService asyncPool;
    private final ExecutorService analyzePool;
    private final ConcurrentLinkedQueue<PendingAnalysis> pendingAnalyses = new ConcurrentLinkedQueue<>();
    private final Map<Long, AnalysisResult> analysisCache = new HashMap<>();

    private int deferredTickCounter = 0;
    private long lastBacklogWarning = 0;
    private int lastLoggedQueueSize = 0;

    public ClassificationScheduler(ServerLevel level) {
        this.level = level;
        this.workQueue = new PriorityQueue<>();
        this.asyncPool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "ZenAtelier-ChunkClassifier");
            t.setDaemon(true);
            return t;
        });
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

        ZoneRegistry zoneRegistry = ZoneRegistry.get(level);
        zoneRegistry.processDeferredExpansions(level);
        zoneRegistry.processDeferredBlockBreaks(level);
        zoneRegistry.expireDisabledZones(level);
        zoneRegistry.processRestoredZones(level);
        zoneRegistry.processDirtyZoneRechecks(level);
        zoneRegistry.tickRoomChanges(level);

        // Always drain completed async results regardless of server load
        drainPendingResults(zoneRegistry);
        drainPendingAnalyses();

        if (isServerUnderLoad()) return;

        // Submit new async classification tasks
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

            submitAsyncClassification(work, chunk, chunkKey, data, zoneRegistry);
            submitted++;
        }
    }

    private void submitAsyncClassification(ChunkWork work, ChunkAccess chunk, long chunkKey,
                                            ChunkClassificationData data, ZoneRegistry zoneRegistry) {
        // Snapshot chunk data on the server thread (reads block states + collision shapes)
        Map<UUID, Set<BlockPos>> restoredInChunk = zoneRegistry.getRestoredDataForChunk(chunkKey);
        ChunkSnapshot snapshot = ChunkSnapshot.of(chunk, restoredInChunk);

        // Optimistically mark as not dirty: if a block change occurs before the result is
        // applied, setDirty(true) will be called again and we'll detect + discard the stale result.
        data.setDirty(false);
        inFlightChunks.add(chunkKey);

        asyncPool.submit(() -> {
            ClassificationOutput output = new ChunkClassifier().classifyAsync(snapshot);
            pendingResults.offer(new PendingResult(work.chunkX, work.chunkZ, chunkKey, output));
        });

        long snapVersion = data.getSnapshotVersion();
        ChunkSnapshotBundle bundle = ChunkSnapshotBundle.of(chunk, snapVersion);
        analyzePool.submit(() -> {
            List<MicroRegion> regions = ChunkAnalyzer.analyze(bundle);
            AnalysisResult result = new AnalysisResult(work.chunkX, work.chunkZ, bundle.snapshotVersion, regions);
            pendingAnalyses.offer(new PendingAnalysis(work.chunkX, work.chunkZ, chunkKey, result));
        });
    }

    private void drainPendingResults(ZoneRegistry zoneRegistry) {
        int applied = 0;
        PendingResult pr;
        while ((pr = pendingResults.poll()) != null) {
            inFlightChunks.remove(pr.chunkKey());

            LevelChunk chunk = level.getChunkSource().getChunkNow(pr.chunkX(), pr.chunkZ());
            if (chunk == null) continue; // unloaded — discard

            ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
            if (data.isDirty()) {
                // Chunk was re-dirtied while the async task was running; discard and reschedule
                scheduleChunk(pr.chunkX(), pr.chunkZ());
                continue;
            }

            long startTime = System.nanoTime();
            zoneRegistry.applyBootstrap(chunk, pr.output(), pr.chunkKey(), level);
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;

            if (elapsedMs > BOOTSTRAP_TIME_WARN_MS) {
                LOGGER.warn("Bootstrap apply took {}ms for [{},{}]", elapsedMs, pr.chunkX(), pr.chunkZ());
            }

            scheduleLoadedDirtyNeighbors(pr.chunkX(), pr.chunkZ());

            if (++applied >= APPLY_PER_TICK) break;
        }
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
        asyncPool.shutdownNow();
        analyzePool.shutdownNow();
    }

    private void scheduleLoadedDirtyNeighbors(int chunkX, int chunkZ) {
        scheduleLoadedDirtyChunk(chunkX - 1, chunkZ);
        scheduleLoadedDirtyChunk(chunkX + 1, chunkZ);
        scheduleLoadedDirtyChunk(chunkX, chunkZ - 1);
        scheduleLoadedDirtyChunk(chunkX, chunkZ + 1);
    }

    private void scheduleLoadedDirtyChunk(int chunkX, int chunkZ) {
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
                LOGGER.warn("Zone classification queue critical: {} chunks queued ({}+ deferred). "
                        + "Possible incompatibility with chunk management mod or excessive structures.",
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

        // Pause only when the server is actually running slow (>45ms/tick).
        return level.getServer().getCurrentSmoothedTickTime() > 45.0f;
    }

    private void drainPendingAnalyses() {
        ZoneCommitter committer = ClassificationTickHandler.getCommitter(level);
        ZoneStore zoneStore = ClassificationTickHandler.getZoneStore(level);
        int applied = 0;
        PendingAnalysis pa;
        while ((pa = pendingAnalyses.poll()) != null && applied < APPLY_PER_TICK) {
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
                UUID existingId = findExistingByHash(zoneStore, candidate.candidateHash());

                committer.commitAccepted(candidate, decision, existingId, walkablePositions, chunkData);
            }
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

    private static UUID findExistingByHash(ZoneStore store, long candidateHash) {
        for (CommittedZone z : store.all()) {
            if (z.candidateHash() == candidateHash) return z.uuid();
        }
        return null;
    }

    private record PendingAnalysis(int chunkX, int chunkZ, long chunkKey, AnalysisResult result) {}

    private record PendingResult(int chunkX, int chunkZ, long chunkKey, ClassificationOutput output) {}

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
