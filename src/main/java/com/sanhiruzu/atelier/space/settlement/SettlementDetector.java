package com.sanhiruzu.atelier.space.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.*;

/**
 * Detects settlements (cities, modded villages, etc.) by analyzing door clustering patterns.
 * <p>
 * Algorithm: For each door, count nearby doors within a proximity threshold.
 * If 4+ doors cluster together (within 40 blocks, similar y-level), mark as settlement area.
 * Scattered doors (like in strongholds) are ignored.
 */
public class SettlementDetector {
    private static final int CLUSTER_RADIUS = 40;
    private static final int Y_TOLERANCE = 3;
    private static final int SETTLEMENT_THRESHOLD = 4;

    private final Level level;
    private final Set<BlockPos> processedDoors = new HashSet<>();
    private final List<Settlement> detectedSettlements = new ArrayList<>();

    public SettlementDetector(Level level) {
        this.level = level;
    }

    /**
     * Scans a chunk for doors and identifies settlements based on clustering.
     */
    public void scanChunk(ChunkAccess chunk) {
        List<BlockPos> doorsInChunk = new ArrayList<>();

        // Find all doors in this chunk
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.getBlock() instanceof DoorBlock) {
                        doorsInChunk.add(pos.immutable());
                    }
                }
            }
        }

        // Analyze door clustering
        for (BlockPos door : doorsInChunk) {
            if (processedDoors.contains(door)) continue;

            // Count nearby doors
            int nearbyCount = countNearbyDoors(door, doorsInChunk);
            if (nearbyCount >= SETTLEMENT_THRESHOLD) {
                // Found a settlement cluster
                Settlement settlement = createSettlementFromCluster(door, doorsInChunk);
                detectedSettlements.add(settlement);
                processedDoors.addAll(settlement.doorPositions);
            }
        }
    }

    /**
     * Counts doors near the given position that would cluster together.
     */
    private int countNearbyDoors(BlockPos center, List<BlockPos> allDoors) {
        int count = 0;
        for (BlockPos door : allDoors) {
            if (isNearby(center, door)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if two doors are close enough to be part of the same settlement.
     */
    private boolean isNearby(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());

        return dx <= CLUSTER_RADIUS && dy <= Y_TOLERANCE && dz <= CLUSTER_RADIUS;
    }

    /**
     * Creates a settlement from a detected door cluster.
     */
    private Settlement createSettlementFromCluster(BlockPos origin, List<BlockPos> allDoors) {
        Set<BlockPos> clusterDoors = new HashSet<>();
        clusterDoors.add(origin);

        // Flood-fill nearby doors to find the full cluster
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(origin);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (BlockPos door : allDoors) {
                if (!clusterDoors.contains(door) && isNearby(current, door)) {
                    clusterDoors.add(door);
                    queue.add(door);
                }
            }
        }

        // Calculate settlement bounds
        int minX = clusterDoors.stream().mapToInt(BlockPos::getX).min().orElse(0);
        int maxX = clusterDoors.stream().mapToInt(BlockPos::getX).max().orElse(0);
        int minZ = clusterDoors.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        int maxZ = clusterDoors.stream().mapToInt(BlockPos::getZ).max().orElse(0);
        int centerY = clusterDoors.stream().mapToInt(BlockPos::getY).sum() / clusterDoors.size();

        return new Settlement(clusterDoors, minX, maxX, minZ, maxZ, centerY);
    }

    /**
     * Returns all detected settlements.
     */
    public List<Settlement> getDetectedSettlements() {
        return new ArrayList<>(detectedSettlements);
    }

    /**
     * Checks if a door position is part of a detected settlement.
     */
    public boolean isDoorInSettlement(BlockPos doorPos) {
        for (Settlement settlement : detectedSettlements) {
            if (settlement.doorPositions.contains(doorPos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Represents a detected settlement cluster.
     */
    public static class Settlement {
        public final Set<BlockPos> doorPositions;
        public final int minX, maxX, minZ, maxZ, centerY;

        public Settlement(Set<BlockPos> doorPositions, int minX, int maxX, int minZ, int maxZ, int centerY) {
            this.doorPositions = doorPositions;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.centerY = centerY;
        }

        public boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX &&
                    pos.getZ() >= minZ && pos.getZ() <= maxZ &&
                    Math.abs(pos.getY() - centerY) <= Y_TOLERANCE;
        }
    }
}
