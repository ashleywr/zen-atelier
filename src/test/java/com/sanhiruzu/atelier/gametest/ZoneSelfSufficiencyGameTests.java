package com.sanhiruzu.atelier.gametest;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.space.*;
import com.sanhiruzu.atelier.space.zone.Zone;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tests for zone self-sufficiency: zones should handle complex player scenarios
 * like building/rebuilding rooms, multi-chunk zones, water avoidance, and merging.
 */
@GameTestHolder(ZenAtelier.MODID)
public class ZoneSelfSufficiencyGameTests {

    // Use the existing empty template from ZoneResilienceGameTests
    private static final String EMPTY_TEMPLATE = "zoneresiliencegametests.testzonesurvivestcascade";

    // =========================================================================
    // Multi-chunk tests
    // =========================================================================

    /**
     * A room that spans 2×2 chunks (cross-chunk) should be recognized as a single
     * zone after bootstrap, with consistent metadata and volume.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testZoneSpansTwoChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;

        // Build a 6×6 room that straddles chunk boundaries
        BlockPos origin = new BlockPos((cx << 4) + 5, templateOrigin.getY() + 1, (cz << 4) + 5);
        buildSealedRoom(level, origin, 6, 6, 3);

        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);
        for (int c1x = cx; c1x <= cx + 1; c1x++) {
            for (int c1z = cz; c1z <= cz + 1; c1z++) {
                ChunkClassificationAttachment.get(level.getChunk(c1x, c1z)).setDirty(true);
                scheduler.scheduleChunk(c1x, c1z);
            }
        }
        for (int c1x = cx; c1x <= cx + 1; c1x++) {
            for (int c1z = cz; c1z <= cz + 1; c1z++) {
                ZoneRegistry.get(level).bootstrapChunk(level.getChunk(c1x, c1z), level);
            }
        }

        UUID roomZoneId = null;
        int totalInsideBlocks = 0;
        for (int c1x = cx; c1x <= cx + 1; c1x++) {
            for (int c1z = cz; c1z <= cz + 1; c1z++) {
                ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(c1x, c1z));
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        for (int y = 0; y < 320; y++) {
                            if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE) {
                                totalInsideBlocks++;
                                int absX = (c1x << 4) + lx;
                                int absZ = (c1z << 4) + lz;
                                BlockPos absPos = new BlockPos(absX, y, absZ);
                                SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
                                UUID id = registry.getRegionIdAt(absPos);
                                if (roomZoneId == null) roomZoneId = id;
                                helper.assertTrue(id != null, "INSIDE block " + absPos + " has no zone");
                                helper.assertTrue(id.equals(roomZoneId),
                                        "INSIDE block " + absPos + " belongs to different zone: " + id + " != " + roomZoneId);
                            }
                        }
                    }
                }
            }
        }

        helper.assertTrue(roomZoneId != null, "No zone found for 2-chunk room");
        helper.assertTrue(totalInsideBlocks > 0, "No INSIDE blocks found");

        Zone zone = ZoneRegistry.get(level).getZoneById(roomZoneId);
        helper.assertTrue(zone != null, "Zone entity not found");
        helper.assertTrue(zone.getVolume() == totalInsideBlocks,
                "Zone volume " + zone.getVolume() + " != total INSIDE blocks " + totalInsideBlocks);

        helper.succeed();
    }

    /**
     * Two separate sealed rooms in adjacent chunks should be recognized as
     * separate zones with independent metadata.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testTwoSeparateZonesInAdjacentChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;

        // Room 1: cx, cz
        BlockPos origin1 = new BlockPos((cx << 4) + 2, templateOrigin.getY() + 1, (cz << 4) + 2);
        buildSealedRoom(level, origin1, 4, 4, 3);

        // Room 2: cx+1, cz (separate chunk, far enough not to merge)
        BlockPos origin2 = new BlockPos(((cx + 1) << 4) + 2, templateOrigin.getY() + 1, (cz << 4) + 2);
        buildSealedRoom(level, origin2, 4, 4, 3);

        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);
        for (int c1x = cx; c1x <= cx + 1; c1x++) {
            ChunkClassificationAttachment.get(level.getChunk(c1x, cz)).setDirty(true);
            scheduler.scheduleChunk(c1x, cz);
        }
        for (int c1x = cx; c1x <= cx + 1; c1x++) {
            ZoneRegistry.get(level).bootstrapChunk(level.getChunk(c1x, cz), level);
        }

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        Set<UUID> zoneIds = new HashSet<>();
        for (int c1x = cx; c1x <= cx + 1; c1x++) {
            ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(c1x, cz));
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    for (int y = 0; y < 320; y++) {
                        if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE) {
                            int absX = (c1x << 4) + lx;
                            UUID id = registry.getRegionIdAt(new BlockPos(absX, y, (cz << 4) + lz));
                            if (id != null) zoneIds.add(id);
                        }
                    }
                }
            }
        }

        helper.assertTrue(zoneIds.size() == 2, "Expected 2 separate zones, got " + zoneIds.size());
        helper.succeed();
    }

    // =========================================================================
    // Build/rebuild scenarios
    // =========================================================================

    /**
     * Player builds a 4×4 room block by block, completes it, then modifies the
     * interior. Zone should adapt to the growing and changing room.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testBuildingRoomGradually(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        BlockPos origin = new BlockPos((cx << 4) + 3, templateOrigin.getY() + 1, (cz << 4) + 3);

        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);
        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));

        // Build walls block by block
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                if (x == 0 || x == 3 || z == 0 || z == 3) {
                    level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
                    data.setDirty(true);
                }
            }
        }

        // Add ceiling
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                level.setBlockAndUpdate(origin.offset(x, 2, z), Blocks.OAK_PLANKS.defaultBlockState());
                data.setDirty(true);
            }
        }

        // Add door
        level.setBlockAndUpdate(origin.offset(2, 0, 0), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        level.setBlockAndUpdate(origin.offset(2, 1, 0), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        data.setDirty(true);

        ZoneRegistry.get(level).bootstrapChunk(level.getChunk(cx, cz), level);

        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        int insideCount1 = 0;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 0; y < 320; y++) {
                    if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE) insideCount1++;
                }
            }
        }
        helper.assertTrue(insideCount1 > 0, "No INSIDE blocks after initial build");

        // Modify interior: place a pillar in the middle
        level.setBlockAndUpdate(origin.offset(2, 1, 2), Blocks.OAK_PLANKS.defaultBlockState());
        data.setDirty(true);
        scheduler.scheduleChunk(cx, cz);
        new com.sanhiruzu.atelier.space.ChunkClassifier().classify(level.getChunk(cx, cz));

        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        int insideCount2 = 0;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 0; y < 320; y++) {
                    if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE) insideCount2++;
                }
            }
        }
        // Pillar placement should reduce INSIDE count
        helper.assertTrue(insideCount2 < insideCount1,
                "Pillar placement did not reduce INSIDE count: " + insideCount1 + " -> " + insideCount2);

        helper.succeed();
    }

    /**
     * Player builds a room, dissolves it completely by placing blocks everywhere,
     * then clears it again. Zone should handle the full lifecycle.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testRoomBuildDestroyCycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        BlockPos origin = new BlockPos((cx << 4) + 2, templateOrigin.getY() + 1, (cz << 4) + 2);

        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);
        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));

        // Build
        buildSealedRoom(level, origin, 4, 4, 3);
        data.setDirty(true);
        ZoneRegistry.get(level).bootstrapChunk(level.getChunk(cx, cz), level);

        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        int insideCountBefore = 0;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 0; y < 320; y++) {
                    if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE) insideCountBefore++;
                }
            }
        }
        helper.assertTrue(insideCountBefore > 0, "No INSIDE blocks after initial build");

        // Destroy: fill interior with blocks
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                for (int y = 0; y < 3; y++) {
                    level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
        }
        data.setDirty(true);
        scheduler.scheduleChunk(cx, cz);
        new com.sanhiruzu.atelier.space.ChunkClassifier().classify(level.getChunk(cx, cz));

        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        int insideCountDestroyed = 0;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 0; y < 320; y++) {
                    if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE) insideCountDestroyed++;
                }
            }
        }
        helper.assertTrue(insideCountDestroyed == 0, "INSIDE blocks remain after filling room");

        // Clear and rebuild
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                for (int y = 0; y < 3; y++) {
                    level.removeBlock(origin.offset(x, y, z), false);
                }
            }
        }
        buildSealedRoom(level, origin, 4, 4, 3);
        data.setDirty(true);
        scheduler.scheduleChunk(cx, cz);
        new com.sanhiruzu.atelier.space.ChunkClassifier().classify(level.getChunk(cx, cz));

        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        int insideCountRebuilt = 0;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 0; y < 320; y++) {
                    if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE) insideCountRebuilt++;
                }
            }
        }
        helper.assertTrue(insideCountRebuilt > 0, "No INSIDE blocks after rebuild");

        helper.succeed();
    }

    // =========================================================================
    // Water and edge cases
    // =========================================================================

    /**
     * A room filled with water should NOT be classified as INSIDE air.
     * Water blocks replace air, so interior should be empty.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testWaterDoesNotCreateZones(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        BlockPos origin = new BlockPos((cx << 4) + 2, templateOrigin.getY() + 1, (cz << 4) + 2);

        // Build room walls
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                if (x == 0 || x == 3 || z == 0 || z == 3) {
                    level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
                    level.setBlockAndUpdate(origin.offset(x, 1, z), Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
        }

        // Add ceiling
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                level.setBlockAndUpdate(origin.offset(x, 2, z), Blocks.OAK_PLANKS.defaultBlockState());
            }
        }

        // Add door
        level.setBlockAndUpdate(origin.offset(2, 0, 0), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        level.setBlockAndUpdate(origin.offset(2, 1, 0), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // Fill interior with water
        for (int x = 1; x < 3; x++) {
            for (int z = 1; z < 3; z++) {
                for (int y = 0; y < 2; y++) {
                    level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.WATER.defaultBlockState());
                }
            }
        }

        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);
        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        data.setDirty(true);
        ZoneRegistry.get(level).bootstrapChunk(level.getChunk(cx, cz), level);

        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        int insideCount = 0;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 0; y < 320; y++) {
                    if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE) insideCount++;
                }
            }
        }

        helper.assertTrue(insideCount == 0, "Water-filled room should have 0 INSIDE blocks, got " + insideCount);
        helper.succeed();
    }

    /**
     * A fence-enclosed outdoor garden should NOT create an indoor zone.
     * Fence height is low; outdoor classification should prevail.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testFenceEnclosureIsOutdoor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        BlockPos origin = new BlockPos((cx << 4) + 2, templateOrigin.getY() + 1, (cz << 4) + 2);

        // Build fence ring (1 block high)
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 6; z++) {
                if (x == 0 || x == 5 || z == 0 || z == 5) {
                    level.setBlockAndUpdate(origin.offset(x, 0, z),
                            Blocks.OAK_FENCE.defaultBlockState());
                }
            }
        }

        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);
        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        data.setDirty(true);
        new com.sanhiruzu.atelier.space.ChunkClassifier().classify(level.getChunk(cx, cz));

        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        int insideCount = 0;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 0; y < 320; y++) {
                    if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE) insideCount++;
                }
            }
        }

        // Fence enclosures should not create INSIDE zones (no ceiling, open to sky)
        helper.assertTrue(insideCount == 0, "Fence enclosure should not create INSIDE blocks, got " + insideCount);
        helper.succeed();
    }

    /**
     * Low-wall outdoor room (1 block high walls, no ceiling) should remain OUTSIDE.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testLowWallOutdoorRoomIsOutside(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        BlockPos origin = new BlockPos((cx << 4) + 2, templateOrigin.getY() + 1, (cz << 4) + 2);

        // Build 1-block-high walls around a 4×4 area
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                if (x == 0 || x == 3 || z == 0 || z == 3) {
                    level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
        }

        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);
        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        data.setDirty(true);
        new com.sanhiruzu.atelier.space.ChunkClassifier().classify(level.getChunk(cx, cz));

        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        int insideCount = 0;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 0; y < 320; y++) {
                    if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE) insideCount++;
                }
            }
        }

        helper.assertTrue(insideCount == 0, "Low-wall outdoor room should have 0 INSIDE blocks, got " + insideCount);
        helper.succeed();
    }

    // =========================================================================
    // Zone merging
    // =========================================================================

    /**
     * Two rooms separated by a wall. Player breaks the wall, zones merge into one.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testZonesMergeWhenWallBroken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;

        // Room 1
        BlockPos origin1 = new BlockPos((cx << 4) + 1, templateOrigin.getY() + 1, (cz << 4) + 1);
        buildSealedRoom(level, origin1, 4, 4, 3);

        // Room 2 (adjacent, sharing a wall)
        BlockPos origin2 = new BlockPos((cx << 4) + 5, templateOrigin.getY() + 1, (cz << 4) + 1);
        buildSealedRoom(level, origin2, 4, 4, 3);

        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);
        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        data.setDirty(true);
        ZoneRegistry.get(level).bootstrapChunk(level.getChunk(cx, cz), level);

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        Set<UUID> zoneIds1 = new HashSet<>();
        collectZoneIds(level, cx, cz, zoneIds1);
        int initialZones = zoneIds1.size();
        helper.assertTrue(initialZones == 2, "Expected 2 zones before wall break, got " + initialZones);

        // Break the wall between rooms
        BlockPos sharedWall = new BlockPos((cx << 4) + 5, templateOrigin.getY() + 1, (cz << 4) + 2);
        level.removeBlock(sharedWall, false);
        data.setDirty(true);
        scheduler.scheduleChunk(cx, cz);
        new com.sanhiruzu.atelier.space.ChunkClassifier().classify(level.getChunk(cx, cz));

        Set<UUID> zoneIds2 = new HashSet<>();
        collectZoneIds(level, cx, cz, zoneIds2);
        int mergedZones = zoneIds2.size();
        helper.assertTrue(mergedZones == 1, "Expected 1 zone after wall break, got " + mergedZones);

        // The merged zone should be bigger than either original zone alone.
        UUID survivingId = zoneIds2.iterator().next();
        ZoneData merged = ZoneRegistry.get(level).getZone(survivingId);
        ZoneAssertion.expect()
                .indoor()
                .enclosureAbove(0.5f)
                .check(helper, merged, "merged zone");

        helper.succeed();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void buildSealedRoom(ServerLevel level, BlockPos origin, int width, int depth, int height) {
        // Walls
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                if (x == 0 || x == width - 1 || z == 0 || z == depth - 1) {
                    for (int y = 0; y < height; y++) {
                        if (!(x == width / 2 && z == 0 && y < 2)) { // leave gap for door
                            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.OAK_PLANKS.defaultBlockState());
                        }
                    }
                }
            }
        }

        // Ceiling
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                level.setBlockAndUpdate(origin.offset(x, height, z), Blocks.OAK_PLANKS.defaultBlockState());
            }
        }

        // Door
        int doorX = width / 2;
        level.setBlockAndUpdate(origin.offset(doorX, 0, 0), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        level.setBlockAndUpdate(origin.offset(doorX, 1, 0), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static void collectZoneIds(ServerLevel level, int cx, int cz, Set<UUID> out) {
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        int baseX = cx << 4, baseZ = cz << 4;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 0; y < 320; y++) {
                    if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE) {
                        UUID id = registry.getRegionIdAt(new BlockPos(baseX + lx, y, baseZ + lz));
                        if (id != null) out.add(id);
                    }
                }
            }
        }
    }
}
