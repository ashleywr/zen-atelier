package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RoomPartitionerTest {
    @Test
    void broadStepOpeningKeepsSplitLevelRoomTogether() {
        Set<BlockPos> lowerPit = Set.of(
                new BlockPos(0, 0, 0),
                new BlockPos(1, 0, 0),
                new BlockPos(0, 0, 1),
                new BlockPos(1, 0, 1)
        );
        Set<BlockPos> upperLivingRoom = Set.of(
                new BlockPos(0, 1, 0),
                new BlockPos(1, 1, 0),
                new BlockPos(0, 1, 1),
                new BlockPos(1, 1, 1),
                new BlockPos(2, 1, 0),
                new BlockPos(2, 1, 1)
        );
        Set<BlockPos> interior = union(lowerPit, upperLivingRoom);
        Set<RoomTraversalEdge> edges = new HashSet<>();
        addFlatEdges(edges, lowerPit);
        addFlatEdges(edges, upperLivingRoom);
        for (BlockPos lower : lowerPit) {
            BlockPos upper = lower.above();
            edges.add(new RoomTraversalEdge(lower, upper, RoomTransitionKind.STEP_UP, lower));
            edges.add(new RoomTraversalEdge(upper, lower, RoomTransitionKind.STEP_DOWN, lower));
        }

        RoomComponent component = new RoomComponent(interior, Set.of(), edges);

        List<RoomComponent> partitions = RoomPartitioner.partition(component);

        assertEquals(1, partitions.size(),
                "a broad open elevation change should remain one living-room-like space");
        assertEquals(interior, partitions.getFirst().interiorBlocks());
    }

    @Test
    void narrowStairThresholdSplitsBasementFromMainFloor() {
        Set<BlockPos> basement = Set.of(
                new BlockPos(0, 0, 0),
                new BlockPos(1, 0, 0),
                new BlockPos(0, 0, 1),
                new BlockPos(1, 0, 1)
        );
        Set<BlockPos> mainFloor = Set.of(
                new BlockPos(0, 1, 0),
                new BlockPos(1, 1, 0),
                new BlockPos(0, 1, 1),
                new BlockPos(1, 1, 1)
        );
        Set<BlockPos> interior = union(basement, mainFloor);
        Set<RoomTraversalEdge> edges = new HashSet<>();
        addFlatEdges(edges, basement);
        addFlatEdges(edges, mainFloor);

        BlockPos basementLanding = new BlockPos(0, 0, 0);
        BlockPos mainLanding = new BlockPos(0, 1, 0);
        BlockPos stair = new BlockPos(0, 0, -1);
        edges.add(new RoomTraversalEdge(basementLanding, mainLanding, RoomTransitionKind.STEP_UP, stair));
        edges.add(new RoomTraversalEdge(mainLanding, basementLanding, RoomTransitionKind.STEP_DOWN, stair));

        RoomComponent component = new RoomComponent(interior, Set.of(), edges);

        List<RoomComponent> partitions = RoomPartitioner.partition(component);
        RoomComponent mainPartition = RoomPartitioner.partitionContaining(component, mainLanding);

        assertEquals(2, partitions.size(),
                "one stair-width transition is a threshold, not a broad shared room opening");
        assertEquals(mainFloor, mainPartition.interiorBlocks());
        assertFalse(mainPartition.interiorBlocks().contains(basementLanding));
    }

    @Test
    void plainVerticalAirVolumeDoesNotSplitTinyRoom() {
        BlockPos lower = new BlockPos(0, 0, 0);
        BlockPos upper = lower.above();
        RoomComponent component = new RoomComponent(
                Set.of(lower, upper),
                Set.of(),
                Set.of(
                        new RoomTraversalEdge(lower, upper, RoomTransitionKind.VERTICAL_OPENING),
                        new RoomTraversalEdge(upper, lower, RoomTransitionKind.VERTICAL_OPENING)
                ));

        List<RoomComponent> partitions = RoomPartitioner.partition(component);

        assertEquals(1, partitions.size(),
                "normal headroom is vertical room volume, not a threshold between rooms");
        assertEquals(Set.of(lower, upper), partitions.getFirst().interiorBlocks());
    }

    private static Set<BlockPos> union(Set<BlockPos> first, Set<BlockPos> second) {
        Set<BlockPos> out = new HashSet<>(first);
        out.addAll(second);
        return out;
    }

    private static void addFlatEdges(Set<RoomTraversalEdge> edges, Set<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(dir);
                if (blocks.contains(next)) {
                    edges.add(new RoomTraversalEdge(pos, next, RoomTransitionKind.FLAT));
                }
            }
        }
    }
}
