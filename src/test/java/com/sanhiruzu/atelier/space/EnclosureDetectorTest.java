package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EnclosureDetector#bfs} — the core detection logic.
 *
 * <p>All tests use a fake world: a {@code Set<BlockPos>} of "air" positions.
 * The BFS predicate is simply {@code air::contains}.  No Minecraft server
 * is needed, which means failures surface immediately as unit-test failures
 * rather than requiring a full in-game reproduction.</p>
 *
 * <p>The scenarios tested here are exactly the cases that DON'T work with
 * a perfect sealed cube: open-top rooms, rooms with door gaps, L-shapes,
 * zone adjacency, and expansion across chunk seams.</p>
 */
class EnclosureDetectorTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a slab of "open sky / open terrain" air that exceeds MAX_INTERIOR_BLOCKS,
     * so the BFS correctly leaks when it enters this region.
     * In the real world, sky and open terrain are effectively infinite; we use a slab
     * large enough to exceed the 10 000-block limit.
     *
     * @param y the Y level of the slab
     */
    private static Set<BlockPos> openAir(int y) {
        // 105×105 = 11 025 > MAX_INTERIOR_BLOCKS (10 000)
        return new HashSet<>(box(-52, y, -52, 52, y, 52));
    }

    /**
     * Runs 3D BFS with the given air set and a generous limit.
     */
    private static Set<BlockPos> bfs3D(Set<BlockPos> air, BlockPos seed) {
        return bfs3D(air, Set.of(), seed, EnclosureDetector.MAX_INTERIOR_BLOCKS);
    }

    private static Set<BlockPos> bfs3D(Set<BlockPos> air, Set<BlockPos> inZone,
                                       BlockPos seed, int limit) {
        return EnclosureDetector.bfs(seed, Arrays.asList(Direction.values()),
                pos -> air.contains(pos) && !inZone.contains(pos), limit);
    }

    /**
     * Runs horizontal-only BFS at seed.getY() with the given air set.
     */
    private static Set<BlockPos> bfsH(Set<BlockPos> air, BlockPos seed) {
        return bfsH(air, Set.of(), seed, EnclosureDetector.MAX_INTERIOR_BLOCKS);
    }

    private static Set<BlockPos> bfsH(Set<BlockPos> air, Set<BlockPos> inZone,
                                      BlockPos seed, int limit) {
        int y = seed.getY();
        return EnclosureDetector.bfs(seed, Direction.Plane.HORIZONTAL,
                pos -> pos.getY() == y && air.contains(pos) && !inZone.contains(pos),
                limit);
    }

    /**
     * Builds an air set for a rectangular prism of positions.
     */
    private static Set<BlockPos> box(int x0, int y0, int z0, int x1, int y1, int z1) {
        Set<BlockPos> out = new HashSet<>();
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++)
                    out.add(new BlockPos(x, y, z));
        return out;
    }

    // -------------------------------------------------------------------------
    // 1. Perfectly sealed cube — baseline sanity check
    // -------------------------------------------------------------------------

    @Test
    void sealedCubeFound3D() {
        // 2×2×2 air interior enclosed on all sides (surrounding solid not in air set)
        Set<BlockPos> air = box(1, 1, 1, 2, 2, 2);
        Set<BlockPos> result = bfs3D(air, new BlockPos(1, 1, 1));
        assertNotNull(result, "sealed cube must be detected");
        assertEquals(8, result.size());
    }

    // -------------------------------------------------------------------------
    // 2. Open-top room — the bug that triggered the horizontal fallback
    // -------------------------------------------------------------------------

    @Test
    void openTopRoom_3dBfsLeaks() {
        // 2×2 floor-level interior at y=1; open sky at y=2 extends >10 000 blocks.
        Set<BlockPos> air = new HashSet<>();
        air.addAll(box(1, 1, 1, 2, 1, 2)); // floor-level interior
        air.addAll(openAir(2));              // open sky — exceeds BFS limit

        Set<BlockPos> result = bfs3D(air, new BlockPos(1, 1, 1));
        assertNull(result, "3D BFS must leak through open top (sky connected)");
    }

    @Test
    void openTopRoom_horizontalBfsDetects() {
        // Same room — horizontal BFS stays on y=1 and finds bounded interior.
        Set<BlockPos> air = new HashSet<>();
        air.addAll(box(1, 1, 1, 2, 1, 2));  // floor-level interior
        air.addAll(box(1, 2, 1, 2, 99, 2)); // sky above (shouldn't be entered)

        Set<BlockPos> result = bfsH(air, new BlockPos(1, 1, 1));
        assertNotNull(result, "horizontal BFS must detect open-top room");
        assertEquals(Set.of(new BlockPos(1, 1, 1), new BlockPos(2, 1, 1),
                        new BlockPos(1, 1, 2), new BlockPos(2, 1, 2)),
                result);
    }

    @Test
    void openTopRoom_4x4interior() {
        // Larger: 4×4 interior at y=1, walls on all sides, open sky above.
        Set<BlockPos> air = new HashSet<>();
        air.addAll(box(1, 1, 1, 4, 1, 4)); // interior
        air.addAll(box(1, 2, 1, 4, 99, 4)); // open sky

        Set<BlockPos> result = bfsH(air, new BlockPos(2, 1, 2));
        assertNotNull(result);
        assertEquals(16, result.size(), "all 4×4 interior blocks should be found");
        assertFalse(result.stream().anyMatch(p -> p.getY() != 1),
                "horizontal BFS must not stray above y=1");
    }

    // -------------------------------------------------------------------------
    // 3. Open-top room with a gap (missing wall block = not yet enclosed)
    // -------------------------------------------------------------------------

    @Test
    void openTopRoom_withGap_notDetected() {
        // 2×2 interior at y=1, but one wall is missing — a gap at (0,1,1).
        // Both (0,1,1) and (-1,1,1), (-2,1,1) … are all air → horizontal BFS leaks.
        Set<BlockPos> air = new HashSet<>();
        air.addAll(box(1, 1, 1, 2, 1, 2)); // interior
        air.addAll(openAir(1));             // open exterior >10 000 blocks (includes gap path)

        Set<BlockPos> result = bfsH(air, new BlockPos(1, 1, 1));
        assertNull(result, "horizontal BFS must leak through the open wall gap");
    }

    // -------------------------------------------------------------------------
    // 4. Door counts as a wall (isAir() is false for a placed door)
    // -------------------------------------------------------------------------

    @Test
    void doorActsAsWall_roomDetected() {
        // 2×2 interior; the "door" at (0,1,1) is NOT in the air set (it's solid).
        // The BFS must stop at the door, giving a bounded region.
        Set<BlockPos> air = new HashSet<>();
        air.addAll(box(1, 1, 1, 2, 1, 2)); // interior only

        Set<BlockPos> result3D = bfs3D(air, new BlockPos(1, 1, 1));
        Set<BlockPos> resultH = bfsH(air, new BlockPos(1, 1, 1));

        // Both should find the bounded interior (door position is not passable)
        assertNotNull(result3D, "3D BFS with ceiling would find sealed room with door");
        assertNotNull(resultH, "horizontal BFS must find open-top room with door");
        assertEquals(4, result3D.size());
        assertEquals(4, resultH.size());
    }

    // -------------------------------------------------------------------------
    // 5. L-shaped room (non-rectangular interior)
    // -------------------------------------------------------------------------

    @Test
    void lShapedRoom_fullyDetected() {
        // L-shape at y=1: 3 blocks east + 2 blocks south off the corner
        //  (1,1,1) (2,1,1) (3,1,1)
        //  (1,1,2) (2,1,2)
        //  (1,1,3)
        Set<BlockPos> interior = Set.of(
                new BlockPos(1, 1, 1), new BlockPos(2, 1, 1), new BlockPos(3, 1, 1),
                new BlockPos(1, 1, 2), new BlockPos(2, 1, 2),
                new BlockPos(1, 1, 3)
        );
        // Wrap it with only interior air (walls are implicit by absence)
        Set<BlockPos> result = bfsH(interior, new BlockPos(1, 1, 1));
        assertNotNull(result);
        assertEquals(interior, result, "must find all 6 blocks of the L-shape");
    }

    // -------------------------------------------------------------------------
    // 6. Zone adjacency — BFS stops at blocks already owned by another zone
    // -------------------------------------------------------------------------

    @Test
    void existingZoneActsAsBarrier() {
        // Extension air (1,1,1) and (2,1,1) next to an existing zone at (3,1,1).
        // The BFS should NOT enter (3,1,1) and return only the 2 extension blocks.
        Set<BlockPos> allAir = Set.of(
                new BlockPos(1, 1, 1), new BlockPos(2, 1, 1), new BlockPos(3, 1, 1)
        );
        Set<BlockPos> inZone = Set.of(new BlockPos(3, 1, 1)); // already owned

        Set<BlockPos> resultH = bfsH(allAir, inZone, new BlockPos(1, 1, 1),
                EnclosureDetector.MAX_INTERIOR_BLOCKS);
        assertNotNull(resultH, "extension room should be bounded by the zone boundary");
        assertEquals(Set.of(new BlockPos(1, 1, 1), new BlockPos(2, 1, 1)), resultH,
                "must not cross into the existing zone");
    }

    @Test
    void existingZoneOnAllSides_emptyResult() {
        // A single air block completely surrounded by zone blocks.
        // BFS finds just the seed (nothing passable adjacent).
        Set<BlockPos> allAir = Set.of(new BlockPos(0, 1, 0));
        Set<BlockPos> inZone = Set.of(
                new BlockPos(1, 1, 0), new BlockPos(-1, 1, 0),
                new BlockPos(0, 1, 1), new BlockPos(0, 1, -1)
        );
        // All 4 horizontal neighbours are in-zone → horizontal BFS can't expand.
        Set<BlockPos> result = bfsH(allAir, inZone, new BlockPos(0, 1, 0),
                EnclosureDetector.MAX_INTERIOR_BLOCKS);
        // BFS returns just {seed} — size 1, which the caller treats as "no new zone"
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // -------------------------------------------------------------------------
    // 7. Open plain — no walls at all → must not be detected as a room
    // -------------------------------------------------------------------------

    @Test
    void openPlain_notDetected() {
        // Flat air in a very large area — hits the limit immediately.
        // Air extends 200 blocks in each horizontal direction.
        Set<BlockPos> air = box(-200, 1, -200, 200, 1, 200);

        Set<BlockPos> result = bfsH(air, new BlockPos(0, 1, 0));
        assertNull(result, "open plain must leak (exceeds block limit)");
    }

    // -------------------------------------------------------------------------
    // 8. Room that spans two chunk-sized regions (chunk-seam crossing)
    // -------------------------------------------------------------------------

    @Test
    void crossChunkRoom_detectedAsOne() {
        // A 4-block-wide room straddling x=0 and x=16 (chunk seam).
        // BFS doesn't know about chunks — it just follows air positions.
        Set<BlockPos> air = new HashSet<>();
        air.addAll(box(14, 1, 1, 17, 1, 2)); // spans x=14..17 across chunk seam at x=16

        Set<BlockPos> result = bfsH(air, new BlockPos(15, 1, 1));
        assertNotNull(result, "room spanning chunk seam should be detected as one region");
        assertEquals(8, result.size());
    }

    // -------------------------------------------------------------------------
    // 9. Enclosure completed by the LAST wall block (detection timing)
    // -------------------------------------------------------------------------

    @Test
    void lastWallBlock_completesEnclosure() {
        // Before: gap at (0,1,1) — BFS leaks west into open terrain.
        Set<BlockPos> beforeAir = new HashSet<>();
        beforeAir.addAll(box(1, 1, 1, 2, 1, 2)); // interior
        beforeAir.addAll(openAir(1));            // open exterior >10 000 blocks
        assertNull(bfsH(beforeAir, new BlockPos(1, 1, 1)), "must not detect before wall placed");

        // After: (0,1,1) is now a solid wall block → NOT in air set.
        Set<BlockPos> afterAir = box(1, 1, 1, 2, 1, 2); // interior only
        Set<BlockPos> result = bfsH(afterAir, new BlockPos(1, 1, 1));
        assertNotNull(result, "must detect room after last wall block placed");
        assertEquals(4, result.size());
    }

    // -------------------------------------------------------------------------
    // 10. BFS limit edge cases
    // -------------------------------------------------------------------------

    @Test
    void exactly_at_limit_returnsNull() {
        // A corridor exactly limit+1 blocks long → should leak.
        int limit = 5;
        Set<BlockPos> air = box(0, 0, 0, limit, 0, 0); // 6 blocks = limit+1
        Set<BlockPos> result = EnclosureDetector.bfs(
                new BlockPos(0, 0, 0),
                Arrays.asList(Direction.values()),
                air::contains,
                limit);
        assertNull(result, "region exactly over limit must return null");
    }

    @Test
    void one_under_limit_returnsResult() {
        // A corridor exactly limit blocks long → should be found.
        int limit = 5;
        Set<BlockPos> air = box(0, 0, 0, limit - 1, 0, 0); // 5 blocks = exactly limit
        Set<BlockPos> result = EnclosureDetector.bfs(
                new BlockPos(0, 0, 0),
                Arrays.asList(Direction.values()),
                air::contains,
                limit);
        assertNotNull(result);
        assertEquals(limit, result.size());
    }
}
