package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.space.ClassificationState;
import com.sanhiruzu.atelier.space.SpaceRegion;
import com.sanhiruzu.atelier.space.SpaceRegionRegistry;
import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.function.Predicate;

/**
 * Test harness for zone validation logic. Builds rooms and wires
 * {@link SpaceRegionRegistry} with predicate-based block state queries
 * so no Minecraft bootstrap is needed.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * ZoneTestHarness h = new ZoneTestHarness("test")
 *     .addInterior(box(0, 1, 0, 3, 1, 3))   // 4x4x1 interior
 *     .addEntry(0, 1, 3);                     // door at one wall position
 *
 * assertTrue(h.isValidPlayerBuiltZone());
 * }</pre>
 */
public class ZoneTestHarness {

    private final SpaceRegionRegistry registry;
    private final UUID regionId;
    private final Set<BlockPos> interior = new HashSet<>();
    private final Set<BlockPos> entries = new HashSet<>();

    public ZoneTestHarness(String key) {
        this.registry = SpaceRegionRegistry.createForTest(key);
        this.regionId = UUID.randomUUID();
    }

    public ZoneTestHarness(String key, UUID regionId) {
        this.registry = SpaceRegionRegistry.createForTest(key);
        this.regionId = regionId;
    }

    // ---- builders ----

    public ZoneTestHarness addInterior(BlockPos pos) {
        interior.add(pos);
        registry.mapBlockToRegion(pos, regionId);
        return this;
    }

    public ZoneTestHarness addInterior(Collection<BlockPos> positions) {
        for (BlockPos pos : positions) addInterior(pos);
        return this;
    }

    public ZoneTestHarness addEntry(BlockPos pos) {
        entries.add(pos);
        return this;
    }

    public ZoneTestHarness addEntry(int x, int y, int z) {
        return addEntry(new BlockPos(x, y, z));
    }

    public ZoneTestHarness addEntries(Collection<BlockPos> positions) {
        entries.addAll(positions);
        return this;
    }

    /**
     * Ensures the region has a SpaceRegion so volume/openingArea queries work.
     */
    public ZoneTestHarness withSpaceRegion(int openingArea) {
        if (registry.getRegion(regionId) == null) {
            registry.registerRegion(new SpaceRegion(regionId, ClassificationState.INSIDE,
                    interior.size(), openingArea));
        }
        return this;
    }

    // ---- predicates (pass to extracted methods) ----

    public Predicate<BlockPos> isEntry() {
        return entries::contains;
    }

    // ---- validation (calls the extracted methods directly) ----

    public boolean isValidPlayerBuiltZone() {
        return ZoneRegistry.isValidPlayerBuiltZone(regionId, registry, isEntry(), null);
    }

    public boolean hasLiveEntry() {
        return hasLiveEntry(null);
    }

    /**
     * Checks hasLiveEntry with an optional excluded position (simulates block just broken).
     */
    public boolean hasLiveEntry(BlockPos excluded) {
        // Build a minimal Zone for the hasLiveEntry check
        Zone zone = new Zone(regionId, interior, 0);
        return zone.hasLiveEntry(isEntry(), excluded);
    }

    public void computeSpatialExtent(ZoneData zone) {
        ZoneRegistry.computeSpatialExtent(zone, regionId, registry, isEntry());
    }

    // ---- accessors ----

    public SpaceRegionRegistry registry() {
        return registry;
    }

    public UUID regionId() {
        return regionId;
    }

    public Set<BlockPos> interior() {
        return Collections.unmodifiableSet(interior);
    }

    public Set<BlockPos> entries() {
        return Collections.unmodifiableSet(entries);
    }

    // ---- static helpers ----

    /**
     * Creates a rectangular prism of positions (inclusive on both ends).
     */
    public static Set<BlockPos> box(int x0, int y0, int z0, int x1, int y1, int z1) {
        Set<BlockPos> out = new HashSet<>();
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++)
                    out.add(new BlockPos(x, y, z));
        return out;
    }

    /**
     * Builds a simple room: interior box + a door at one wall position.
     */
    public static ZoneTestHarness room(String key, int width, int height, int depth,
                                       int doorX, int doorY, int doorZ) {
        return room(key, 0, 1, 0, width, height, depth, doorX, doorY, doorZ);
    }

    /**
     * Builds a room with interior starting at (ox, oy, oz).
     */
    public static ZoneTestHarness room(String key, int ox, int oy, int oz,
                                       int width, int height, int depth,
                                       int doorX, int doorY, int doorZ) {
        ZoneTestHarness h = new ZoneTestHarness(key);
        h.addInterior(box(ox, oy, oz, ox + width - 1, oy + height - 1, oz + depth - 1));
        h.addEntry(doorX, doorY, doorZ);
        return h;
    }

    /**
     * Builds a roofless room: y+1 is open to "outside" (no interior blocks above).
     */
    public static ZoneTestHarness rooflessRoom(String key, int width, int depth) {
        // Interior at y=1 only
        ZoneTestHarness h = new ZoneTestHarness(key);
        h.addInterior(box(0, 1, 0, width - 1, 1, depth - 1));
        // Opening area = each interior block's UP face × width*depth
        h.withSpaceRegion(width * depth);
        return h;
    }

    /**
     * Roofless room with a door.
     */
    public static ZoneTestHarness rooflessRoomWithDoor(String key, int width, int depth,
                                                       int doorX, int doorZ) {
        ZoneTestHarness h = rooflessRoom(key, width, depth);
        h.addEntry(doorX, 1, doorZ);
        return h;
    }
}
