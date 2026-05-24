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

import java.util.*;

@GameTestHolder(ZenAtelier.MODID)
public class ZoneResilienceGameTests {

    /**
     * Loads the plains village church, bootstraps zones, then breaks one ceiling
     * block near the chunk seam. Verifies the zone expands to claim the new air
     * and the old interior stays intact.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "plains_temple_3")
    public static void testCeilingBreakDoesNotSplitZone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int ox = origin.getX() >> 4;
        int oz = origin.getZ() >> 4;

        // Clean restored zones from previous runs that may occupy these chunks.
        for (int cx = ox; cx <= ox + 1; cx++)
            for (int cz = oz; cz <= oz + 1; cz++)
                cleanChunkOfZones(level, cx, cz);

        // Bootstrap the 2x2 chunk area — classify + create zone entities.
        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);
        for (int cx = ox; cx <= ox + 1; cx++)
            for (int cz = oz; cz <= oz + 1; cz++)
                ChunkClassificationAttachment.get(level.getChunk(cx, cz)).setDirty(true);
        ZoneRegistry.get(level).bootstrapChunk(level.getChunk(ox, oz), level);

        Set<BlockPos> insideBlocks = collectInsideBlocks(level, ox, oz);
        helper.assertTrue(!insideBlocks.isEmpty(), "Expected INSIDE blocks before ceiling break");

        // Prefer a ceiling block within 3 blocks of the chunk seam.
        int seamX = (ox + 1) << 4;
        BlockPos ceiling = findCeilingNearX(level, insideBlocks, seamX, 3);
        if (ceiling == null) ceiling = findAnyCeiling(level, insideBlocks);
        helper.assertTrue(ceiling != null, "Could not find a solid ceiling block above any INSIDE block");

        // Find the zone adjacent to the ceiling break position.
        Zone zone = findAdjacentZone(level, ceiling);
        helper.assertTrue(zone != null, "No zone adjacent to ceiling break position");

        level.removeBlock(ceiling, false);

        // Manually expand zone — GameTest removeBlock does not fire NeoForge events.
        zone.tryExpand(ceiling, level);
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);
        helper.assertTrue(!zone.isDissolved(), "Zone dissolved after ceiling break");

        // Every old interior block must still be INSIDE.
        int wrongCount = 0;
        BlockPos firstWrong = null;
        for (BlockPos pos : insideBlocks) {
            ChunkClassificationData data = ChunkClassificationAttachment.get(
                    level.getChunk(pos.getX() >> 4, pos.getZ() >> 4));
            if (data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15) != ClassificationState.INSIDE) {
                wrongCount++;
                if (firstWrong == null) firstWrong = pos;
            }
        }
        helper.assertTrue(wrongCount == 0,
                wrongCount + " interior block(s) became non-INSIDE after ceiling break; first at " + firstWrong);

        // Ceiling break must not increase fragmentation.
        int beforeComponents = countConnectedComponents(insideBlocks);
        Set<BlockPos> afterInside = collectInsideBlocks(level, ox, oz);
        int afterComponents = countConnectedComponents(afterInside);
        helper.assertTrue(afterComponents <= beforeComponents,
                "Ceiling break increased interior fragmentation: " + beforeComponents
                        + " zone(s) before, " + afterComponents + " after");

        helper.succeed();
    }

    /**
     * Removes a 3×3 patch of ceiling blocks, then verifies the zone expands into
     * the new air and all old interior blocks remain INSIDE.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "plains_temple_3")
    public static void testExplosionDegradesButPreservesZone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int ox = origin.getX() >> 4, oz = origin.getZ() >> 4;

        // Clean restored zones from previous runs that may occupy these chunks.
        for (int cx = ox; cx <= ox + 1; cx++)
            for (int cz = oz; cz <= oz + 1; cz++)
                cleanChunkOfZones(level, cx, cz);

        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);
        for (int cx = ox; cx <= ox + 1; cx++)
            for (int cz = oz; cz <= oz + 1; cz++)
                ChunkClassificationAttachment.get(level.getChunk(cx, cz)).setDirty(true);
        ZoneRegistry.get(level).bootstrapChunk(level.getChunk(ox, oz), level);

        Set<BlockPos> insideBlocks = collectInsideBlocks(level, ox, oz);
        helper.assertTrue(!insideBlocks.isEmpty(), "Expected INSIDE blocks before explosion");

        BlockPos ceilingCenter = findAnyCeiling(level, insideBlocks);
        helper.assertTrue(ceilingCenter != null, "Could not find a ceiling block");

        // Find zone owning the interior.
        Zone zone = findAdjacentZone(level, ceilingCenter);
        helper.assertTrue(zone != null, "No zone adjacent to ceiling");

        // Remove a 3×3 patch of ceiling blocks.
        List<BlockPos> removed = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos candidate = ceilingCenter.offset(dx, 0, dz);
                if (!level.getBlockState(candidate).isAir()) {
                    level.removeBlock(candidate, false);
                    removed.add(candidate);
                }
            }
        }
        helper.assertTrue(!removed.isEmpty(), "No ceiling blocks removed in 3×3 patch");

        // Expand zone into each newly exposed air position.
        for (BlockPos pos : removed) {
            zone.tryExpand(pos, level);
        }
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);
        helper.assertTrue(!zone.isDissolved(), "Zone dissolved after explosion");

        // All old interior blocks must still be INSIDE.
        int wrongCount = 0;
        BlockPos firstWrong = null;
        for (BlockPos pos : insideBlocks) {
            ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(pos.getX() >> 4, pos.getZ() >> 4));
            if (data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15) != ClassificationState.INSIDE) {
                wrongCount++;
                if (firstWrong == null) firstWrong = pos;
            }
        }
        helper.assertTrue(wrongCount == 0, wrongCount + " interior block(s) lost after explosion; first at " + firstWrong);

        Set<BlockPos> afterInside = collectInsideBlocks(level, ox, oz);
        int beforeComponents = countConnectedComponents(insideBlocks);
        int afterComponents = countConnectedComponents(afterInside);
        helper.assertTrue(afterComponents <= beforeComponents,
                "Explosion fragmented interior: " + beforeComponents + " component(s) before, " + afterComponents + " after");

        helper.succeed();
    }

    // Shared template for programmatic ring tests. 12×5×12 empty air space.
    private static final String EMPTY_TEMPLATE = "zoneresiliencegametests.testzonesurvivestcascade";

    /**
     * A sealed room (4×4 walls + ceiling, door) survives 10 re-bootstraps.
     * Each bootstrap reclassifies the chunk and creates a new zone entity;
     * the interior must remain INSIDE and zone-owned throughout.
     */
    /**
     * Diagnostic: builds a 4x4x2 sealed ring with door and verifies
     * that the classifier marks the interior as INSIDE.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testDiagnosticRingClassification(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        cleanChunkOfZones(level, cx, cz);
        BlockPos origin = new BlockPos((cx << 4) + 3, templateOrigin.getY() + 1, (cz << 4) + 3);
        BlockPos doorBottom = buildRingWithDoor(level, origin, true);

        // Verify blocks are actually placed.
        helper.assertTrue(level.getBlockState(origin.offset(0, 0, 0)).is(Blocks.OAK_PLANKS),
                "West wall missing");
        helper.assertTrue(level.getBlockState(origin.offset(0, 2, 0)).is(Blocks.OAK_PLANKS),
                "Ceiling missing");
        helper.assertTrue(level.getBlockState(doorBottom).is(Blocks.OAK_DOOR),
                "Door missing");
        helper.assertTrue(level.getBlockState(origin.offset(1, 0, 1)).isAir(),
                "Interior should be air, got " + level.getBlockState(origin.offset(1, 0, 1)));

        // Check what the chunk sees.
        var chunk = level.getChunk(cx, cz);
        helper.assertTrue(chunk.getBlockState(origin.offset(1, 0, 1)).isAir(),
                "Chunk sees interior as non-air: " + chunk.getBlockState(origin.offset(1, 0, 1)));
        helper.assertTrue(!chunk.getBlockState(doorBottom).isAir(),
                "Chunk sees door as air");

        // Run classifier and verify interior becomes INSIDE.
        new com.sanhiruzu.atelier.space.ChunkClassifier().classify(chunk);
        var data = ChunkClassificationAttachment.get(chunk);
        int insideCount = 0;
        for (int y = 0; y <= 1; y++)
            for (int x = 1; x <= 2; x++)
                for (int z = 1; z <= 2; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    ClassificationState s = data.getBlockState(
                            pos.getX() & 15, pos.getY(), pos.getZ() & 15);
                    if (s == ClassificationState.INSIDE) insideCount++;
                }
        helper.assertTrue(insideCount == 8,
                "Expected 8 INSIDE blocks, got " + insideCount);

        // Verify that a position clearly outside the ring is not INSIDE.
        BlockPos outsidePos = origin.offset(-1, 0, 0);
        ClassificationState outsideState = data.getBlockState(
                outsidePos.getX() & 15, outsidePos.getY(), outsidePos.getZ() & 15);
        helper.assertTrue(outsideState != ClassificationState.INSIDE,
                "Position west of ring should not be INSIDE, got " + outsideState);

        // Check ground level — is there a solid floor under the ring?
        BlockPos underRing = origin.offset(1, -1, 1);
        var groundState = level.getBlockState(underRing);
        helper.assertTrue(!level.getBlockState(underRing).isAir(),
                "Ground under ring at " + underRing + " should be solid, got " + groundState);

        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testOpenTopZoneSurvivesCascade(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        cleanChunkOfZones(level, cx, cz);
        BlockPos origin = new BlockPos((cx << 4) + 3, templateOrigin.getY() + 1, (cz << 4) + 3);
        BlockPos doorBottom = buildRingWithDoor(level, origin, true);

        Set<BlockPos> enclosed = EnclosureDetector.detect(level, doorBottom);
        helper.assertTrue(enclosed != null, "EnclosureDetector found no enclosure after building ring");
        int openingArea = Zone.computeOpeningAreaFor(enclosed, level);
        Zone zone = Zone.createFromBootstrap(UUID.randomUUID(), enclosed, openingArea, level);
        helper.assertTrue(zone != null, "Zone.createFromBootstrap returned null");
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);
        UUID zoneId = zone.getId();
        assertRedBlueConsistency(helper, level, zoneId, cx, cz, "pass 0");

        for (int pass = 1; pass <= 10; pass++) {
            zone.dissolve(level);
            enclosed = EnclosureDetector.detect(level, doorBottom);
            helper.assertTrue(enclosed != null, "EnclosureDetector found no enclosure on pass " + pass);
            openingArea = Zone.computeOpeningAreaFor(enclosed, level);
            zone = Zone.createFromBootstrap(UUID.randomUUID(), enclosed, openingArea, level);
            helper.assertTrue(zone != null, "Zone.createFromBootstrap returned null on pass " + pass);
            ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);
            zoneId = zone.getId();
            assertRedBlueConsistency(helper, level, zoneId, cx, cz, "pass " + pass);
        }

        helper.succeed();
    }

    /**
     * After bootstrap, breaking a wall block adjacent to the zone interior
     * triggers zone expansion. Verifies the zone claims the new air and
     * remains alive.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testSealedRoomSurvivesCascade(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        cleanChunkOfZones(level, cx, cz);
        BlockPos origin = new BlockPos((cx << 4) + 3, templateOrigin.getY() + 1, (cz << 4) + 3);
        BlockPos doorBottom = buildRingWithDoor(level, origin, true);
        Set<BlockPos> enclosed = EnclosureDetector.detect(level, doorBottom);
        helper.assertTrue(enclosed != null, "EnclosureDetector found no enclosure after building ring");
        int enclosingOpening = Zone.computeOpeningAreaFor(enclosed, level);
        Zone zone = Zone.createFromBootstrap(UUID.randomUUID(), enclosed, enclosingOpening, level);
        helper.assertTrue(zone != null, "Zone.createFromBootstrap returned null");
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);
        UUID zoneId = zone.getId();
        int origVolume = zone.getVolume();

        // Build a bounded 1-block enclosure west of the ring wall so the
        // expansion BFS does not leak into the void outside the template.
        // The wall at (0, 0, 1) will be broken; the space at (-1, 0, 1)
        // becomes the extension after being sealed by these five blocks.
        // Clear the barrier-box interior first — stale blocks or registry
        // entries from previous tests in the batch would block expansion.
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        for (int y = -1; y <= 1; y++)
            for (int x = -2; x <= -1; x++)
                for (int z = 0; z <= 2; z++) {
                    BlockPos cp = origin.offset(x, y, z);
                    registry.unmapBlock(cp);
                }
        level.setBlockAndUpdate(origin.offset(-1, 0, 1), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(-2, 0, 1), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(-1, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(-1, 0, 2), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(-1, 1, 1), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(-1, -1, 1), Blocks.OAK_PLANKS.defaultBlockState());

        // Break a wall block at origin.offset(0, 0, 1) — west wall, adjacent to interior.
        BlockPos wall = origin.offset(0, 0, 1);
        helper.assertTrue(!level.getBlockState(wall).isAir(), "Wall block is not solid");
        level.removeBlock(wall, false);

        // Expand zone into the newly exposed air.
        zone.tryExpand(wall, level);
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);
        helper.assertTrue(!zone.isDissolved(), "Zone dissolved after wall break");
        helper.assertTrue(zone.getVolume() >= origVolume + 1,
                "Zone did not expand after wall break: volume was " + origVolume
                        + ", now " + zone.getVolume());

        // The wall-break position must now be INSIDE.
        ChunkClassificationData postData = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        ClassificationState wallState = postData.getBlockState(wall.getX() & 15, wall.getY(), wall.getZ() & 15);
        helper.assertTrue(wallState == ClassificationState.INSIDE,
                "Broken wall position is " + wallState + ", expected INSIDE");

        // Zone still has a live entry and registry consistency.
        helper.assertTrue(zone.hasLiveEntry(level), "Zone lacks live entry after wall break");
        assertRedBlueConsistency(helper, level, zoneId, cx, cz, "post-expand");

        helper.succeed();
    }

    /**
     * Removing the door causes the zone to dissolve: interior blocks are unclaimed
     * and revert to SOLID. Only the ring's specific interior positions are checked.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testZoneDissolveOnDoorRemoval(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        cleanChunkOfZones(level, cx, cz);
        BlockPos origin = new BlockPos((cx << 4) + 3, templateOrigin.getY() + 1, (cz << 4) + 3);
        BlockPos doorBottom = buildRingWithDoor(level, origin, true);
        BlockPos doorTop = doorBottom.above();

        // Verify ring construction: perimeter walls + ceiling + door solid, interior air.
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                boolean isPerimeter = (x == 0 || x == 3 || z == 0 || z == 3);
                boolean isDoor = (x == 2 && z == 0);
                for (int y = 0; y <= 1; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (isDoor) {
                        // door fills this gap — skip wall check
                    } else if (isPerimeter) {
                        helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_PLANKS),
                                "Wall missing at " + pos);
                    } else {
                        var state = level.getBlockState(pos);
                        helper.assertTrue(state.isAir(),
                                "Interior not air at " + pos + " — is " + state.getBlock());
                    }
                }
            }
        }
        for (int x = 0; x < 4; x++)
            for (int z = 0; z < 4; z++)
                helper.assertTrue(level.getBlockState(origin.offset(x, 2, z)).is(Blocks.OAK_PLANKS),
                        "Ceiling missing at " + origin.offset(x, 2, z));
        helper.assertTrue(level.getBlockState(doorBottom).is(Blocks.OAK_DOOR),
                "Door bottom missing at " + doorBottom);
        helper.assertTrue(level.getBlockState(doorTop).is(Blocks.OAK_DOOR),
                "Door top missing at " + doorTop);

        Set<BlockPos> enclosed = EnclosureDetector.detect(level, doorBottom);
        helper.assertTrue(enclosed != null, "EnclosureDetector found no enclosure after building ring");

        // Collect the ring's specific interior positions.
        Set<BlockPos> ringInterior = new HashSet<>();
        for (int y = 0; y <= 1; y++)
            for (int x = 1; x <= 2; x++)
                for (int z = 1; z <= 2; z++)
                    ringInterior.add(origin.offset(x, y, z));

        // Verify all expected interior blocks are in the detected enclosure.
        for (BlockPos pos : ringInterior) {
            helper.assertTrue(enclosed.contains(pos),
                    "Ring interior block missing from enclosure: " + pos);
        }

        int dissolveOpening = Zone.computeOpeningAreaFor(enclosed, level);
        Zone zone = Zone.createFromBootstrap(UUID.randomUUID(), enclosed, dissolveOpening, level);
        helper.assertTrue(zone != null, "Zone.createFromBootstrap returned null");
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);

        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        for (BlockPos pos : ringInterior) {
            ClassificationState state = data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
            helper.assertTrue(state == ClassificationState.INSIDE,
                    "Ring interior not INSIDE before door removal at " + pos + " state=" + state);
        }

        UUID ringZoneId = zone.getId();
        helper.assertTrue(ringZoneId != null, "No zone owns the ring interior before door removal");

        // Remove door and manually dissolve (GameTest events don't fire).
        level.removeBlock(doorBottom, false);
        level.removeBlock(doorTop, false);
        helper.assertTrue(!zone.hasLiveEntry(level), "Zone should lack live entry after door removal");
        zone.dissolve(level);

        // Ring interior positions must no longer be INSIDE.
        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        for (BlockPos pos : ringInterior) {
            ClassificationState state = data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
            helper.assertTrue(state != ClassificationState.INSIDE,
                    "Ring interior still INSIDE after door removal at " + pos + " state=" + state);
        }

        // Zone no longer owns any blocks.
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        Set<BlockPos> remaining = registry.getBlocksInRegion(ringZoneId);
        helper.assertTrue(remaining.isEmpty(),
                "Zone still has " + remaining.size() + " blocks after dissolution");

        helper.succeed();
    }

    /**
     * Replacing a door revives the zone: remove door → dissolve → place new door
     * → enclosure detection + bootstrap → zone reforms.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testDoorReplacementRevivesZone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        cleanChunkOfZones(level, cx, cz);
        BlockPos origin = new BlockPos((cx << 4) + 3, templateOrigin.getY() + 1, (cz << 4) + 3);
        BlockPos doorBottom = buildRingWithDoor(level, origin, true);
        BlockPos doorTop = doorBottom.above();

        // Track the ring's specific interior for targeted assertions.
        Set<BlockPos> ringInterior = new HashSet<>();
        for (int y = 0; y <= 1; y++)
            for (int x = 1; x <= 2; x++)
                for (int z = 1; z <= 2; z++)
                    ringInterior.add(origin.offset(x, y, z));

        Set<BlockPos> enclosed = EnclosureDetector.detect(level, doorBottom);
        helper.assertTrue(enclosed != null, "EnclosureDetector found no enclosure after building ring");
        for (BlockPos pos : ringInterior) {
            helper.assertTrue(enclosed.contains(pos),
                    "Ring interior block missing from enclosure: " + pos);
        }
        int replacementOpening = Zone.computeOpeningAreaFor(enclosed, level);
        Zone zone = Zone.createFromBootstrap(UUID.randomUUID(), enclosed, replacementOpening, level);
        helper.assertTrue(zone != null, "Zone.createFromBootstrap returned null");
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);
        UUID ringZoneId = zone.getId();

        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        for (BlockPos pos : ringInterior) {
            ClassificationState state = data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
            helper.assertTrue(state == ClassificationState.INSIDE,
                    "Ring interior not INSIDE initially at " + pos + " state=" + state);
        }

        // Remove oak door → dissolve.
        level.removeBlock(doorBottom, false);
        level.removeBlock(doorTop, false);
        helper.assertTrue(!zone.hasLiveEntry(level), "Zone should lack live entry after door removal");
        zone.dissolve(level);

        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        for (BlockPos pos : ringInterior) {
            ClassificationState state = data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
            helper.assertTrue(state != ClassificationState.INSIDE,
                    "Ring interior still INSIDE after door removal at " + pos + " state=" + state);
        }

        // Replace with iron door.
        level.setBlockAndUpdate(doorBottom, Blocks.IRON_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        level.setBlockAndUpdate(doorTop, Blocks.IRON_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // Re-detect enclosure and revive zone via EnclosureDetector.
        Set<BlockPos> revivedEnclosed = EnclosureDetector.detect(level, doorBottom);
        helper.assertTrue(revivedEnclosed != null, "EnclosureDetector found no enclosure after door replacement");
        int revivedOpening = Zone.computeOpeningAreaFor(revivedEnclosed, level);
        Zone revivedZone = Zone.createFromBootstrap(UUID.randomUUID(), revivedEnclosed, revivedOpening, level);
        helper.assertTrue(revivedZone != null, "Zone.createFromBootstrap returned null after revival");

        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        for (BlockPos pos : ringInterior) {
            ClassificationState state = data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
            helper.assertTrue(state == ClassificationState.INSIDE,
                    "Ring interior not INSIDE after revival at " + pos + " state=" + state);
        }
        UUID revivedZoneId = revivedZone.getId();
        helper.assertTrue(revivedZoneId != null,
                "ZoneRegistry has no zone after door replacement");
        assertRedBlueConsistency(helper, level, revivedZoneId, cx, cz, "revived");

        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // Shared structure builder
    // -------------------------------------------------------------------------

    /**
     * Zone is detected only after placing the door that seals the final gap.
     *
     * <p>The test builds a 4×4 sealed ring + ceiling but deliberately leaves a
     * 2-block-tall gap where the door will go.  While the gap is open, the
     * interior air is connected to the outside → {@link com.sanhiruzu.atelier.space.EnclosureDetector}
     * cannot find a bounded enclosure, so no zone is created.
     * Placing the door seals the gap: the enclosure detector now returns the bounded
     * interior, {@link Zone#createFromBootstrap} confirms a live entry (the door), and
     * the zone is registered.  Removing the door dissolves it; replacing it revives it.</p>
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testZoneDetectedOnDoorPlacement(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        cleanChunkOfZones(level, cx, cz);
        BlockPos origin = new BlockPos((cx << 4) + 3, templateOrigin.getY() + 1, (cz << 4) + 3);

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        ZoneRegistry zoneRegistry = ZoneRegistry.get(level);
        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);

        // Interior positions (2×2×2 = 8 blocks): x=1..2, z=1..2, y=0..1 relative to origin.
        Set<BlockPos> interior = new HashSet<>();
        for (int y = 0; y <= 1; y++)
            for (int x = 1; x <= 2; x++)
                for (int z = 1; z <= 2; z++)
                    interior.add(origin.offset(x, y, z));

        // ---- Step 1: Build the ring + ceiling WITHOUT the door (gap at x=2, z=0) ----
        // Floor — seals the bottom so the only leak path is through the door gap
        for (int x = 0; x < 4; x++)
            for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.OAK_PLANKS.defaultBlockState());
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                if (x == 0 || x == 3 || z == 0 || z == 3) {
                    if (x == 2 && z == 0) continue; // leave door gap
                    level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
                    level.setBlockAndUpdate(origin.offset(x, 1, z), Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
        }
        for (int x = 0; x < 4; x++)
            for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(origin.offset(x, 2, z), Blocks.OAK_PLANKS.defaultBlockState());

        // Ensure door gap is truly open — previous test's door blocks or stale
        // registry entries at these positions would seal the gap and cause a
        // false-positive enclosure detection.
        level.setBlockAndUpdate(origin.offset(2, 0, 0), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(2, 1, 0), Blocks.AIR.defaultBlockState());
        // Also unmap stale registry entries in the entire ring interior — orphaned
        // blockToRegion entries from previous tests block EnclosureDetector BFS.
        for (int y = 0; y <= 1; y++)
            for (int x = 1; x <= 2; x++)
                for (int z = 1; z <= 2; z++)
                    registry.unmapBlock(origin.offset(x, y, z));
        registry.unmapBlock(origin.offset(2, 0, 0));
        registry.unmapBlock(origin.offset(2, 1, 0));

        // Verify: interior air is open through the gap → no bounded enclosure.
        // Simulate onBlockPlace for a ceiling block (last block placed).
        BlockPos lastCeilingBlock = origin.offset(2, 2, 2);
        Set<BlockPos> leakyDetect = EnclosureDetector.detect(level, lastCeilingBlock);
        helper.assertTrue(leakyDetect == null,
                "Enclosure detected before door is placed — interior air should leak through the door gap");
        for (BlockPos pos : interior) {
            helper.assertTrue(registry.getRegionIdAt(pos) == null,
                    "Interior block unexpectedly in a zone before door placement: " + pos);
        }

        // ---- Step 2: Place the door (seals the gap) ----
        BlockPos doorBottom = origin.offset(2, 0, 0);
        level.setBlockAndUpdate(doorBottom, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        level.setBlockAndUpdate(doorBottom.above(), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // Simulate onBlockPlace for the door (level.setBlockAndUpdate doesn't fire EntityPlaceEvent).
        Set<BlockPos> enclosed = EnclosureDetector.detect(level, doorBottom);
        helper.assertTrue(enclosed != null, "EnclosureDetector found no enclosure after door placement");
        helper.assertTrue(!enclosed.isEmpty(), "EnclosureDetector returned empty set");
        for (BlockPos pos : interior) {
            helper.assertTrue(enclosed.contains(pos),
                    "Interior block missing from detected enclosure: " + pos);
        }

        int openingArea = Zone.computeOpeningAreaFor(enclosed, level);
        Zone zone = Zone.createFromBootstrap(UUID.randomUUID(), enclosed, openingArea, level);
        helper.assertTrue(zone != null,
                "Zone.createFromBootstrap returned null — missing door entry or other validation failure");

        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);
        ZoneData zoneData = zoneRegistry.getZone(zone.getId());
        helper.assertTrue(zoneData != null, "ZoneData not in registry after zone creation");

        for (BlockPos pos : interior) {
            helper.assertTrue(registry.getRegionIdAt(pos) != null,
                    "Interior block not registered in zone after door placement: " + pos);
        }

        // ---- Step 3: Remove the door → zone dissolves ----
        level.removeBlock(doorBottom, false);
        level.removeBlock(doorBottom.above(), false);

        helper.assertTrue(!zone.hasLiveEntry(level, doorBottom),
                "Zone still reports a live entry after door removal");
        zone.dissolve(level);
        helper.assertTrue(zone.isDissolved(), "Zone did not dissolve");

        for (BlockPos pos : interior) {
            helper.assertTrue(registry.getRegionIdAt(pos) == null,
                    "Interior block still in registry after dissolution: " + pos);
        }

        // ---- Step 4: Replace door → zone revives ----
        level.setBlockAndUpdate(doorBottom, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        level.setBlockAndUpdate(doorBottom.above(), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        Set<BlockPos> revivedEnclosed = EnclosureDetector.detect(level, doorBottom);
        helper.assertTrue(revivedEnclosed != null, "EnclosureDetector found no enclosure after door replacement");
        Zone revivedZone = Zone.createFromBootstrap(UUID.randomUUID(), revivedEnclosed,
                Zone.computeOpeningAreaFor(revivedEnclosed, level), level);
        helper.assertTrue(revivedZone != null, "Zone not created after door replacement");

        for (BlockPos pos : interior) {
            helper.assertTrue(registry.getRegionIdAt(pos) != null,
                    "Interior block not in revived zone: " + pos);
        }

        helper.succeed();
    }

    /**
     * Zone grows when the player breaks a wall and then encloses the extension.
     *
     * <p>Scenario:
     * <ol>
     *   <li>Bootstrap a sealed ring → zone exists (2×2 interior, 4 blocks).</li>
     *   <li>Break the south wall (z=3, x=1) — gap opens but extension is not yet enclosed.</li>
     *   <li>Build outer south wall one column further south (z=4, full row) + connect side walls.</li>
     *   <li>Ceiling already extends over both areas.</li>
     *   <li>Last wall block placed → {@link EnclosureDetector} finds the extension, detects adjacency
     *       to the existing zone, and expands it. Zone grows from 4 to 6 interior blocks.</li>
     * </ol></p>
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testZoneGrowsWhenRoomExtended(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        cleanChunkOfZones(level, cx, cz);
        BlockPos origin = new BlockPos((cx << 4) + 2, templateOrigin.getY() + 1, (cz << 4) + 2);

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        ZoneRegistry zoneRegistry = ZoneRegistry.get(level);

        // ---- Build initial 4×4 sealed ring with door (2×2 interior at y=0,1) ----
        BlockPos doorBottom = buildRingWithDoor(level, origin, true);
        Set<BlockPos> enclosed = EnclosureDetector.detect(level, doorBottom);
        helper.assertTrue(enclosed != null, "EnclosureDetector found no enclosure after building ring");
        int growOpening = Zone.computeOpeningAreaFor(enclosed, level);
        Zone zone = Zone.createFromBootstrap(UUID.randomUUID(), enclosed, growOpening, level);
        helper.assertTrue(zone != null, "Zone.createFromBootstrap returned null");
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);
        int initialVolume = zone.getVolume();

        // ---- Break south wall (x=1, z=3, y=0) — creates a gap to outside ----
        // The ring's south wall runs along z=3. Break one block there.
        BlockPos brokenWall = origin.offset(1, 0, 3);
        helper.assertTrue(!level.getBlockState(brokenWall).isAir(),
                "Expected south wall to be solid at " + brokenWall);
        level.removeBlock(brokenWall, false);
        // tryExpand — extension is open (no south enclosure yet), zone stays same size.
        Zone.ExpansionResult exp = zone.tryExpand(brokenWall, level);
        helper.assertTrue(exp.blocksClaimed() == 0,
                "Zone should NOT expand into open space, got " + exp.blocksClaimed() + " blocks claimed");
        helper.assertTrue(zone.getVolume() == initialVolume,
                "Volume changed unexpectedly after breaking to open space");

        // ---- Extend: add a new south wall at z=5 and connect the side walls ----
        // Clear the entire extension footprint (air + unmap stale registry entries)
        // so that previous-test debris doesn't interfere with EnclosureDetector.
        for (int x = 0; x < 4; x++) {
            for (int z = 4; z <= 5; z++) {
                for (int y = -1; y <= 2; y++) {
                    BlockPos cp = origin.offset(x, y, z);
                    registry.unmapBlock(cp);
                    level.setBlockAndUpdate(cp, Blocks.AIR.defaultBlockState());
                }
            }
        }
        // Also clear the broken-wall gap itself — stale registry entries here would
        // block adjacency detection between the extension enclosure and the existing zone.
        registry.unmapBlock(origin.offset(1, 0, 3));
        registry.unmapBlock(origin.offset(1, 1, 3));

        // Floor for entire extension area
        for (int x = 0; x < 4; x++) {
            level.setBlockAndUpdate(origin.offset(x, -1, 4), Blocks.OAK_PLANKS.defaultBlockState());
            level.setBlockAndUpdate(origin.offset(x, -1, 5), Blocks.OAK_PLANKS.defaultBlockState());
        }
        // Side extensions (x=0 and x=3, z=4 and z=5) close the sides of the extension corridor.
        level.setBlockAndUpdate(origin.offset(0, 0, 4), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(0, 1, 4), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(3, 0, 4), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(3, 1, 4), Blocks.OAK_PLANKS.defaultBlockState());
        // Ceiling over extension
        level.setBlockAndUpdate(origin.offset(0, 2, 4), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(1, 2, 4), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(2, 2, 4), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(3, 2, 4), Blocks.OAK_PLANKS.defaultBlockState());

        // ---- Place last sealing block: south wall at z=4 ----
        // This is the block that completes the extension enclosure.
        // After this, the extension air (x=1..2, z=4, y=0..1) is bounded and adjacent
        // to the existing zone through the broken-wall gap at (x=1, z=3).
        Set<BlockPos> extensionAir = new HashSet<>();
        for (int y = 0; y <= 1; y++) {
            extensionAir.add(origin.offset(1, y, 4));
            extensionAir.add(origin.offset(2, y, 4));
        }
        // Place the south wall piece at x=1 (the last block that seals the extension).
        BlockPos lastWall = origin.offset(1, 0, 5);
        // Extend side walls properly: place the actual south wall.
        level.setBlockAndUpdate(origin.offset(0, 0, 5), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(0, 1, 5), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(1, 0, 5), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(1, 1, 5), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(2, 0, 5), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(2, 1, 5), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(3, 0, 5), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(3, 1, 5), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(0, 2, 5), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(1, 2, 5), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(2, 2, 5), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(3, 2, 5), Blocks.OAK_PLANKS.defaultBlockState());

        // Simulate the onBlockPlace detection for the last sealing block.
        Set<BlockPos> detected = EnclosureDetector.detect(level, lastWall);
        helper.assertTrue(detected != null,
                "EnclosureDetector found no enclosure after completing the extension");

        // Detected region should be adjacent to the existing zone.
        Zone adjacentZone = null;
        outer:
        for (BlockPos ipos : detected) {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                UUID adjId = registry.getRegionIdAt(ipos.relative(dir));
                if (adjId != null) {
                    Zone adj = zoneRegistry.getZoneById(adjId);
                    if (adj != null && !adj.isDissolved()) {
                        adjacentZone = adj;
                        break outer;
                    }
                }
            }
        }
        helper.assertTrue(adjacentZone != null,
                "Extension enclosure is not adjacent to the existing zone");
        helper.assertTrue(adjacentZone.getId().equals(zone.getId()),
                "Adjacent zone is not the original zone");

        // Expand the existing zone into the extension.
        BlockPos seed = detected.iterator().next();
        adjacentZone.tryExpand(seed, level);
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(adjacentZone, level);

        helper.assertTrue(zone.getVolume() > initialVolume,
                "Zone volume did not grow after room extension: was " + initialVolume
                        + ", now " + zone.getVolume());

        // Extension air blocks should now belong to the zone.
        for (BlockPos ext : extensionAir) {
            UUID extId = registry.getRegionIdAt(ext);
            helper.assertTrue(extId != null && extId.equals(zone.getId()),
                    "Extension block " + ext + " not in zone after expansion");
        }

        helper.succeed();
    }

    /**
     * A 1-block-high room (floor at y=-1, walls at y=0, ceiling at y=1) drops into
     * grace period when any wall block at the interior's Y level is removed, because
     * {@code breachesMinimumEnclosure} fires (pos.getY() == zone.getMinY()).
     * The zone should be disabled and its interior preserved during the grace period,
     * then dissolved when the timer expires.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testOneHighRoomEntersGracePeriodOnWallBreach(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        cleanChunkOfZones(level, cx, cz);
        BlockPos origin = new BlockPos((cx << 4) + 3, templateOrigin.getY() + 1, (cz << 4) + 3);
        BlockPos doorBottom = buildOneHighRingWithDoor(level, origin);
        Set<BlockPos> enclosed = EnclosureDetector.detect(level, doorBottom);
        helper.assertTrue(enclosed != null, "EnclosureDetector found no enclosure after building 1-high ring");

        // Verify zone created — 4 interior blocks at y=0, x=1..2, z=1..2.
        Set<BlockPos> interior = new HashSet<>();
        for (int x = 1; x <= 2; x++)
            for (int z = 1; z <= 2; z++)
                interior.add(origin.offset(x, 0, z));
        helper.assertTrue(interior.size() == 4, "Expected 4 interior blocks");
        for (BlockPos pos : interior) {
            helper.assertTrue(enclosed.contains(pos),
                    "Interior block missing from enclosure: " + pos);
        }

        int graceOpening = Zone.computeOpeningAreaFor(enclosed, level);
        Zone zone = Zone.createFromBootstrap(UUID.randomUUID(), enclosed, graceOpening, level);
        helper.assertTrue(zone != null, "Zone.createFromBootstrap returned null");
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);
        UUID zoneId = zone.getId();

        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        for (BlockPos pos : interior) {
            ClassificationState s = data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
            helper.assertTrue(s == ClassificationState.INSIDE,
                    "Interior not INSIDE at " + pos + " state=" + s);
        }

        // Remove a wall block at the room's only interior Y level → breachesMinimumEnclosure.
        BlockPos wall = origin.offset(0, 0, 1); // west wall, middle
        helper.assertTrue(zone.getMinY() == wall.getY(),
                "Wall Y should match zone minY for breachesMinimumEnclosure");
        level.removeBlock(wall, false);

        // Simulate ClassificationEventHandler.onBlockBreak: tryExpand, then enterGracePeriod.
        zone.tryExpand(wall, level);
        ZoneRegistry.get(level).enterGracePeriod(zone.getId(), level);
        ZoneData zd = ZoneRegistry.get(level).getZone(zoneId);
        helper.assertTrue(zd != null && zd.isDisabled(),
                "Zone should be disabled after wall breach");
        helper.assertTrue(ZoneRegistry.get(level).isInGracePeriod(zoneId),
                "Zone should be in grace period");
        helper.assertTrue(!zone.isDissolved(), "Zone should not dissolve during grace period");

        // Interior blocks must remain INSIDE during grace period (preserved for recovery).
        for (BlockPos pos : interior) {
            ClassificationState s = data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
            helper.assertTrue(s == ClassificationState.INSIDE,
                    "Interior block " + pos + " should remain INSIDE during grace period, got " + s);
        }

        // Expire the grace period — dissolving the zone.
        zone.dissolve(level);
        helper.assertTrue(zone.isDissolved(), "Zone should be dissolved after grace expiry");

        // Interior blocks must no longer be INSIDE after dissolution.
        data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        for (BlockPos pos : interior) {
            ClassificationState s = data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
            helper.assertTrue(s != ClassificationState.INSIDE,
                    "Interior block " + pos + " should not be INSIDE after dissolution, got " + s);
        }

        helper.succeed();
    }

    /**
     * A 1-block-high room that enters grace period after a wall breach recovers
     * when the wall block is replaced: the opening area shrinks back, the zone
     * is re-evaluated, and the grace period is cancelled.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = EMPTY_TEMPLATE)
    public static void testOneHighRoomRecoversWhenBreachRepaired(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int cx = templateOrigin.getX() >> 4, cz = templateOrigin.getZ() >> 4;
        cleanChunkOfZones(level, cx, cz);
        BlockPos origin = new BlockPos((cx << 4) + 3, templateOrigin.getY() + 1, (cz << 4) + 3);
        BlockPos doorBottom = buildOneHighRingWithDoor(level, origin);
        Set<BlockPos> enclosed = EnclosureDetector.detect(level, doorBottom);
        helper.assertTrue(enclosed != null, "EnclosureDetector found no enclosure after building 1-high ring");

        Set<BlockPos> interior = new HashSet<>();
        for (int x = 1; x <= 2; x++)
            for (int z = 1; z <= 2; z++)
                interior.add(origin.offset(x, 0, z));
        for (BlockPos pos : interior) {
            helper.assertTrue(enclosed.contains(pos),
                    "Interior block missing from enclosure: " + pos);
        }

        int recoverOpening = Zone.computeOpeningAreaFor(enclosed, level);
        Zone zone = Zone.createFromBootstrap(UUID.randomUUID(), enclosed, recoverOpening, level);
        helper.assertTrue(zone != null, "Zone.createFromBootstrap returned null");
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);
        UUID zoneId = zone.getId();

        // ---- Breach: remove wall block → grace period ----
        BlockPos wall = origin.offset(0, 0, 1);
        level.removeBlock(wall, false);
        zone.tryExpand(wall, level);
        ZoneRegistry.get(level).enterGracePeriod(zone.getId(), level);
        ZoneData zd = ZoneRegistry.get(level).getZone(zoneId);
        helper.assertTrue(zd != null && zd.isDisabled(), "Zone should be disabled after breach");
        helper.assertTrue(ZoneRegistry.get(level).isInGracePeriod(zoneId), "Zone should be in grace period");

        // ---- Repair: replace the wall block ----
        level.setBlockAndUpdate(wall, Blocks.OAK_PLANKS.defaultBlockState());
        zone.refreshOpeningArea(level);
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level);

        // Zone should recover — disabled flag cleared, grace period cancelled.
        helper.assertTrue(!zone.isDissolved(), "Zone should not be dissolved after repair");
        ZoneRegistry.get(level).evaluateRoomAndSyncNow(zone, level); // second call to ensure saved data is current
        zd = ZoneRegistry.get(level).getZone(zoneId);
        helper.assertTrue(zd != null, "ZoneData should exist after recovery");
        // After recovery, the zone should no longer be disabled.
        // getOrEvaluate clears the disabled flag, so re-fetching gets the fresh state.
        zd = ZoneRegistry.get(level).getOrEvaluateRoom(zoneId, level);
        helper.assertTrue(zd != null, "ZoneData should evaluate after recovery");
        helper.assertTrue(!zd.isDisabled(), "Zone should not be disabled after repair");
        helper.assertTrue(!ZoneRegistry.get(level).isInGracePeriod(zoneId),
                "Zone should not be in grace period after repair");

        // Interior blocks must be INSIDE and owned by the recovered zone.
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        for (BlockPos pos : interior) {
            ClassificationState s = data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
            helper.assertTrue(s == ClassificationState.INSIDE,
                    "Interior not INSIDE after recovery at " + pos + " state=" + s);
            UUID mapped = registry.getRegionIdAt(pos);
            helper.assertTrue(zoneId.equals(mapped),
                    "Interior block " + pos + " maps to " + mapped + " instead of " + zoneId);
        }

        helper.succeed();
    }

    /**
     * Builds a 4×4×2 ring of oak planks with a door gap at (origin + 2, y, 0).
     * Includes a floor at y=-1 (relative to origin) so the enclosure is fully sealed
     * regardless of what the template or world generation provides.
     * If {@code addCeiling} is true, seals the top with oak planks.
     * Returns the BlockPos of the door's lower half.
     */
    private static BlockPos buildRingWithDoor(ServerLevel level, BlockPos origin, boolean addCeiling) {
        // Unmap any stale SpaceRegion entries in the ring footprint — previous
        // tests may leave orphaned blockToRegion entries for now-solid blocks
        // that the EnclosureDetector BFS would otherwise treat as barriers.
        SpaceRegionRegistry reg = SpaceRegionRegistry.get(level);
        int ceilingY = addCeiling ? 2 : 1;
        for (int y = -1; y <= ceilingY + 1; y++)
            for (int x = -1; x <= 4; x++)
                for (int z = -1; z <= 4; z++)
                    reg.unmapBlock(origin.offset(x, y, z));

        // Clear interior air — previous tests may have left solid blocks here.
        for (int y = 0; y < ceilingY; y++)
            for (int x = 1; x <= 2; x++)
                for (int z = 1; z <= 2; z++)
                    level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());

        // Floor — ensures ring is sealed even on void/air-only templates
        for (int x = 0; x < 4; x++)
            for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.OAK_PLANKS.defaultBlockState());

        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                if (x == 0 || x == 3 || z == 0 || z == 3) {
                    if (x == 2 && z == 0) continue; // door gap at both y=0 and y=1
                    level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
                    level.setBlockAndUpdate(origin.offset(x, 1, z), Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
        }
        if (addCeiling) {
            for (int x = 0; x < 4; x++)
                for (int z = 0; z < 4; z++)
                    level.setBlockAndUpdate(origin.offset(x, 2, z), Blocks.OAK_PLANKS.defaultBlockState());
        }

        BlockPos doorBottom = origin.offset(2, 0, 0);
        level.setBlockAndUpdate(doorBottom, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        level.setBlockAndUpdate(doorBottom.above(), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        return doorBottom;
    }

    /**
     * Builds a 4×4×1 ring (1-high walls at y=0, ceiling at y=1) with a door gap
     * at (origin + 2, y, 0).  The ceiling has a gap for the door's upper half.
     * Floor at y=-1.  Returns the BlockPos of the door's lower half.
     */
    private static BlockPos buildOneHighRingWithDoor(ServerLevel level, BlockPos origin) {
        // Unmap stale SpaceRegion entries in the ring footprint.
        SpaceRegionRegistry reg = SpaceRegionRegistry.get(level);
        for (int y = -1; y <= 2; y++)
            for (int x = -1; x <= 4; x++)
                for (int z = -1; z <= 4; z++)
                    reg.unmapBlock(origin.offset(x, y, z));

        // Clear interior air — previous tests may have left solid blocks here.
        for (int x = 1; x <= 2; x++)
            for (int z = 1; z <= 2; z++)
                level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.AIR.defaultBlockState());

        // Floor
        for (int x = 0; x < 4; x++)
            for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.OAK_PLANKS.defaultBlockState());

        // Walls at y=0 only (1-high), with door gap at x=2, z=0
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                if (x == 0 || x == 3 || z == 0 || z == 3) {
                    if (x == 2 && z == 0) continue; // door gap
                    level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
        }

        // Ceiling at y=1, with door gap
        for (int x = 0; x < 4; x++)
            for (int z = 0; z < 4; z++)
                if (!(x == 2 && z == 0)) // leave gap for door upper half
                    level.setBlockAndUpdate(origin.offset(x, 1, z), Blocks.OAK_PLANKS.defaultBlockState());

        // Door
        BlockPos doorBottom = origin.offset(2, 0, 0);
        level.setBlockAndUpdate(doorBottom, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        level.setBlockAndUpdate(doorBottom.above(), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        return doorBottom;
    }

    // -------------------------------------------------------------------------
    // Helpers — interior collection
    // -------------------------------------------------------------------------

    private static Set<BlockPos> collectInsideBlocks(ServerLevel level, int ox, int oz) {
        Set<BlockPos> result = new HashSet<>();
        for (int cx = ox; cx <= ox + 1; cx++) {
            int baseX = cx << 4;
            for (int cz = oz; cz <= oz + 1; cz++) {
                int baseZ = cz << 4;
                ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
                for (int lx = 0; lx < 16; lx++)
                    for (int lz = 0; lz < 16; lz++)
                        for (int y = -64; y < 320; y++)
                            if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE)
                                result.add(new BlockPos(baseX + lx, y, baseZ + lz));
            }
        }
        return result;
    }

    private static Set<BlockPos> collectInsideBlocksInChunk(ServerLevel level, int cx, int cz) {
        Set<BlockPos> result = new HashSet<>();
        int baseX = cx << 4, baseZ = cz << 4;
        ChunkClassificationData data = ChunkClassificationAttachment.get(level.getChunk(cx, cz));
        for (int lx = 0; lx < 16; lx++)
            for (int lz = 0; lz < 16; lz++)
                for (int y = -64; y < 320; y++)
                    if (data.getBlockState(lx, y, lz) == ClassificationState.INSIDE)
                        result.add(new BlockPos(baseX + lx, y, baseZ + lz));
        return result;
    }

    // -------------------------------------------------------------------------
    // Helpers — ceiling location
    // -------------------------------------------------------------------------

    private static BlockPos findCeilingNearX(ServerLevel level, Set<BlockPos> insideBlocks, int targetX, int radius) {
        for (BlockPos pos : insideBlocks) {
            if (Math.abs(pos.getX() - targetX) > radius) continue;
            BlockPos above = pos.above();
            if (!level.getBlockState(above).isAir()) return above;
        }
        return null;
    }

    private static BlockPos findAnyCeiling(ServerLevel level, Set<BlockPos> insideBlocks) {
        for (BlockPos pos : insideBlocks) {
            BlockPos above = pos.above();
            if (!level.getBlockState(above).isAir()) return above;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Helpers — zone lookup
    // -------------------------------------------------------------------------

    /**
     * Returns the first live zone adjacent to {@code pos}, or null.
     */
    private static Zone findAdjacentZone(ServerLevel level, BlockPos pos) {
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        ZoneRegistry zoneRegistry = ZoneRegistry.get(level);
        for (Direction dir : Direction.values()) {
            UUID id = registry.getRegionIdAt(pos.relative(dir));
            if (id == null) continue;
            Zone zone = zoneRegistry.getZoneById(id);
            if (zone != null && !zone.isDissolved()) return zone;
        }
        return null;
    }

    /**
     * Returns the zone UUID for any interior block, or null if none.
     */
    private static UUID getZoneIdForAnyInteriorBlock(ServerLevel level, Set<BlockPos> interiorBlocks) {
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        for (BlockPos pos : interiorBlocks) {
            UUID id = registry.getRegionIdAt(pos);
            if (id != null) return id;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Helpers — validation / connectivity
    // -------------------------------------------------------------------------

    /**
     * Dissolves every zone touching {@code chunkX, chunkZ} and resets the chunk
     * bitfield to SOLID.  Prevents restored zones from previous test runs from
     * interfering with EnclosureDetector, Zone.tryExpand, or createFromBootstrap.
     */
    private static void cleanChunkOfZones(ServerLevel level, int chunkX, int chunkZ) {
        ZoneRegistry zoneRegistry = ZoneRegistry.get(level);
        ChunkClassificationData data = ChunkClassificationAttachment.get(
                level.getChunk(chunkX, chunkZ));

        // Dissolve any zone with at least one block in this chunk.
        for (Zone zone : new ArrayList<>(zoneRegistry.getAllZones())) {
            if (zone.isDissolved()) continue;
            for (BlockPos pos : zone.getInteriorBlocks()) {
                if (pos.getX() >> 4 == chunkX && pos.getZ() >> 4 == chunkZ) {
                    zone.dissolve(level);
                    break;
                }
            }
        }

        // Also discard the scheduler's pre-cached restored-zone data for this chunk,
        // otherwise bootstrapChunk step 0 will re-mark the dissolved blocks as INSIDE.
        ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(level);
        ZoneRegistry.get(level).clearRestoredDataForChunk(chunkX, chunkZ);

        // Reset the entire chunk bitfield so clearStates() won't preserve stale INSIDE.
        for (int lx = 0; lx < 16; lx++)
            for (int lz = 0; lz < 16; lz++)
                for (int y = -64; y < 320; y++)
                    data.setBlockState(lx, y, lz, ClassificationState.SOLID);
        data.clearRegions();
        data.setDirty(true);

        // Sweep orphaned SpaceRegion entries for this chunk — blocks whose
        // region UUID no longer maps to any live zone after dissolution.
        SpaceRegionRegistry sr = SpaceRegionRegistry.get(level);
        Set<UUID> liveIds = new HashSet<>();
        for (Zone z : zoneRegistry.getAllZones())
            if (!z.isDissolved()) liveIds.add(z.getId());
        for (UUID regionId : new ArrayList<>(sr.getAllRegionIds())) {
            if (liveIds.contains(regionId)) continue;
            Set<BlockPos> blocks = sr.getBlocksInRegion(regionId);
            boolean touches = false;
            for (BlockPos pos : blocks) {
                if (pos.getX() >> 4 == chunkX && pos.getZ() >> 4 == chunkZ) {
                    touches = true;
                    break;
                }
            }
            if (touches || blocks.isEmpty())
                sr.removeRegion(regionId);
        }

        // Sweep orphaned blockToRegion entries — stale mappings for now-solid
        // blocks whose region was already removed.  Without this, EnclosureDetector
        // BFS treats them as barriers and skips what should be claimable air.
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        for (int lx = 0; lx < 16; lx++)
            for (int lz = 0; lz < 16; lz++)
                for (int y = -64; y <= 0; y++)
                    sr.unmapBlock(new BlockPos(baseX + lx, y, baseZ + lz));
    }

    /**
     * Verifies three-way consistency between the classification bitfield,
     * SpaceRegionRegistry, and ZoneData for a given zone.
     */
    private static void assertRedBlueConsistency(GameTestHelper helper, ServerLevel level,
                                                 UUID zoneId, int chunkX, int chunkZ, String label) {
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        ZoneRegistry zoneReg = ZoneRegistry.get(level);
        ChunkClassificationData data = ChunkClassificationAttachment.get(
                level.getChunk(chunkX, chunkZ));
        int baseX = chunkX << 4, baseZ = chunkZ << 4;

        Set<BlockPos> registryBlocks = registry.getBlocksInRegion(zoneId);

        // 1. Registry → bitfield: every registered block must be INSIDE.
        for (BlockPos pos : registryBlocks) {
            int lx = pos.getX() & 15, lz = pos.getZ() & 15;
            ClassificationState state = data.getBlockState(lx, pos.getY(), lz);
            helper.assertTrue(state == ClassificationState.INSIDE,
                    "[" + label + "] Registry block " + pos + " has state " + state
                            + " (expected INSIDE)");
        }

        // 2. Registry internal: blocks belonging to this zone must map back to it.
        for (BlockPos pos : registryBlocks) {
            int lx = pos.getX() & 15, lz = pos.getZ() & 15;
            if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) {
                UUID mappedId = registry.getRegionIdAt(pos);
                helper.assertTrue(zoneId.equals(mappedId),
                        "[" + label + "] Registry block " + pos + " maps to " + mappedId
                                + " (expected " + zoneId + ")");
            }
        }

        // 3. Metadata ↔ registry: ZoneData volume must match registry block count.
        ZoneData zoneData = zoneReg.getZone(zoneId);
        if (zoneData != null) {
            helper.assertTrue(zoneData.getVolume() == registryBlocks.size(),
                    "[" + label + "] ZoneData volume " + zoneData.getVolume()
                            + " != registry block count " + registryBlocks.size());
        }
    }

    /**
     * Returns how many spatially disconnected groups exist in {@code blocks}.
     */
    private static int countConnectedComponents(Set<BlockPos> blocks) {
        if (blocks.isEmpty()) return 0;
        Set<BlockPos> unvisited = new HashSet<>(blocks);
        int components = 0;
        while (!unvisited.isEmpty()) {
            components++;
            Queue<BlockPos> queue = new ArrayDeque<>();
            BlockPos seed = unvisited.iterator().next();
            unvisited.remove(seed);
            queue.add(seed);
            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                for (BlockPos n : new BlockPos[]{
                        pos.above(), pos.below(),
                        pos.north(), pos.south(),
                        pos.east(), pos.west()}) {
                    if (unvisited.remove(n)) queue.add(n);
                }
            }
        }
        return components;
    }
}
