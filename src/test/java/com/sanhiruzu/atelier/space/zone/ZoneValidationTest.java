package com.sanhiruzu.atelier.space.zone;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for zone validation methods ({@code isValidPlayerBuiltZone}, {@code hasLiveEntry}).
 * Uses {@link ZoneTestHarness} with predicate-based block queries — no Minecraft bootstrap.
 */
class ZoneValidationTest {

    // =========================================================================
    // The exact bug: roofless room with no door
    // =========================================================================

    @Test
    void roofless4x4x1_noDoor_failsValidation() {
        ZoneTestHarness h = ZoneTestHarness.rooflessRoom("test", 4, 4);
        // No entry added — this is the bug scenario.

        assertFalse(h.isValidPlayerBuiltZone(),
                "4x4x1 roofless room with no door must fail validation — it has no entry point");
    }

    @Test
    void roofless4x4x1_withDoor_passesValidation() {
        ZoneTestHarness h = ZoneTestHarness.rooflessRoom("test", 4, 4);
        h.addEntry(1, 1, 0); // door at one wall

        assertTrue(h.isValidPlayerBuiltZone(),
                "4x4x1 roofless room with a door must pass validation");
    }

    // =========================================================================
    // Minimum size
    // =========================================================================

    @Test
    void singleBlock_failsValidation() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(new BlockPos(0, 1, 0));
        h.addEntry(1, 1, 0);

        assertFalse(h.isValidPlayerBuiltZone(),
                "Single-block interior must fail validation (minimum is 2)");
    }

    @Test
    void twoBlocks1x2_withDoor_passesValidation() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 1, 1, 0));
        h.addEntry(0, 1, -1);

        assertTrue(h.isValidPlayerBuiltZone(),
                "1x2 interior with door must pass validation");
    }

    // =========================================================================
    // Entry adjacency — entry must touch an interior block
    // =========================================================================

    @Test
    void entryMustBeAdjacent_notDiagonal() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        // 2x2x1 interior
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 1, 1, 1));
        // Door at (2, 1, 2) is diagonal from (1,1,1) — not orthogonal
        h.addEntry(2, 1, 2);

        assertFalse(h.isValidPlayerBuiltZone(),
                "Diagonal entry must not count — only orthogonal adjacency is valid");
    }

    @Test
    void entryMustBeAdjacent_orthogonalPasses() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 1, 1, 1));
        h.addEntry(2, 1, 0); // adjacent to (1,1,0)

        assertTrue(h.isValidPlayerBuiltZone(),
                "Orthogonal entry adjacent to interior must pass");
    }

    @Test
    void entryMustBeAdjacent_notTwoBlocksAway() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 1, 1, 1));
        h.addEntry(3, 1, 0); // two blocks away from nearest interior

        assertFalse(h.isValidPlayerBuiltZone(),
                "Entry two blocks from interior must not count");
    }

    // =========================================================================
    // Entry from above/below (trapdoor in ceiling/floor)
    // =========================================================================

    @Test
    void entryAbove_passesValidation() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 1, 1, 1));
        h.addEntry(0, 2, 0); // trapdoor in ceiling, above interior block

        assertTrue(h.isValidPlayerBuiltZone(),
                "Entry above interior block (ceiling trapdoor) must pass");
    }

    @Test
    void entryBelow_passesValidation() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 1, 1, 1));
        h.addEntry(0, 0, 0); // trapdoor in floor

        assertTrue(h.isValidPlayerBuiltZone(),
                "Entry below interior block (floor trapdoor) must pass");
    }

    // =========================================================================
    // Multiple doors
    // =========================================================================

    @Test
    void multipleDoors_onlyOneNeeded() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 3, 1, 3));
        h.addEntry(0, 1, -1);
        h.addEntry(3, 1, 4);
        h.addEntry(1, 2, 0);

        assertTrue(h.isValidPlayerBuiltZone(),
                "Multiple doors — only one entry needed for validation");
    }

    // =========================================================================
    // hasLiveEntry with excluded position
    // =========================================================================

    @Test
    void hasLiveEntry_excludedPosition_ignored() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 1, 1, 1));
        BlockPos onlyDoor = new BlockPos(0, 1, -1);
        h.addEntry(onlyDoor);

        assertTrue(h.hasLiveEntry(),
                "hasLiveEntry must return true when door is present");
        assertFalse(h.hasLiveEntry(onlyDoor),
                "hasLiveEntry must return false when the only door is excluded");
    }

    @Test
    void hasLiveEntry_twoDoors_excludeOne_stillHasEntry() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 1, 1, 1));
        h.addEntry(0, 1, -1);
        h.addEntry(1, 1, 2);

        assertTrue(h.hasLiveEntry(new BlockPos(0, 1, -1)),
                "hasLiveEntry must return true when excluding one of two doors");
    }

    @Test
    void ownedEntryBlock_countsAsLiveEntry() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        BlockPos stairLanding = new BlockPos(0, 1, 0);
        h.addInterior(stairLanding);
        h.addInterior(new BlockPos(1, 1, 0));
        h.addEntry(stairLanding);

        assertTrue(h.hasLiveEntry(),
                "An entry block owned by the zone should count as the room entry");
        assertTrue(h.isValidPlayerBuiltZone(),
                "Player-built validation should accept owned stairs/slabs/trapdoors/doors as entries");
        assertFalse(h.hasLiveEntry(stairLanding),
                "Excluding the owned entry should remove the only live entry");
    }

    @Test
    void ownedEntryBlocks_areSeparateFromInteriorAir() {
        UUID id = UUID.randomUUID();
        Set<BlockPos> interior = Set.of(new BlockPos(0, 1, 0), new BlockPos(1, 1, 0));
        BlockPos door = new BlockPos(0, 1, -1);
        Zone zone = new Zone(id, interior, 0);
        zone.addOwnedEntryBlocks(Set.of(door));

        assertEquals(interior, zone.getInteriorBlocks(),
                "Interior blocks should remain the floodfilled air volume");
        assertTrue(zone.getOwnedEntryBlocks().contains(door),
                "Entry block should be owned separately by the zone");
        assertTrue(zone.getAllOwnedBlocks().contains(door),
                "All owned blocks should include entry blocks for evaluation/debug ownership");
    }

    // =========================================================================
    // Entry does NOT touch interior — validate both methods agree
    // =========================================================================

    @Test
    void noEntry_validAndLiveEntry_agree() {
        for (int w = 2; w <= 8; w++) {
            for (int d = 2; d <= 8; d++) {
                ZoneTestHarness h = ZoneTestHarness.rooflessRoom("agree" + w + "x" + d, w, d);
                assertEquals(h.hasLiveEntry(), h.isValidPlayerBuiltZone(),
                        "hasLiveEntry and isValidPlayerBuiltZone must agree for " + w + "x" + d
                                + " roofless room with no door");
            }
        }
    }

    // =========================================================================
    // Randomized property tests
    // =========================================================================

    @ParameterizedTest
    @ValueSource(ints = {100, 500, 1000})
    void randomizedRooms_entryPresence_determinesValidity(int iterations) {
        RandomGenerator rng = RandomGenerator.getDefault();
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < iterations; i++) {
            int w = rng.nextInt(2, 16);  // width 2-15
            int d = rng.nextInt(2, 16);  // depth 2-15
            boolean hasDoor = rng.nextBoolean();

            String key = "rand_" + i;
            ZoneTestHarness h;
            if (hasDoor) {
                int doorX = rng.nextInt(w);
                int doorZ = rng.nextInt(d);
                // Place door on one of the 4 walls
                int side = rng.nextInt(4);
                int dx = 0, dz = 0;
                switch (side) {
                    case 0 -> dz = -1;          // north wall
                    case 1 -> dz = d;           // south wall
                    case 2 -> dx = -1;          // west wall
                    case 3 -> dx = w;           // east wall
                }
                h = ZoneTestHarness.rooflessRoomWithDoor(key, w, d, dx, dz);
            } else {
                h = ZoneTestHarness.rooflessRoom(key, w, d);
            }

            boolean valid = h.isValidPlayerBuiltZone();
            String scenario = w + "x" + d + (hasDoor ? " with door" : " no door");

            if (hasDoor) {
                assertTrue(valid, "Room " + scenario + " must be valid");
            } else {
                assertFalse(valid, "Room " + scenario + " must NOT be valid (no entry)");
            }
        }
    }

    @Test
    void randomizedRooms_entryMustBeAdjacent_invariant() {
        RandomGenerator rng = RandomGenerator.getDefault();

        for (int i = 0; i < 500; i++) {
            int w = rng.nextInt(2, 10);
            int d = rng.nextInt(2, 10);

            ZoneTestHarness h = new ZoneTestHarness("adj_" + i);
            h.addInterior(ZoneTestHarness.box(0, 1, 0, w - 1, 1, d - 1));

            // Place a door at a position near the room, maybe adjacent, maybe not
            int doorX = rng.nextInt(-2, w + 2);
            int doorY = rng.nextInt(0, 3);
            int doorZ = rng.nextInt(-2, d + 2);
            h.addEntry(doorX, doorY, doorZ);

            // Check if door is orthogonally adjacent to any interior block
            Set<BlockPos> interior = h.interior();
            BlockPos door = new BlockPos(doorX, doorY, doorZ);
            boolean isAdjacent = interior.stream().anyMatch(ip ->
                    Math.abs(ip.getX() - door.getX()) + Math.abs(ip.getY() - door.getY())
                            + Math.abs(ip.getZ() - door.getZ()) == 1);

            boolean valid = h.isValidPlayerBuiltZone();
            assertEquals(isAdjacent, valid,
                    "Validation must match adjacency: door at " + door + " is"
                            + (isAdjacent ? "" : " NOT") + " adjacent to " + w + "x" + d + " interior"
                            + " but validation returned " + valid);
        }
    }

    // =========================================================================
    // Entry block on DIFFERENT Y level from interior
    // =========================================================================

    @Test
    void multiYInterior_doorAtMidLevel_passes() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        // 2x3x2 interior (2 tall)
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 1, 2, 1));
        h.addEntry(-1, 1, 0); // door on west wall at mid-level

        assertTrue(h.isValidPlayerBuiltZone(),
                "Door at mid-Y of 2-tall room must pass validation");
    }

    @Test
    void multiYInterior_doorAtUpperLevel_passes() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 1, 2, 1));
        h.addEntry(-1, 2, 0); // door on west wall at upper level

        assertTrue(h.isValidPlayerBuiltZone(),
                "Door at upper Y of 2-tall room must pass validation");
    }

    // =========================================================================
    // Irregular rooms (L-shape)
    // =========================================================================

    @Test
    void lShapedRoom_withDoor_passes() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        // L-shape at y=1:
        //  (0,1,0) (1,1,0) (2,1,0)
        //  (0,1,1) (1,1,1)
        //  (0,1,2)
        h.addInterior(Set.of(
                new BlockPos(0, 1, 0), new BlockPos(1, 1, 0), new BlockPos(2, 1, 0),
                new BlockPos(0, 1, 1), new BlockPos(1, 1, 1),
                new BlockPos(0, 1, 2)
        ));
        h.addEntry(0, 1, -1); // entrance adjacent to (0,1,0)

        assertTrue(h.isValidPlayerBuiltZone(),
                "L-shaped room with entry must pass validation");
    }

    @Test
    void lShapedRoom_noDoor_fails() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(Set.of(
                new BlockPos(0, 1, 0), new BlockPos(1, 1, 0), new BlockPos(2, 1, 0),
                new BlockPos(0, 1, 1), new BlockPos(1, 1, 1),
                new BlockPos(0, 1, 2)
        ));

        assertFalse(h.isValidPlayerBuiltZone(),
                "L-shaped room without entry must fail validation");
    }

    // =========================================================================
    // computeSpatialExtent
    // =========================================================================

    @Test
    void spatialExtent_includesEntryBlocks() {
        ZoneTestHarness h = new ZoneTestHarness("test");
        h.addInterior(ZoneTestHarness.box(0, 1, 0, 3, 1, 3));
        h.addEntry(0, 1, -1);   // door at z=-1
        h.addEntry(4, 1, 0);    // door at x=4

        ZoneData data = new RoomData(h.regionId(), 16, 0.9f, Map.of(), 0.5f);
        h.computeSpatialExtent(data);

        assertTrue(data.hasSpatialExtent());
        assertEquals(-1, data.getMinZ(), "extent must include door at z=-1");
        assertEquals(4, data.getMaxX(), "extent must include door at x=4");
        assertEquals(0, data.getMinX(), "extent minX is interior corner");
        assertEquals(3, data.getMaxZ(), "extent maxZ is interior corner");
    }

    @Test
    void openingArea_countsVerticalAirGapWithoutConnector() {
        Set<BlockPos> interior = Set.of(new BlockPos(0, 1, 0));
        Set<BlockPos> air = Set.of(new BlockPos(0, 2, 0));

        int openingArea = Zone.computeOpeningAreaFor(interior, air::contains, p -> false);

        assertEquals(1, openingArea,
                "vertical ceiling gap without stairs/ladder/trapdoor/slab must count as exposure");
    }

    @Test
    void openingArea_ignoresVerticalAirGapNearConnector() {
        Set<BlockPos> interior = Set.of(new BlockPos(0, 1, 0));
        Set<BlockPos> air = Set.of(new BlockPos(0, 2, 0));
        Set<BlockPos> connectors = Set.of(new BlockPos(0, 0, 0));

        int openingArea = Zone.computeOpeningAreaFor(interior, air::contains, connectors::contains);

        assertEquals(0, openingArea,
                "vertical headroom gap near stairs/ladder/trapdoor/slab must be treated as an internal connector");
    }

    @Test
    void rawOpeningAreaCountsConnectorGapForStrictSealedChecks() {
        Set<BlockPos> interior = Set.of(new BlockPos(0, 1, 0));
        Set<BlockPos> air = Set.of(new BlockPos(0, 2, 0));

        int openingArea = Zone.computeRawOpeningAreaFor(interior, air::contains);

        assertEquals(1, openingArea,
                "strict sealed checks need raw openings even when normal room enclosure ignores connector gaps");
    }

    @Test
    void openingArea_stillCountsHorizontalAirNearConnector() {
        Set<BlockPos> interior = Set.of(new BlockPos(0, 1, 0));
        Set<BlockPos> air = Set.of(new BlockPos(1, 1, 0));
        Set<BlockPos> connectors = Set.of(new BlockPos(0, 0, 0));

        int openingArea = Zone.computeOpeningAreaFor(interior, air::contains, connectors::contains);

        assertEquals(1, openingArea,
                "connector exemption is vertical-only so wall holes still count as exposure");
    }
}
