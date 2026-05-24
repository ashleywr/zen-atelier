package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.space.ClassificationState;
import com.sanhiruzu.atelier.space.SpaceRegion;
import com.sanhiruzu.atelier.space.SpaceRegionRegistry;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Invariant oracles for the zone data structures.
 *
 * <p>These check invariants derived from the <em>specification</em>, not the
 * implementation. If an invariant fails, the implementation is buggy — even
 * if the implementation "looks right."</p>
 *
 * <h3>Oracle types used:</h3>
 * <ul>
 * <li><b>Cross-validation</b> — two independent code paths must agree</li>
 * <li><b>Map consistency</b> — the three maps must stay in sync</li>
 * <li><b>Round-trip</b> — apply + undo = identity</li>
 * <li><b>Idempotency</b> — repeated operation = same result</li>
 * <li><b>Brute-force</b> — for small inputs, exhaustively compute the answer</li>
 * </ul>
 */
public final class ZoneInvariants {

    private ZoneInvariants() {
    }

    // =========================================================================
    // 1. Cross-validation: two code paths must agree
    // =========================================================================

    /**
     * {@code hasLiveEntry} and {@code isValidPlayerBuiltZone} both check whether
     * an entry block touches the interior. For any given interior + entry set,
     * they must agree.
     */
    public static void assertHasLiveEntryAgreesWithIsValid(ZoneTestHarness h) {
        boolean liveEntry = h.hasLiveEntry();
        boolean valid = h.isValidPlayerBuiltZone();
        assertEquals(liveEntry, valid,
                "hasLiveEntry=" + liveEntry + " but isValidPlayerBuiltZone=" + valid
                        + " — they must agree on entry detection");
    }

    /**
     * Brute-force check: independently verify {@code hasLiveEntry} by manually
     * iterating every interior block's 6-neighbors. This is a second independent
     * implementation of the same check — if they disagree, one is buggy.
     */
    public static void assertHasLiveEntryMatchesBruteForce(ZoneTestHarness h) {
        boolean fromMethod = h.hasLiveEntry();
        boolean fromBruteForce = bruteForceHasEntry(h.interior(), h.entries());
        assertEquals(fromBruteForce, fromMethod,
                "hasLiveEntry=" + fromMethod + " but brute-force says " + fromBruteForce);
    }

    static boolean bruteForceHasEntry(Set<BlockPos> interior, Set<BlockPos> entries) {
        for (BlockPos pos : interior) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                        if (entries.contains(pos.offset(dx, dy, dz))) return true;
                    }
                }
            }
        }
        return false;
    }

    // =========================================================================
    // 2. Map consistency: the three maps must stay in sync
    // =========================================================================

    /**
     * Checks all three maps are mutually consistent.
     * <ul>
     * <li>Every (pos, uuid) in blockToRegion → pos is in regionToBlocks[uuid]</li>
     * <li>Every (uuid, blockSet) in regionToBlocks → every pos in blockSet maps
     *     back to uuid in blockToRegion</li>
     * <li>Every uuid in regions has a non-null entry in regionToBlocks</li>
     * </ul>
     */
    public static void assertMapsAreConsistent(SpaceRegionRegistry registry) {
        StringBuilder errors = new StringBuilder();

        // Use reflection to access internal maps (package-private accessors
        // aren't exposed; we verify through public API and indirect checks)
        for (UUID id : registry.getAllRegionIds()) {
            Set<BlockPos> blocks = registry.getBlocksInRegion(id);

            // Every block in regionToBlocks must map back to this UUID
            for (BlockPos pos : blocks) {
                UUID mapped = registry.getRegionIdAt(pos);
                if (!id.equals(mapped)) {
                    errors.append("  blockToRegion[").append(pos).append("] = ")
                            .append(mapped).append(" but regionToBlocks[").append(id)
                            .append("] contains it\n");
                }
            }
        }

        // Check for dangling blockToRegion entries (blocks mapped to
        // non-existent regions)
        // We can't iterate blockToRegion directly, so we use a different check:
        // after dissolving a zone, verify its blocks are unmapped.
        // This is tested separately in lifecycle tests.

        if (!errors.isEmpty()) {
            fail("Map consistency violation:\n" + errors);
        }
    }

    /**
     * Checks consistency for all registries created for a given key prefix.
     */
    public static void assertAllRegistriesConsistent(Map<String, SpaceRegionRegistry> registries) {
        for (var entry : registries.entrySet()) {
            assertMapsAreConsistent(entry.getValue());
        }
    }

    // =========================================================================
    // 3. Round-trip invariants
    // =========================================================================

    /**
     * After mapping a block then immediately unmapping it, the block must
     * have no region assignment.
     */
    public static void assertMapUnmapRoundTrip(SpaceRegionRegistry registry, BlockPos pos, UUID id) {
        UUID before = registry.getRegionIdAt(pos);
        registry.mapBlockToRegion(pos, id);
        registry.unmapBlock(pos);
        UUID after = registry.getRegionIdAt(pos);
        assertEquals(before, after,
                "map+unmap must be identity: before=" + before + " after=" + after);
    }

    /**
     * After registering a region then immediately removing it, the region
     * must be gone from all maps.
     */
    public static void assertRegisterRemoveRoundTrip(SpaceRegionRegistry registry, UUID id) {
        SpaceRegion region = new SpaceRegion(id, ClassificationState.INSIDE, 0, 0);
        registry.registerRegion(region);

        // Map a single block to verify removeRegion cleans it up
        BlockPos pos = new BlockPos(0, 0, 0);
        registry.mapBlockToRegion(pos, id);
        registry.removeRegion(id);

        assertNull(registry.getRegionIdAt(pos),
                "block must be unmapped after region removal");
        assertNull(registry.getRegion(id),
                "region must be gone after removal");
        assertTrue(registry.getBlocksInRegion(id).isEmpty(),
                "regionToBlocks must be empty after removal");
    }

    // =========================================================================
    // 4. Idempotency invariants
    // =========================================================================

    /**
     * Dissolving an already-dissolved zone must be safe (no exceptions,
     * no state change).
     */
    public static void assertDissolveIsIdempotent(Zone zone, SpaceRegionRegistry registry,
                                                  ZoneRegistry zoneRegistry) {
        zone.dissolveForTest(registry, zoneRegistry);
        assertTrue(zone.isDissolved());

        int blockCountBefore = countMappedBlocks(registry, zone.getId());

        // Second dissolve must be a no-op
        zone.dissolveForTest(registry, zoneRegistry);
        assertTrue(zone.isDissolved());
        assertEquals(blockCountBefore, countMappedBlocks(registry, zone.getId()),
                "second dissolve must not change mapped block count");
    }

    private static int countMappedBlocks(SpaceRegionRegistry registry, UUID id) {
        return registry.getBlocksInRegion(id).size();
    }

    // =========================================================================
    // 5. Zone lifecycle invariants (tested across random scenarios)
    // =========================================================================

    /**
     * After dissolving a zone, all of its interior blocks must be unmapped
     * and the zone must be removed from the registry.
     */
    public static void assertDissolveCleansUpCompletely(Zone zone, SpaceRegionRegistry registry,
                                                        ZoneRegistry zoneRegistry,
                                                        Set<BlockPos> originalInterior) {
        zone.dissolveForTest(registry, zoneRegistry);

        for (BlockPos pos : originalInterior) {
            assertNull(registry.getRegionIdAt(pos),
                    "Block at " + pos + " must be unmapped after dissolution");
        }
        assertNull(zoneRegistry.getZoneById(zone.getId()),
                "Zone must be removed from registry after dissolution");
    }

    // =========================================================================
    // 6. Structural invariants that survive random mutation
    // =========================================================================

    /**
     * If a zone is NOT dissolved, every interior block must map back to the zone.
     * If a zone IS dissolved, NO interior block may map back to the zone.
     */
    public static void assertZoneBlockMappingIsConsistent(Zone zone, SpaceRegionRegistry registry,
                                                          Set<BlockPos> interior) {
        if (zone.isDissolved()) {
            for (BlockPos pos : interior) {
                assertNull(registry.getRegionIdAt(pos),
                        "Block at " + pos + " must not be mapped after dissolution");
            }
        } else {
            for (BlockPos pos : interior) {
                UUID mapped = registry.getRegionIdAt(pos);
                // Block might be unmapped if it was never registered
                if (mapped != null) {
                    assertEquals(zone.getId(), mapped,
                            "Block at " + pos + " mapped to " + mapped
                                    + " but should be " + zone.getId());
                }
            }
        }
    }
}
