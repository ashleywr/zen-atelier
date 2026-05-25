package com.sanhiruzu.atelier.space.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Predicate;

public record ZoneGeometryProfile(
        CeilingStatus ceilingStatus,
        int ceilingOpeningArea,
        int walkableBlocks,
        int reachableWalkableBlocks,
        int subAreaCount,
        boolean hasVerticalConnector
) {
    public enum CeilingStatus {
        SEALED,
        COVERED,
        OPEN
    }

    public static ZoneGeometryProfile compute(Set<BlockPos> zoneBlocks, Level level, Optional<Zone.ZoneEntry> entry) {
        return compute(zoneBlocks,
                pos -> level.getBlockState(pos).isAir(),
                pos -> !level.getBlockState(pos).isAir(),
                pos -> {
                    var state = level.getBlockState(pos);
                    return state.is(BlockTags.TRAPDOORS)
                            || state.is(BlockTags.STAIRS)
                            || state.is(BlockTags.SLABS)
                            || state.is(BlockTags.CLIMBABLE);
                },
                entry.map(Zone.ZoneEntry::primaryBlock).orElse(null));
    }

    static ZoneGeometryProfile compute(Set<BlockPos> zoneBlocks,
                                       Predicate<BlockPos> isAir,
                                       Predicate<BlockPos> isSupport,
                                       Predicate<BlockPos> isConnector,
                                       BlockPos entryBlock) {
        if (zoneBlocks.isEmpty()) {
            return new ZoneGeometryProfile(CeilingStatus.OPEN, 0, 0, 0, 0, false);
        }

        CeilingMetrics ceiling = computeCeiling(zoneBlocks, isAir, isConnector);
        Set<BlockPos> walkable = computeWalkable(zoneBlocks, isAir, isSupport, isConnector);
        int subAreas = countWalkableComponents(walkable);
        int reachable = countReachableWalkable(walkable, entryBlock);
        boolean verticalConnector = zoneBlocks.stream().anyMatch(isConnector)
                && hasWalkableHeightChangeNearConnector(walkable, isConnector);

        return new ZoneGeometryProfile(ceiling.status(), ceiling.openings(),
                walkable.size(), reachable, subAreas, verticalConnector);
    }

    private static CeilingMetrics computeCeiling(Set<BlockPos> zoneBlocks,
                                                 Predicate<BlockPos> isAir,
                                                 Predicate<BlockPos> isConnector) {
        Map<Long, Integer> topYByColumn = new HashMap<>();
        for (BlockPos pos : zoneBlocks) {
            long column = columnKey(pos);
            topYByColumn.merge(column, pos.getY(), Math::max);
        }

        int rawOpenings = 0;
        int coveredOpenings = 0;
        for (BlockPos pos : zoneBlocks) {
            if (topYByColumn.get(columnKey(pos)) != pos.getY()) continue;

            BlockPos above = pos.above();
            if (zoneBlocks.contains(above)) continue;
            if (!isAir.test(above)) continue;

            rawOpenings++;
            if (!hasNearbyConnector(pos, above, isConnector)) {
                coveredOpenings++;
            }
        }

        CeilingStatus status = rawOpenings == 0
                ? CeilingStatus.SEALED
                : coveredOpenings == 0 ? CeilingStatus.COVERED : CeilingStatus.OPEN;
        return new CeilingMetrics(status, coveredOpenings);
    }

    private static Set<BlockPos> computeWalkable(Set<BlockPos> zoneBlocks,
                                                 Predicate<BlockPos> isAir,
                                                 Predicate<BlockPos> isSupport,
                                                 Predicate<BlockPos> isConnector) {
        Set<BlockPos> walkable = new HashSet<>();
        for (BlockPos pos : zoneBlocks) {
            boolean standingSpace = zoneBlocks.contains(pos) || isAir.test(pos);
            boolean headSpace = zoneBlocks.contains(pos.above()) || isAir.test(pos.above());
            boolean supported = isSupport.test(pos.below()) || isConnector.test(pos) || isConnector.test(pos.below());
            if (standingSpace && headSpace && supported) {
                walkable.add(pos);
            }
        }
        return walkable;
    }

    private static int countReachableWalkable(Set<BlockPos> walkable, BlockPos entryBlock) {
        if (walkable.isEmpty()) return 0;
        BlockPos seed = null;
        if (entryBlock != null) {
            if (walkable.contains(entryBlock)) {
                seed = entryBlock;
            } else {
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = entryBlock.relative(dir);
                    if (walkable.contains(neighbor)) {
                        seed = neighbor;
                        break;
                    }
                }
            }
        }
        if (seed == null) seed = walkable.iterator().next();
        return floodWalkable(walkable, seed).size();
    }

    private static int countWalkableComponents(Set<BlockPos> walkable) {
        Set<BlockPos> remaining = new HashSet<>(walkable);
        int count = 0;
        while (!remaining.isEmpty()) {
            Set<BlockPos> component = floodWalkable(walkable, remaining.iterator().next());
            remaining.removeAll(component);
            count++;
        }
        return count;
    }

    private static Set<BlockPos> floodWalkable(Set<BlockPos> walkable, BlockPos seed) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(seed);
        queue.add(seed);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (walkable.contains(neighbor) && seen.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return seen;
    }

    private static boolean hasWalkableHeightChangeNearConnector(Set<BlockPos> walkable, Predicate<BlockPos> isConnector) {
        for (BlockPos pos : walkable) {
            if (!isConnector.test(pos) && !isConnector.test(pos.below())) continue;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (walkable.contains(pos.relative(dir).above()) || walkable.contains(pos.relative(dir).below())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasNearbyConnector(BlockPos interiorPos, BlockPos airNeighbor, Predicate<BlockPos> isConnector) {
        for (BlockPos origin : List.of(interiorPos, airNeighbor)) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (isConnector.test(origin.offset(dx, dy, dz))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static long columnKey(BlockPos pos) {
        return ((long) pos.getX() << 32) ^ (pos.getZ() & 0xFFFFFFFFL);
    }

    private record CeilingMetrics(CeilingStatus status, int openings) {
    }
}
