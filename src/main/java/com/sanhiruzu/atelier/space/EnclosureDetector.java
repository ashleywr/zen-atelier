package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * Detects newly enclosed air pockets after a block is placed.
 *
 * <p>Uses a 3D bounded flood-fill: if all air reachable from a seed position fits
 * within {@link #MAX_INTERIOR_BLOCKS}, the region is considered enclosed.
 * Blocks already registered to a zone are treated as barriers.</p>
 */
public class EnclosureDetector {
    static final int MAX_INTERIOR_BLOCKS = 10_000;

    /**
     * Returns the set of enclosed air blocks adjacent to {@code placedBlock}, or
     * {@code null} if no bounded enclosure was created by this placement.
     *
     * <p>Two passes are tried in order:</p>
     * <ol>
     *   <li>3D bounded BFS — finds rooms that are fully enclosed (ceiling + walls + floor).
     *       Best for proper indoor rooms.</li>
     *   <li>2D horizontal BFS at the seed's Y level — finds open-top enclosures like
     *       1-block-high wall rooms that are open to the sky.  The result is only the
     *       floor-level air slice; {@link net.minecraft.world.level.block.state.BlockState#isAir}
     *       boundaries and the block limit prevent false positives in open terrain.</li>
     * </ol>
     *
     * <p>Skips air that is already part of an existing zone.</p>
     */
    @Nullable
    public static Set<BlockPos> detect(ServerLevel level, BlockPos placedBlock) {
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        Set<BlockPos> testedSeeds = new HashSet<>();

        // Pass 1: 3D — fully enclosed (ceiling present)
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = placedBlock.relative(dir);
            if (!testedSeeds.add(neighbor)) continue;
            if (!level.getBlockState(neighbor).isAir()) continue;
            if (registry.getRegionIdAt(neighbor) != null) continue;

            Set<BlockPos> enclosed = tryBoundedFloodFill(level, registry, neighbor);
            if (enclosed != null) return enclosed;
        }

        // Pass 2: 2D horizontal — open-top enclosures (no ceiling, e.g. 1-high wall rooms)
        Set<BlockPos> testedH = new HashSet<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = placedBlock.relative(dir);
            if (!testedH.add(neighbor)) continue;
            if (!level.getBlockState(neighbor).isAir()) continue;
            if (registry.getRegionIdAt(neighbor) != null) continue;

            Set<BlockPos> enclosed = tryHorizontalFloodFill(level, registry, neighbor);
            if (enclosed != null) return enclosed;
        }

        return null;
    }

    @Nullable
    static Set<BlockPos> tryHorizontalFloodFill(ServerLevel level, SpaceRegionRegistry registry, BlockPos seed) {
        int y = seed.getY();
        return bfs(seed, Direction.Plane.HORIZONTAL,
                next -> next.getY() == y
                        && level.getBlockState(next).isAir()
                        && registry.getRegionIdAt(next) == null,
                MAX_INTERIOR_BLOCKS);
    }

    @Nullable
    static Set<BlockPos> tryBoundedFloodFill(ServerLevel level, SpaceRegionRegistry registry, BlockPos seed) {
        return bfs(seed, Arrays.asList(Direction.values()),
                next -> level.getBlockState(next).isAir()
                        && registry.getRegionIdAt(next) == null,
                MAX_INTERIOR_BLOCKS);
    }

    /**
     * Core bounded BFS used by both 3D and horizontal detection.
     *
     * <p>Starts at {@code seed}, expands into neighbors for which
     * {@code isPassable} returns {@code true}, using only the given
     * {@code directions}.  Returns {@code null} when the visited set
     * would exceed {@code limit} (the region is unbounded / open).</p>
     *
     * <p>Package-private so unit tests can drive it directly with a fake world
     * represented as a {@code Set<BlockPos>} predicate.</p>
     */
    @Nullable
    static Set<BlockPos> bfs(BlockPos seed,
                             Iterable<Direction> directions,
                             Predicate<BlockPos> isPassable,
                             int limit) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(seed);
        queue.add(seed);

        while (!queue.isEmpty()) {
            if (visited.size() > limit) return null;
            BlockPos pos = queue.poll();
            for (Direction dir : directions) {
                BlockPos next = pos.relative(dir);
                if (visited.contains(next)) continue;
                if (!isPassable.test(next)) continue;
                if (visited.size() >= limit) return null;
                visited.add(next);
                queue.add(next);
            }
        }
        return visited;
    }
}
