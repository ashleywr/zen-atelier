package com.sanhiruzu.atelier.space.zone;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneGeometryProfileTest {
    @Test
    void sealedCeiling_hasNoVerticalOpenings() {
        Set<BlockPos> blocks = box(0, 1, 0, 1, 2, 1);
        Set<BlockPos> solid = ceilingAbove(blocks);
        solid.addAll(floorBelow(blocks));

        ZoneGeometryProfile profile = profile(blocks, Set.of(), solid, Set.of(), null);

        assertEquals(ZoneGeometryProfile.CeilingStatus.SEALED, profile.ceilingStatus());
        assertEquals(0, profile.ceilingOpeningArea());
    }

    @Test
    void connectorCeilingGap_countsAsCovered() {
        Set<BlockPos> blocks = box(0, 1, 0, 1, 2, 1);
        Set<BlockPos> solid = ceilingAbove(blocks);
        BlockPos gap = new BlockPos(0, 3, 0);
        solid.remove(gap);
        solid.addAll(floorBelow(blocks));
        Set<BlockPos> air = Set.of(gap);
        Set<BlockPos> connectors = Set.of(new BlockPos(0, 2, 0));

        ZoneGeometryProfile profile = profile(blocks, air, solid, connectors, null);

        assertEquals(ZoneGeometryProfile.CeilingStatus.COVERED, profile.ceilingStatus());
        assertEquals(0, profile.ceilingOpeningArea());
    }

    @Test
    void uncoveredCeilingGap_countsAsOpen() {
        Set<BlockPos> blocks = box(0, 1, 0, 1, 2, 1);
        Set<BlockPos> solid = ceilingAbove(blocks);
        BlockPos gap = new BlockPos(0, 3, 0);
        solid.remove(gap);
        solid.addAll(floorBelow(blocks));

        ZoneGeometryProfile profile = profile(blocks, Set.of(gap), solid, Set.of(), null);

        assertEquals(ZoneGeometryProfile.CeilingStatus.OPEN, profile.ceilingStatus());
        assertEquals(1, profile.ceilingOpeningArea());
    }

    @Test
    void disconnectedWalkableAreas_reportSubAreasAndReachableArea() {
        Set<BlockPos> blocks = new HashSet<>();
        blocks.add(new BlockPos(0, 1, 0));
        blocks.add(new BlockPos(1, 1, 0));
        blocks.add(new BlockPos(5, 1, 0));
        Set<BlockPos> solid = floorBelow(blocks);

        ZoneGeometryProfile profile = profile(blocks, Set.of(), solid, Set.of(), new BlockPos(0, 1, 0));

        assertEquals(3, profile.walkableBlocks());
        assertEquals(2, profile.reachableWalkableBlocks());
        assertEquals(2, profile.subAreaCount());
    }

    @Test
    void heightChangeNearConnector_setsVerticalConnectorFlag() {
        Set<BlockPos> blocks = new HashSet<>();
        BlockPos stair = new BlockPos(0, 1, 0);
        blocks.add(stair);
        blocks.add(new BlockPos(1, 2, 0));
        Set<BlockPos> solid = floorBelow(blocks);
        Set<BlockPos> connectors = Set.of(stair);

        ZoneGeometryProfile profile = profile(blocks, Set.of(), solid, connectors, stair);

        assertTrue(profile.hasVerticalConnector());
    }

    private static ZoneGeometryProfile profile(Set<BlockPos> blocks,
                                               Set<BlockPos> air,
                                               Set<BlockPos> solid,
                                               Set<BlockPos> connectors,
                                               BlockPos entry) {
        return ZoneGeometryProfile.compute(blocks,
                pos -> air.contains(pos) || (!solid.contains(pos) && !blocks.contains(pos)),
                solid::contains,
                connectors::contains,
                entry);
    }

    private static Set<BlockPos> box(int x0, int y0, int z0, int x1, int y1, int z1) {
        Set<BlockPos> out = new HashSet<>();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    out.add(new BlockPos(x, y, z));
                }
            }
        }
        return out;
    }

    private static Set<BlockPos> ceilingAbove(Set<BlockPos> blocks) {
        Set<BlockPos> out = new HashSet<>();
        for (BlockPos pos : blocks) {
            if (!blocks.contains(pos.above())) {
                out.add(pos.above());
            }
        }
        return out;
    }

    private static Set<BlockPos> floorBelow(Set<BlockPos> blocks) {
        Set<BlockPos> out = new HashSet<>();
        for (BlockPos pos : blocks) {
            if (!blocks.contains(pos.below())) {
                out.add(pos.below());
            }
        }
        return out;
    }
}
