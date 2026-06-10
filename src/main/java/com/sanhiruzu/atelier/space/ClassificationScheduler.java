package com.sanhiruzu.atelier.space;

import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Priority work queue for chunk classification. Player-nearby chunks are processed
 * first. All zone lifecycle logic lives in {@link ZoneRegistry}; this class only
 * owns scheduling — what to classify and when.
 *
 * Adaptive scheduling: pauses classification when server tick time exceeds threshold
 * to avoid compounding lag during heavy chunk generation. Zone registry lifecycle
 * tasks still run regardless.
 */
public class ClassificationScheduler {
    private static final int CHUNKS_PER_TICK = 2;
    private static final int NEARBY_CHUNK_RADIUS = 6;
    private static final int DEFERRED_CHECK_INTERVAL = 20;
    private static final int QUEUE_BACKLOG_THRESHOLD = 20;

    private final ServerLevel level;
    private final PriorityQueue<ChunkWork> workQueue;
    private final Set<Long> queuedChunks = new HashSet<>();
    private final Set<Long> deferredChunks = new HashSet<>();
    private int deferredTickCounter = 0;

    public ClassificationScheduler(ServerLevel level) {
        this.level = level;
        this.workQueue = new PriorityQueue<>();
    }

    public void tick() {
        if (++deferredTickCounter >= DEFERRED_CHECK_INTERVAL) {
            deferredTickCounter = 0;
            promoteDeferredChunks();
        }

        ZoneRegistry zoneRegistry = ZoneRegistry.get(level);
        zoneRegistry.processDeferredExpansions(level);
        zoneRegistry.expireDisabledZones(level);
        zoneRegistry.processRestoredZones(level);
        zoneRegistry.processDirtyZoneRechecks(level);
        zoneRegistry.tickRoomChanges(level);

        if (isServerUnderLoad()) {
            return;
        }

        int processed = 0;
        while (!workQueue.isEmpty() && processed < CHUNKS_PER_TICK) {
            ChunkWork work = workQueue.poll();
            queuedChunks.remove(ChunkPos.asLong(work.chunkX, work.chunkZ));
            if (work.isExpired()) continue;

            ChunkAccess chunk = level.getChunk(work.chunkX, work.chunkZ);
            zoneRegistry.bootstrapChunk(chunk, level);
            scheduleLoadedDirtyNeighbors(work.chunkX, work.chunkZ);
            processed++;
        }
    }

    public void scheduleChunk(int chunkX, int chunkZ) {
        long key = ChunkPos.asLong(chunkX, chunkZ);
        if (!queuedChunks.add(key)) {
            return;
        }
        double priority = getPlayerDistance(chunkX, chunkZ);
        workQueue.offer(new ChunkWork(chunkX, chunkZ, priority));
    }

    public void deferChunk(int chunkX, int chunkZ) {
        deferredChunks.add(ChunkPos.asLong(chunkX, chunkZ));
    }

    public boolean isNearAnyPlayer(int chunkX, int chunkZ) {
        return getPlayerDistance(chunkX, chunkZ) <= NEARBY_CHUNK_RADIUS;
    }

    private void scheduleLoadedDirtyNeighbors(int chunkX, int chunkZ) {
        scheduleLoadedDirtyChunk(chunkX - 1, chunkZ);
        scheduleLoadedDirtyChunk(chunkX + 1, chunkZ);
        scheduleLoadedDirtyChunk(chunkX, chunkZ - 1);
        scheduleLoadedDirtyChunk(chunkX, chunkZ + 1);
    }

    private void scheduleLoadedDirtyChunk(int chunkX, int chunkZ) {
        ChunkAccess neighbor = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (neighbor == null) {
            return;
        }
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
        return workQueue.size() > QUEUE_BACKLOG_THRESHOLD;
    }

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
