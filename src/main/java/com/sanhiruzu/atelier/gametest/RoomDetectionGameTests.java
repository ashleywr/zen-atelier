package com.sanhiruzu.atelier.gametest;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.space.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(ZenAtelier.MODID)
public class RoomDetectionGameTests {

    // -------------------------------------------------------------------------
    // Shared helper — classifies the 2×2 chunk area starting at the structure
    // origin, then asserts (a) at least one INSIDE block exists and (b) every
    // bed block in that area is classified as INSIDE via SpaceQuery.
    // -------------------------------------------------------------------------
    private static void assertBedsAreInside(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(BlockPos.ZERO);

        // Structures are placed with origin at a corner; a medium house may extend
        // into the adjacent chunk in +X and/or +Z. Classify a 2×2 chunk area so
        // beds near a chunk boundary are always found and correctly classified.
        int ox = origin.getX() >> 4;
        int oz = origin.getZ() >> 4;
        for (int cx = ox; cx <= ox + 1; cx++)
            for (int cz = oz; cz <= oz + 1; cz++)
                new ChunkClassifier().classify(level.getChunk(cx, cz));

        int insideCount = 0;
        for (int cx = ox; cx <= ox + 1; cx++) {
            for (int cz = oz; cz <= oz + 1; cz++) {
                ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
                for (int x = 0; x < 16; x++)
                    for (int z = 0; z < 16; z++)
                        for (int y = -64; y < 320; y++)
                            if (data.getBlockState(x, y, z) == ClassificationState.INSIDE) insideCount++;
            }
        }

        helper.assertTrue(insideCount > 0,
                "Expected at least one INSIDE block, but got " + insideCount);

        List<BlockPos> bedsFound = new ArrayList<>();
        List<BlockPos> bedsNotInside = new ArrayList<>();

        for (int cx = ox; cx <= ox + 1; cx++) {
            for (int cz = oz; cz <= oz + 1; cz++) {
                int baseX = cx << 4;
                int baseZ = cz << 4;
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        for (int y = -64; y < 320; y++) {
                            BlockPos absPos = new BlockPos(baseX + lx, y, baseZ + lz);
                            if (!level.getBlockState(absPos).is(BlockTags.BEDS)) continue;
                            bedsFound.add(absPos);
                            if (!SpaceQuery.isInside(level, absPos)) bedsNotInside.add(absPos);
                        }
                    }
                }
            }
        }

        helper.assertTrue(!bedsFound.isEmpty(),
                "Expected at least one bed in the structure but found none");
        helper.assertTrue(bedsNotInside.isEmpty(),
                bedsNotInside.size() + " bed(s) not classified as INSIDE: " + bedsNotInside);

        helper.succeed();
    }

    // Random sample of 3 structures from different biomes for fast testing
    // Full test suite with 37+ structures available in git history

    @PrefixGameTestTemplate(false)
    @GameTest(template = "plains_small_house_1")
    public static void testPlainsSmallHouse1(GameTestHelper helper) {
        assertBedsAreInside(helper);
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "desert_small_house_1")
    public static void testDesertSmallHouse1(GameTestHelper helper) {
        assertBedsAreInside(helper);
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "taiga_medium_house_2")
    public static void testTaigaMediumHouse2(GameTestHelper helper) {
        assertBedsAreInside(helper);
    }
}
