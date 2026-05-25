package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Bounded connected-component scanner for room discovery.
 *
 * <p>This class is deliberately independent from Zone/registry state. Callers
 * provide the traversal graph and the rule for which cells count as interior.
 * That keeps scanning deterministic and makes later reconciliation possible:
 * scan first, then decide whether the component creates, preserves, splits, or
 * dissolves a zone.</p>
 */
public final class RoomComponentScanner {
    private RoomComponentScanner() {
    }

    /**
     * Scans a bounded connected component.
     *
     * @return the component, or {@code null} if the scan exceeds {@code limit}.
     * Exceeding the limit means the region should be treated as open/unbounded
     * for current room-discovery purposes.
     */
    @Nullable
    public static RoomComponent scanBounded(BlockPos seed,
                                            Function<BlockPos, Iterable<BlockPos>> neighbors,
                                            Predicate<BlockPos> isInterior,
                                            Predicate<BlockPos> isBoundary,
                                            int limit) {
        return scanBoundedEdges(seed,
                pos -> {
                    Set<RoomTraversalEdge> edges = new HashSet<>();
                    for (BlockPos next : neighbors.apply(pos)) {
                        edges.add(new RoomTraversalEdge(pos, next, RoomTransitionKind.FLAT));
                    }
                    return edges;
                },
                isInterior,
                isBoundary,
                limit);
    }

    /**
     * Scans a bounded component while preserving the traversed graph edges.
     *
     * <p>The graph metadata is the handoff point for threshold-aware room
     * partitioning: discovery answers "what air is reachable?", and partitioning
     * answers "which reachable air belongs to the same room identity?".</p>
     */
    @Nullable
    public static RoomComponent scanBoundedEdges(BlockPos seed,
                                                 Function<BlockPos, Iterable<RoomTraversalEdge>> edgesFrom,
                                                 Predicate<BlockPos> isInterior,
                                                 Predicate<BlockPos> isBoundary,
                                                 int limit) {
        if (!isInterior.test(seed)) return null;

        Set<BlockPos> interior = new HashSet<>();
        Set<BlockPos> boundary = new HashSet<>();
        Set<RoomTraversalEdge> edges = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        interior.add(seed);
        queue.add(seed);

        while (!queue.isEmpty()) {
            if (interior.size() > limit) return null;

            BlockPos pos = queue.poll();
            for (RoomTraversalEdge edge : edgesFrom.apply(pos)) {
                BlockPos next = edge.to();

                if (!isInterior.test(next)) {
                    if (isBoundary.test(next)) {
                        boundary.add(next);
                    }
                    continue;
                }

                edges.add(edge);
                if (interior.contains(next)) continue;

                if (interior.size() >= limit) return null;
                interior.add(next);
                queue.add(next);
            }
        }

        return new RoomComponent(interior, boundary, edges);
    }
}
