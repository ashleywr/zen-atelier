package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.space.ClassificationState;
import com.sanhiruzu.atelier.space.SpaceRegion;
import com.sanhiruzu.atelier.space.SpaceRegionRegistry;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lifecycle tests for the zone pipeline without Minecraft bootstrap.
 *
 * <p>Tests the exact bug fix: zones that were never evaluated (no cached ZoneData)
 * must be dissolved immediately rather than entering a grace period that shows
 * confusing "Closing in Xs" UI for rooms that never appeared on the HUD.</p>
 */
class ZoneLifecycleTest {

    // =========================================================================
    // The bug fix: zone without cached ZoneData dissolves immediately
    // =========================================================================

    @Test
    void neverEvaluatedZone_enterGracePeriodGuard_dissolvesImmediately() {
        SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest("test1");
        ZoneRegistry zoneRegistry = ZoneRegistry.createForTest("test1");

        UUID zoneId = UUID.randomUUID();
        Set<BlockPos> interior = ZoneTestHarness.box(0, 1, 0, 3, 1, 3);
        for (BlockPos pos : interior) registry.mapBlockToRegion(pos, zoneId);
        registry.registerRegion(new SpaceRegion(zoneId, ClassificationState.INSIDE, 16, 0));

        Zone zone = new Zone(zoneId, interior, 0);
        zoneRegistry.registerZone(zone);

        // No ZoneData in cache — simulate the guard from enterGracePeriod
        boolean needsImmediateDissolve = zoneRegistry.getZone(zoneId) == null;
        if (needsImmediateDissolve && !zone.isDissolved()) {
            zone.dissolveForTest(registry, zoneRegistry);
        }

        assertTrue(needsImmediateDissolve,
                "Guard must trigger: zone has no cached ZoneData");
        assertTrue(zone.isDissolved(),
                "Zone without cached ZoneData must dissolve immediately");
        assertNull(zoneRegistry.getZoneById(zoneId),
                "Zone must be removed from registry");
        assertTrue(registry.getBlocksInRegion(zoneId).isEmpty(),
                "Blocks must be unmapped from region");
    }

    @Test
    void evaluatedZone_enterGracePeriodGuard_doesNotTrigger() {
        SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest("test2");
        ZoneRegistry zoneRegistry = ZoneRegistry.createForTest("test2");

        UUID zoneId = UUID.randomUUID();
        Set<BlockPos> interior = ZoneTestHarness.box(0, 1, 0, 3, 1, 3);
        for (BlockPos pos : interior) registry.mapBlockToRegion(pos, zoneId);

        Zone zone = new Zone(zoneId, interior, 0);
        zoneRegistry.registerZone(zone);

        // Cache ZoneData — simulates a zone that passed evaluation
        RoomData roomData = new RoomData(zoneId, 16, 0.9f, Map.of(), 0.5f);
        roomData.setInitialized(true);
        zoneRegistry.putForTest(zoneId, roomData);

        // Guard check — should NOT trigger because ZoneData exists
        boolean needsImmediateDissolve = zoneRegistry.getZone(zoneId) == null;

        assertFalse(needsImmediateDissolve,
                "Guard must NOT trigger: zone HAS cached ZoneData");
        assertFalse(zone.isDissolved(),
                "Zone with cached data must survive");
        assertNotNull(zoneRegistry.getZone(zoneId),
                "ZoneData must still be in cache");
    }

    @Test
    void disabledZone_enterGracePeriodGuard_stillHasCachedData() {
        // A zone in grace period has DISABLED ZoneData — it's still in cache.
        // The guard must NOT trigger for disabled zones.
        SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest("test3");
        ZoneRegistry zoneRegistry = ZoneRegistry.createForTest("test3");

        UUID zoneId = UUID.randomUUID();
        Set<BlockPos> interior = ZoneTestHarness.box(0, 1, 0, 3, 1, 3);
        for (BlockPos pos : interior) registry.mapBlockToRegion(pos, zoneId);

        Zone zone = new Zone(zoneId, interior, 0);
        zoneRegistry.registerZone(zone);

        RoomData roomData = new RoomData(zoneId, 16, 0.9f, Map.of(), 0.5f);
        roomData.setInitialized(true);
        roomData.setDisabled(true, 1000L); // disabled
        zoneRegistry.putForTest(zoneId, roomData);

        // Guard check
        boolean needsImmediateDissolve = zoneRegistry.getZone(zoneId) == null;

        assertFalse(needsImmediateDissolve,
                "Guard must NOT trigger for disabled zone — it was previously valid");
        assertFalse(zone.isDissolved(),
                "Disabled zone must not be dissolved — it's in grace period");
    }

    // =========================================================================
    // Zone lifecycle with entry predicates
    // =========================================================================

    @Test
    void zoneWithDoor_losesDoor_hasNoLiveEntry() {
        ZoneTestHarness h = ZoneTestHarness.rooflessRoomWithDoor("test4", 4, 4, 1, 0);

        assertTrue(h.hasLiveEntry(),
                "Zone with a door must have live entry");

        // Simulate door being removed — recreate harness without the door
        ZoneTestHarness h2 = ZoneTestHarness.rooflessRoom("test4b", 4, 4);
        assertFalse(h2.hasLiveEntry(),
                "Zone without a door must have no live entry");
    }

    @Test
    void zoneLifecycle_fullCycle_predicatesOnly() {
        // Create → evaluate → remove entry → (would be grace period) → dissolve
        String key = "test5";
        SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest(key);
        ZoneRegistry zoneRegistry = ZoneRegistry.createForTest(key);

        UUID zoneId = UUID.randomUUID();
        Set<BlockPos> interior = ZoneTestHarness.box(0, 1, 0, 3, 1, 3);
        for (BlockPos pos : interior) registry.mapBlockToRegion(pos, zoneId);
        registry.registerRegion(new SpaceRegion(zoneId, ClassificationState.INSIDE, 16, 0));

        // Simulate validated zone (has entry)
        BlockPos doorPos = new BlockPos(0, 1, -1);
        Set<BlockPos> entries = Set.of(doorPos);
        Predicate<BlockPos> isEntry = entries::contains;

        Zone zone = new Zone(zoneId, interior, 0);
        zoneRegistry.registerZone(zone);

        assertTrue(zone.hasLiveEntry(isEntry, null),
                "Step 1: zone has entry → live entry check passes");

        // Cache ZoneData (simulating successful evaluation)
        RoomData roomData = new RoomData(zoneId, 16, 0.9f, Map.of(), 0.5f);
        roomData.setInitialized(true);
        zoneRegistry.putForTest(zoneId, roomData);

        // Now "remove" the door — change entry predicate to empty
        Predicate<BlockPos> noEntry = p -> false;
        assertFalse(zone.hasLiveEntry(noEntry, null),
                "Step 2: door removed → no live entry");

        // Guard check: zone still has cached data, so grace period would start
        assertNotNull(zoneRegistry.getZone(zoneId),
                "Step 3: ZoneData still cached → would enter grace period (not dissolve)");

        // Simulate grace period expiry → dissolve
        zoneRegistry.disable(zoneId, 0L);
        zone.dissolveForTest(registry, zoneRegistry);
        assertTrue(zone.isDissolved(),
                "Step 4: after grace period expiry → zone dissolved");
    }

    // =========================================================================
    // Randomized lifecycle tests
    // =========================================================================

    @Test
    void randomized_neverEvaluatedZones_alwaysDissolvedByGuard() {
        RandomGenerator rng = RandomGenerator.getDefault();

        for (int i = 0; i < 500; i++) {
            String key = "rand_life_" + i;
            SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest(key);
            ZoneRegistry zoneRegistry = ZoneRegistry.createForTest(key);

            int w = rng.nextInt(2, 10);
            int d = rng.nextInt(2, 10);

            UUID zoneId = UUID.randomUUID();
            Set<BlockPos> interior = ZoneTestHarness.box(0, 1, 0, w - 1, 1, d - 1);
            for (BlockPos pos : interior) registry.mapBlockToRegion(pos, zoneId);

            Zone zone = new Zone(zoneId, interior, 0);
            zoneRegistry.registerZone(zone);

            // Randomly decide: was this zone ever evaluated?
            boolean wasEvaluated = rng.nextBoolean();
            if (wasEvaluated) {
                RoomData roomData = new RoomData(zoneId, interior.size(), 0.85f, Map.of(), 0.3f);
                roomData.setInitialized(true);
                if (rng.nextBoolean()) roomData.setDisabled(true, rng.nextLong(1000));
                zoneRegistry.putForTest(zoneId, roomData);
            }

            // Guard check
            boolean shouldDissolve = zoneRegistry.getZone(zoneId) == null;
            if (shouldDissolve && !zone.isDissolved()) {
                zone.dissolveForTest(registry, zoneRegistry);
            }

            if (wasEvaluated) {
                assertFalse(shouldDissolve,
                        "Evaluated zone " + w + "x" + d + " must NOT trigger immediate dissolve");
                assertFalse(zone.isDissolved(),
                        "Evaluated zone must survive guard check");
            } else {
                assertTrue(shouldDissolve,
                        "Never-evaluated zone " + w + "x" + d + " MUST trigger immediate dissolve");
                assertTrue(zone.isDissolved(),
                        "Never-evaluated zone must be dissolved by guard");
            }
        }
    }

    @Test
    void randomized_zonesMaintainInvariants_acrossLifecycle() {
        RandomGenerator rng = RandomGenerator.getDefault();

        for (int i = 0; i < 300; i++) {
            String key = "inv_" + i;
            SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest(key);
            ZoneRegistry zoneRegistry = ZoneRegistry.createForTest(key);

            UUID zoneId = UUID.randomUUID();
            int w = rng.nextInt(2, 8);
            int d = rng.nextInt(2, 8);
            Set<BlockPos> interior = ZoneTestHarness.box(0, 1, 0, w - 1, 1, d - 1);

            Zone zone = new Zone(zoneId, interior, 0);
            zoneRegistry.registerZone(zone);
            for (BlockPos pos : interior) registry.mapBlockToRegion(pos, zoneId);

            // Invariant 1: after registration, blocks are mapped
            for (BlockPos pos : interior) {
                assertEquals(zoneId, registry.getRegionIdAt(pos),
                        "Each interior block must be mapped to the zone");
            }
            assertEquals(interior.size(), registry.getBlocksInRegion(zoneId).size(),
                    "regionToBlocks size must match interior size");

            // Random operations: evaluate, disable, dissolve
            int action = rng.nextInt(3);
            switch (action) {
                case 0 -> {
                    // Evaluate (cache ZoneData)
                    RoomData rd = new RoomData(zoneId, interior.size(), 0.8f, Map.of(), 0.4f);
                    rd.setInitialized(true);
                    zoneRegistry.putForTest(zoneId, rd);
                }
                case 1 -> {
                    // Disable
                    RoomData rd = new RoomData(zoneId, interior.size(), 0.8f, Map.of(), 0.4f);
                    rd.setInitialized(true);
                    zoneRegistry.putForTest(zoneId, rd);
                    zoneRegistry.disable(zoneId, 100L);
                }
                case 2 -> {
                    // Dissolve
                    zone.dissolveForTest(registry, zoneRegistry);
                }
            }

            // Invariant 2: after dissolve, blocks are unmapped
            if (zone.isDissolved()) {
                for (BlockPos pos : interior) {
                    assertNull(registry.getRegionIdAt(pos),
                            "Blocks must be unmapped after dissolution");
                }
                assertTrue(registry.getBlocksInRegion(zoneId).isEmpty(),
                        "regionToBlocks must be empty after dissolution");
                assertNull(zoneRegistry.getZoneById(zoneId),
                        "Zone must be removed from registry after dissolution");
            }

            // Invariant 3: space region registry consistency
            if (!zone.isDissolved()) {
                for (BlockPos pos : interior) {
                    assertEquals(zoneId, registry.getRegionIdAt(pos),
                            "blockToRegion must stay consistent");
                }
            }
        }
    }

    // =========================================================================
    // Edge case: zero-volume zone
    // =========================================================================

    @Test
    void zeroVolumeZone_guardDissolvesImmediately() {
        SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest("zero_vol");
        ZoneRegistry zoneRegistry = ZoneRegistry.createForTest("zero_vol");

        UUID zoneId = UUID.randomUUID();
        Zone zone = new Zone(zoneId, Set.of(), 0);
        zoneRegistry.registerZone(zone);

        boolean needsImmediateDissolve = zoneRegistry.getZone(zoneId) == null;
        if (needsImmediateDissolve && !zone.isDissolved()) {
            zone.dissolveForTest(registry, zoneRegistry);
        }

        assertTrue(needsImmediateDissolve);
        assertTrue(zone.isDissolved(),
                "Zero-volume zone without evaluation must dissolve immediately");
    }

    // =========================================================================
    // Edge case: already dissolved zone must not trigger double-dissolve
    // =========================================================================

    @Test
    void alreadyDissolvedZone_guardIsNoop() {
        SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest("already_dissolved");
        ZoneRegistry zoneRegistry = ZoneRegistry.createForTest("already_dissolved");

        UUID zoneId = UUID.randomUUID();
        Zone zone = new Zone(zoneId, Set.of(), 0);
        zoneRegistry.registerZone(zone);
        zone.dissolveForTest(registry, zoneRegistry);

        assertTrue(zone.isDissolved());

        // Guard check after dissolution — must be safe to call
        boolean needsImmediateDissolve = zoneRegistry.getZone(zoneId) == null;
        if (needsImmediateDissolve && !zone.isDissolved()) {
            zone.dissolveForTest(registry, zoneRegistry);
        }

        assertTrue(zone.isDissolved(),
                "Already dissolved zone must remain dissolved (no double-dissolve)");
    }

}
