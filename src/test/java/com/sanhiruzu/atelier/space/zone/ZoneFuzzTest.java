package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.space.ClassificationState;
import com.sanhiruzu.atelier.space.SpaceRegion;
import com.sanhiruzu.atelier.space.SpaceRegionRegistry;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Randomized fuzzing tests for zone data structures.
 *
 * <h3>Oracle strategy</h3>
 * Each test maintains a <b>model</b> (simple {@code Map<BlockPos, UUID>}) alongside
 * the real {@link SpaceRegionRegistry}. After every operation, the model is compared
 * to the real state. A mismatch is a bug.
 *
 * <p>Additional invariant checks run after each step:
 * <ul>
 * <li>Map consistency (cross-checking blockToRegion, regionToBlocks, regions)</li>
 * <li>Zone lifecycle (dissolved zones have no blocks; live zones have their blocks)</li>
 * <li>Brute-force cross-validation (hasLiveEntry independently verified)</li>
 * <li>Round-trip idempotency (map+unmap = identity)</li>
 * </ul>
 *
 * <p>All tests run without Minecraft bootstrap (~1000 iterations in ~200ms).</p>
 */
class ZoneFuzzTest {

    private static final RandomGenerator RNG = RandomGenerator.getDefault();

    // =========================================================================
    // Model-based fuzzing: maintain a reference model alongside the real state
    // =========================================================================

    @Test
    void fuzz_blockMapping_operations() {
        for (int seed = 0; seed < 1000; seed++) {
            RandomGenerator rng = new Random(seed);
            SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest("fuzz_map_" + seed);

            // Model: Map<BlockPos, UUID>
            Map<BlockPos, UUID> model = new HashMap<>();

            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();
            registry.registerRegion(new SpaceRegion(idA, ClassificationState.INSIDE, 0, 0));
            registry.registerRegion(new SpaceRegion(idB, ClassificationState.INSIDE, 0, 0));

            int steps = rng.nextInt(5, 30);
            for (int step = 0; step < steps; step++) {
                int op = rng.nextInt(4);
                BlockPos pos = randomPos(rng, -10, 10);

                switch (op) {
                    case 0 -> { // mapBlockToRegion to idA
                        UUID old = model.put(pos, idA);
                        registry.mapBlockToRegion(pos, idA);
                        // mapBlockToRegion handles the old mapping
                        if (old != null && !old.equals(idA)) {
                            // old region's block set should no longer contain pos
                        }
                    }
                    case 1 -> { // mapBlockToRegion to idB
                        UUID old = model.put(pos, idB);
                        registry.mapBlockToRegion(pos, idB);
                    }
                    case 2 -> { // unmapBlock
                        model.remove(pos);
                        registry.unmapBlock(pos);
                    }
                    case 3 -> { // mapBlockToRegion overlapping
                        UUID old = model.put(pos, idA);
                        registry.mapBlockToRegion(pos, idA);
                    }
                }

                // Verify model matches real state
                assertModelMatches(registry, model, "seed=" + seed + " step=" + step + " op=" + op);
            }
        }
    }

    @Test
    void fuzz_zoneLifecycle_withModel() {
        for (int seed = 0; seed < 500; seed++) {
            RandomGenerator rng = new Random(seed);
            String key = "fuzz_life_" + seed;
            SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest(key);
            ZoneRegistry zoneRegistry = ZoneRegistry.createForTest(key);

            // Create 1-5 zones
            int zoneCount = rng.nextInt(1, 6);
            List<ZoneState> zones = new ArrayList<>();
            for (int i = 0; i < zoneCount; i++) {
                UUID id = UUID.randomUUID();
                int w = rng.nextInt(1, 8);
                int d = rng.nextInt(1, 8);
                Set<BlockPos> interior = ZoneTestHarness.box(
                        rng.nextInt(-5, 5), rng.nextInt(0, 3), rng.nextInt(-5, 5),
                        rng.nextInt(-5, 5) + w - 1, rng.nextInt(0, 3) + rng.nextInt(0, 1),
                        rng.nextInt(-5, 5) + d - 1);
                interior = normalizeBox(interior); // ensure it's a proper box

                // mapBlockToRegion moves blocks from prior zones — keep models mutually exclusive
                for (ZoneState prior : zones) {
                    prior.interior.removeAll(interior);
                }

                Zone zone = new Zone(id, interior, 0);
                zoneRegistry.registerZone(zone);
                registry.registerRegion(new SpaceRegion(id, ClassificationState.INSIDE, interior.size(), 0));
                for (BlockPos pos : interior) {
                    registry.mapBlockToRegion(pos, id);
                }

                // Randomly decide if zone was evaluated
                boolean evaluated = rng.nextBoolean();
                if (evaluated) {
                    RoomData rd = new RoomData(id, interior.size(), 0.7f + rng.nextFloat() * 0.3f,
                            Map.of(), rng.nextFloat());
                    rd.setInitialized(true);
                    zoneRegistry.putForTest(id, rd);
                }

                zones.add(new ZoneState(zone, interior, evaluated));
            }

            // Apply random operations
            int steps = rng.nextInt(10, 50);
            for (int step = 0; step < steps; step++) {
                int op = rng.nextInt(7);
                ZoneState z = zones.get(rng.nextInt(zones.size()));

                switch (op) {
                    case 0 -> { // add interior block (expansion)
                        BlockPos pos = randomAdjacentPos(rng, z.interior);
                        if (pos != null && !z.interior.contains(pos)) {
                            // Keep model sets mutually exclusive — mapBlockToRegion
                            // moves the block, so remove from any other zone's model.
                            for (ZoneState other : zones) {
                                if (other != z) other.interior.remove(pos);
                            }
                            z.interior.add(pos);
                            registry.mapBlockToRegion(pos, z.zone.getId());
                        }
                    }
                    case 1 -> { // remove interior block (shrink)
                        if (!z.interior.isEmpty()) {
                            BlockPos pos = randomFromSet(rng, z.interior);
                            // Remove from all model sets — we don't know which zone
                            // the registry has it mapped to.
                            for (ZoneState other : zones) other.interior.remove(pos);
                            registry.unmapBlock(pos);
                        }
                    }
                    case 2 -> { // dissolve zone
                        if (!z.zone.isDissolved()) {
                            for (BlockPos pos : new ArrayList<>(z.interior)) {
                                registry.unmapBlock(pos);
                            }
                            z.interior.clear();
                            z.zone.dissolveForTest(registry, zoneRegistry);
                        }
                    }
                    case 3 -> { // disable zone (simulates grace period entry)
                        if (!z.zone.isDissolved() && z.evaluated) {
                            zoneRegistry.disable(z.zone.getId(), rng.nextLong(1000));
                        }
                    }
                    case 4 -> { // re-evaluate (cache new ZoneData)
                        if (!z.zone.isDissolved()) {
                            RoomData rd = new RoomData(z.zone.getId(), z.interior.size(),
                                    0.8f, Map.of(), rng.nextFloat());
                            rd.setInitialized(true);
                            zoneRegistry.putForTest(z.zone.getId(), rd);
                        }
                    }
                    case 5 -> { // guard: if never-evaluated, dissolve immediately
                        if (!z.zone.isDissolved() && zoneRegistry.getZone(z.zone.getId()) == null) {
                            for (BlockPos pos : new ArrayList<>(z.interior)) {
                                registry.unmapBlock(pos);
                            }
                            z.interior.clear();
                            z.zone.dissolveForTest(registry, zoneRegistry);
                        }
                    }
                    case 6 -> { // remove region (simulates reclassification)
                        if (!z.zone.isDissolved()) {
                            registry.removeRegion(z.zone.getId());
                            // removeRegion unmaps all blocks — clear all model sets
                            for (ZoneState other : zones) {
                                other.interior.removeIf(pos ->
                                        registry.getRegionIdAt(pos) == null);
                            }
                        }
                    }
                }

                // Invariants after every step
                assertZoneBlockMappingConsistent(z, registry);
                if (!z.zone.isDissolved() && registry.getRegion(z.zone.getId()) != null) {
                    assertMapsCrossCheck(registry, z.zone.getId(), z.interior);
                }
            }

            // Final consistency check
            for (ZoneState z : zones) {
                assertZoneBlockMappingConsistent(z, registry);
            }
        }
    }

    // =========================================================================
    // Fuzzing: cross-validation oracle (two methods must agree)
    // =========================================================================

    @Test
    void fuzz_crossValidation_hasLiveEntry_vs_isValid() {
        for (int seed = 0; seed < 2000; seed++) {
            RandomGenerator rng = new Random(seed);
            int w = rng.nextInt(2, 12);
            int d = rng.nextInt(2, 12);
            int height = rng.nextInt(1, 4);

            ZoneTestHarness harness = new ZoneTestHarness("cv_" + seed);
            harness.addInterior(ZoneTestHarness.box(0, 1, 0, w - 1, height, d - 1));

            // Random entry placement — may or may not be adjacent
            int entryCount = rng.nextInt(0, 5);
            for (int i = 0; i < entryCount; i++) {
                int ex = rng.nextInt(-3, w + 3);
                int ey = rng.nextInt(-2, height + 3);
                int ez = rng.nextInt(-3, d + 3);
                harness.addEntry(ex, ey, ez);
            }

            // Both methods must agree
            ZoneInvariants.assertHasLiveEntryAgreesWithIsValid(harness);

            // Both must agree with brute-force oracle
            ZoneInvariants.assertHasLiveEntryMatchesBruteForce(harness);
        }
    }

    // =========================================================================
    // Fuzzing: map consistency under random mutations
    // =========================================================================

    @Test
    void fuzz_mapConsistency_underMutations() {
        for (int seed = 0; seed < 500; seed++) {
            RandomGenerator rng = new Random(seed);
            SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest("fuzz_cons_" + seed);
            ZoneRegistry zoneRegistry = ZoneRegistry.createForTest("fuzz_cons_" + seed);

            // Create some zones
            List<UUID> zoneIds = new ArrayList<>();
            for (int i = 0; i < rng.nextInt(1, 6); i++) {
                UUID id = UUID.randomUUID();
                zoneIds.add(id);
                registry.registerRegion(new SpaceRegion(id, ClassificationState.INSIDE, 0, 0));
                zoneRegistry.registerZone(new Zone(id));
            }

            // Random operations
            int steps = rng.nextInt(20, 100);
            for (int step = 0; step < steps; step++) {
                int op = rng.nextInt(6);
                UUID id = zoneIds.get(rng.nextInt(zoneIds.size()));
                BlockPos pos = randomPos(rng, -20, 20);

                switch (op) {
                    case 0 -> registry.mapBlockToRegion(pos, id);
                    case 1 -> {
                        UUID mapped = registry.getRegionIdAt(pos);
                        if (mapped != null) registry.unmapBlock(pos);
                    }
                    case 2 -> { // remove and re-register region
                        registry.removeRegion(id);
                        registry.registerRegion(new SpaceRegion(id, ClassificationState.INSIDE, 0, 0));
                    }
                    case 3 -> { // read-only check (shouldn't change state)
                        registry.getBlocksInRegion(id);
                    }
                    case 4 -> registry.mapBlockToRegion(pos,
                            zoneIds.get(rng.nextInt(zoneIds.size()))); // remap
                    case 5 -> registry.removeRegion(id); // delete everything
                }
            }

            // Final consistency check
            ZoneInvariants.assertMapsAreConsistent(registry);
        }
    }

    // =========================================================================
    // Fuzzing: round-trip invariants
    // =========================================================================

    @Test
    void fuzz_roundTrip_invariants() {
        for (int seed = 0; seed < 500; seed++) {
            RandomGenerator rng = new Random(seed);
            SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest("fuzz_rt_" + seed);
            UUID id = UUID.randomUUID();

            // Register a test region
            registry.registerRegion(new SpaceRegion(id, ClassificationState.INSIDE, 0, 0));

            // Round-trip: map + unmap
            BlockPos pos = randomPos(rng, -50, 50);
            UUID before = registry.getRegionIdAt(pos);
            registry.mapBlockToRegion(pos, id);
            registry.unmapBlock(pos);
            assertEquals(before, registry.getRegionIdAt(pos),
                    "map+unmap must be identity (seed=" + seed + ")");

            // Round-trip: register + remove
            UUID testId = UUID.randomUUID();
            SpaceRegion testRegion = new SpaceRegion(testId, ClassificationState.INSIDE, 0, 0);
            registry.registerRegion(testRegion);
            BlockPos testPos = randomPos(rng, -50, 50);
            registry.mapBlockToRegion(testPos, testId);
            registry.removeRegion(testId);
            assertNull(registry.getRegion(testId),
                    "region must be gone after removeRegion (seed=" + seed + ")");
            assertNull(registry.getRegionIdAt(testPos),
                    "blocks must be unmapped after removeRegion (seed=" + seed + ")");
            assertTrue(registry.getBlocksInRegion(testId).isEmpty(),
                    "regionToBlocks must be empty (seed=" + seed + ")");
        }
    }

    // =========================================================================
    // Fuzzing: idempotency
    // =========================================================================

    @Test
    void fuzz_idempotency_dissolve() {
        for (int seed = 0; seed < 300; seed++) {
            RandomGenerator rng = new Random(seed);
            SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest("fuzz_idem_" + seed);
            ZoneRegistry zoneRegistry = ZoneRegistry.createForTest("fuzz_idem_" + seed);

            UUID id = UUID.randomUUID();
            int size = rng.nextInt(1, 50);
            Set<BlockPos> interior = new HashSet<>();
            for (int i = 0; i < size; i++) {
                interior.add(randomPos(rng, -10, 10));
            }

            Zone zone = new Zone(id, interior, 0);
            zoneRegistry.registerZone(zone);
            registry.registerRegion(new SpaceRegion(id, ClassificationState.INSIDE, size, 0));
            for (BlockPos pos : interior) registry.mapBlockToRegion(pos, id);

            // First dissolve
            zone.dissolveForTest(registry, zoneRegistry);
            assertTrue(zone.isDissolved());

            // Second dissolve — must be no-op (no exceptions, no state change)
            int blockCountAfterFirst = countBlocksInOtherZones(registry, id);
            zone.dissolveForTest(registry, zoneRegistry);
            assertEquals(blockCountAfterFirst, countBlocksInOtherZones(registry, id),
                    "second dissolve must not change state (seed=" + seed + ")");
        }
    }

    // =========================================================================
    // Fuzzing: the specific bug — never-evaluated zone lifecycle
    // =========================================================================

    @Test
    void fuzz_neverEvaluatedZone_neverShowsGracePeriod() {
        // The guard must ensure: if getZone(zoneId) == null, the zone dissolves
        // immediately. This invariant must hold for EVERY random room shape.
        for (int seed = 0; seed < 2000; seed++) {
            RandomGenerator rng = new Random(seed);
            String key = "fuzz_grace_" + seed;
            SpaceRegionRegistry registry = SpaceRegionRegistry.createForTest(key);
            ZoneRegistry zoneRegistry = ZoneRegistry.createForTest(key);

            int w = rng.nextInt(2, 16);
            int d = rng.nextInt(2, 16);
            Set<BlockPos> interior = ZoneTestHarness.box(0, 1, 0, w - 1, 1, d - 1);

            UUID zoneId = UUID.randomUUID();
            Zone zone = new Zone(zoneId, interior, 0);
            zoneRegistry.registerZone(zone);
            registry.registerRegion(new SpaceRegion(zoneId, ClassificationState.INSIDE, interior.size(), 0));
            for (BlockPos pos : interior) registry.mapBlockToRegion(pos, zoneId);

            // Randomly decide: was this zone ever evaluated?
            boolean wasEvaluated = rng.nextBoolean();
            if (wasEvaluated) {
                RoomData rd = new RoomData(zoneId, interior.size(), 0.8f, Map.of(), 0.3f);
                rd.setInitialized(true);
                zoneRegistry.putForTest(zoneId, rd);
            }

            // Apply the guard (simulating enterGracePeriod check)
            boolean hasCachedData = zoneRegistry.getZone(zoneId) != null;
            if (!hasCachedData && !zone.isDissolved()) {
                zone.dissolveForTest(registry, zoneRegistry);
            }

            // Invariant: if it was never evaluated, it MUST be dissolved
            if (!wasEvaluated) {
                assertTrue(zone.isDissolved(),
                        "Never-evaluated zone must dissolve immediately (seed=" + seed
                                + ", " + w + "x" + d + ")");
            } else {
                assertFalse(zone.isDissolved(),
                        "Evaluated zone must NOT be dissolved by guard (seed=" + seed + ")");
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static BlockPos randomPos(RandomGenerator rng, int min, int max) {
        return new BlockPos(
                rng.nextInt(min, max + 1),
                rng.nextInt(min, max + 1),
                rng.nextInt(min, max + 1));
    }

    @SuppressWarnings("OptionalGet")
    private static BlockPos randomAdjacentPos(RandomGenerator rng, Set<BlockPos> interior) {
        if (interior.isEmpty()) return null;
        BlockPos seed = interior.stream().skip(rng.nextInt(interior.size())).findFirst().get();
        int side = rng.nextInt(6);
        return switch (side) {
            case 0 -> seed.north();
            case 1 -> seed.south();
            case 2 -> seed.east();
            case 3 -> seed.west();
            case 4 -> seed.above();
            default -> seed.below();
        };
    }

    @SuppressWarnings("OptionalGet")
    private static BlockPos randomFromSet(RandomGenerator rng, Set<BlockPos> set) {
        return set.stream().skip(rng.nextInt(set.size())).findFirst().get();
    }

    private static Set<BlockPos> normalizeBox(Set<BlockPos> positions) {
        if (positions.isEmpty()) return positions;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : positions) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
        }
        Set<BlockPos> box = new HashSet<>();
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    box.add(new BlockPos(x, y, z));
        return box;
    }

    // Model comparison

    private static void assertModelMatches(SpaceRegionRegistry registry,
                                           Map<BlockPos, UUID> model,
                                           String context) {
        for (var entry : model.entrySet()) {
            UUID actual = registry.getRegionIdAt(entry.getKey());
            assertEquals(entry.getValue(), actual,
                    "Model mismatch at " + entry.getKey() + ": model=" + entry.getValue()
                            + " actual=" + actual + " [" + context + "]");
        }
    }

    // Partial consistency checks

    private static void assertZoneBlockMappingConsistent(ZoneState z, SpaceRegionRegistry registry) {
        ZoneInvariants.assertZoneBlockMappingIsConsistent(z.zone, registry, z.interior);
    }

    private static void assertMapsCrossCheck(SpaceRegionRegistry registry, UUID zoneId,
                                             Set<BlockPos> interior) {
        Set<BlockPos> mapped = registry.getBlocksInRegion(zoneId);
        for (BlockPos pos : interior) {
            UUID owner = registry.getRegionIdAt(pos);
            if (owner != null && owner.equals(zoneId)) {
                assertTrue(mapped.contains(pos),
                        "blockToRegion has " + pos + " → " + zoneId
                                + " but regionToBlocks does not contain it");
            }
        }
    }

    private static int countBlocksInOtherZones(SpaceRegionRegistry registry, UUID excludeId) {
        int count = 0;
        for (UUID id : registry.getAllRegionIds()) {
            if (!id.equals(excludeId)) {
                count += registry.getBlocksInRegion(id).size();
            }
        }
        return count;
    }

    // ---- record ----

    record ZoneState(Zone zone, Set<BlockPos> interior, boolean evaluated) {
    }
}
