package com.sanhiruzu.atelier.gametest;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.space.*;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneEvaluator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.*;

@GameTestHolder(ZenAtelier.MODID)
public class ZoneEvaluatorGameTests {

    @PrefixGameTestTemplate(false)
    @GameTest(template = "plains_small_house_1")
    public static void testRoomDataQualityFromRealStructure(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(BlockPos.ZERO);
        var registry = SpaceRegionRegistry.get(level);

        int ox = origin.getX() >> 4;
        int oz = origin.getZ() >> 4;

        // Classify each chunk and collect block positions + regions.
        Map<UUID, Set<BlockPos>> allBlocks = new HashMap<>();
        List<ClassifiedRegion> allRegions = new ArrayList<>();
        for (int cx = ox; cx <= ox + 1; cx++) {
            for (int cz = oz; cz <= oz + 1; cz++) {
                var classifier = new ChunkClassifier();
                classifier.classify(level.getChunk(cx, cz));
                allBlocks.putAll(classifier.getLastRegionBlocks());
                allRegions.addAll(ChunkClassificationAttachment
                        .get(level.getChunk(cx, cz)).getRegions());
            }
        }

        helper.assertTrue(!allRegions.isEmpty(),
                "Expected at least one classified region in structure");

        for (ClassifiedRegion cr : allRegions) {
            Set<BlockPos> blocks = allBlocks.getOrDefault(cr.getId(), Set.of());
            helper.assertTrue(!blocks.isEmpty(),
                    "Region has no blocks; cannot compute room data");

            // Populate registry so computeRoomData can find the blocks.
            if (registry.getRegion(cr.getId()) == null) {
                registry.registerRegion(new SpaceRegion(cr.getId(),
                        ClassificationState.INSIDE, 0, cr.getOpeningArea()));
            }
            for (BlockPos pos : blocks) {
                registry.mapBlockToRegion(pos, cr.getId());
            }

            RoomData roomData = ZoneEvaluator.computeRoomData(
                    registry.getRegion(cr.getId()), level);

            helper.assertTrue(roomData.getVolume() > 0,
                    "Volume should be > 0, got " + roomData.getVolume());
            helper.assertTrue(roomData.getQuality() > 0f,
                    "Quality should be > 0, got " + roomData.getQuality());
            helper.assertTrue(roomData.getQuality() <= 1f,
                    "Quality should be <= 1, got " + roomData.getQuality());
            helper.assertTrue(roomData.getEnclosureScore() > 0.5f,
                    "Enclosure score should be > 0.5, got " + roomData.getEnclosureScore());

            // Cleanup for next iteration.
            for (BlockPos pos : blocks) {
                registry.unmapBlock(pos);
            }
            registry.removeRegion(cr.getId());
        }

        helper.succeed();
    }
}
