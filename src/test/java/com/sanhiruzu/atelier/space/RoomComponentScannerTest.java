package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoomComponentScannerTest {
    @Test
    void scanBoundedReturnsInteriorAndBoundarySeparately() {
        Set<BlockPos> interior = Set.of(
                new BlockPos(0, 0, 0),
                new BlockPos(1, 0, 0)
        );

        RoomComponent component = RoomComponentScanner.scanBounded(
                new BlockPos(0, 0, 0),
                pos -> faceNeighbors(pos),
                interior::contains,
                pos -> pos.getY() == 0,
                10);

        assertNotNull(component);
        assertEquals(interior, component.interiorBlocks());
        assertTrue(component.boundaryBlocks().contains(new BlockPos(-1, 0, 0)));
        assertFalse(component.boundaryBlocks().contains(new BlockPos(0, 1, 0)),
                "boundary predicate should filter non-room-plane neighbors");
    }

    @Test
    void scanBoundedReturnsNullWhenLimitExceeded() {
        Set<BlockPos> corridor = new HashSet<>();
        for (int x = 0; x < 6; x++) {
            corridor.add(new BlockPos(x, 0, 0));
        }

        RoomComponent component = RoomComponentScanner.scanBounded(
                new BlockPos(0, 0, 0),
                pos -> faceNeighbors(pos),
                corridor::contains,
                pos -> true,
                5);

        assertNull(component, "regions over the limit should be treated as unbounded");
    }

    @Test
    void scanBoundedEdgesPreservesTraversalMetadata() {
        BlockPos lower = new BlockPos(0, 0, 0);
        BlockPos upper = new BlockPos(1, 1, 0);
        BlockPos stair = new BlockPos(1, 0, 0);
        Set<BlockPos> interior = Set.of(lower, upper);

        RoomTraversalEdge up = new RoomTraversalEdge(lower, upper, RoomTransitionKind.STEP_UP, stair);
        RoomTraversalEdge down = new RoomTraversalEdge(upper, lower, RoomTransitionKind.STEP_DOWN, stair);

        RoomComponent component = RoomComponentScanner.scanBoundedEdges(
                lower,
                pos -> pos.equals(lower) ? Set.of(up) : Set.of(down),
                interior::contains,
                pos -> true,
                10);

        assertNotNull(component);
        assertEquals(interior, component.interiorBlocks());
        assertTrue(component.traversalEdges().contains(up));
        assertTrue(component.traversalEdges().contains(down));
    }

    private static Iterable<BlockPos> faceNeighbors(BlockPos pos) {
        Set<BlockPos> neighbors = new HashSet<>();
        for (Direction dir : Direction.values()) {
            neighbors.add(pos.relative(dir));
        }
        return neighbors;
    }
}
