package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.space.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * Non-player-facing spatial entity. After bootstrap discovery on chunk load, the zone handles all
 * block events itself: growing when adjacent walls are dug out, shrinking when blocks are
 * placed inside, dissolving when its last entry point is removed, and merging when it
 * touches another zone.
 *
 * <p>The bitfield in {@link ChunkClassificationData} is the persisted state. Zone entities
 * are the runtime geometry layer — they read/write the bitfield and {@link SpaceRegionRegistry}
 * as they expand, shrink, and dissolve. Interpreted room state, UI data, quality, and
 * naming are computed by {@link ZoneRegistry}'s room-facing evaluation methods.</p>
 */
public final class Zone {
    private static final Logger LOGGER = LoggerFactory.getLogger("ZenAtelier/Zones");
    private static final int BFS_LIMIT = 10_000;

    private final UUID id;
    private final Set<BlockPos> interiorBlocks;
    private final Set<BlockPos> ownedEntryBlocks;
    private final Set<BlockPos> coreBlocks;
    private final Map<Integer, Set<BlockPos>> boundaryBlocksByY;
    private int volume;
    private int openingArea;
    private boolean dissolved;
    private boolean sealed;
    private long gracePeriodExpiresAt = -1L;
    @Nullable
    private ZoneEntry mainEntryway;

    /**
     * Identity-only constructor for client-side tracking (ZoneAttachment).
     */
    public Zone(UUID id) {
        this.id = id;
        this.interiorBlocks = new HashSet<>();
        this.ownedEntryBlocks = new HashSet<>();
        this.coreBlocks = new HashSet<>();
        this.boundaryBlocksByY = new HashMap<>();
        this.volume = 0;
        this.openingArea = 0;
    }

    public Zone(UUID id, Set<BlockPos> interiorBlocks, int openingArea) {
        this.id = id;
        this.interiorBlocks = new HashSet<>(interiorBlocks);
        this.ownedEntryBlocks = new HashSet<>();
        this.coreBlocks = new HashSet<>();
        this.boundaryBlocksByY = new HashMap<>();
        this.volume = interiorBlocks.size();
        this.openingArea = openingArea;
    }

    // ---- Core operations ----

    /**
     * BFS-expand from {@code start} into adjacent air, claiming newly exposed space.
     * Stops at OUTSIDE-classified air (don't leak outdoors), non-air blocks (walls),
     * and already-claimed positions.
     *
     * <p>Crosses chunk boundaries — updates each chunk's bitfield as it goes.
     * If expansion reaches another zone's interior, records it for merging.</p>
     */
    public ExpansionResult tryExpand(BlockPos start, ServerLevel level) {
        if (dissolved) return ExpansionResult.EMPTY;
        // BreakEvent fires before Minecraft removes the block, so start may still be solid.
        // BFS checks actual block states for neighbors, not for start itself.

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        Set<UUID> mergedZoneIds = new HashSet<>();

        // --- Phase 1a: 3D dry-run BFS ---
        Set<BlockPos> claimed = bfs3D(start, registry, level, mergedZoneIds);

        // --- Phase 1b: if 3D leaked (open-top room), fall back to horizontal BFS ---
        if (claimed == null) {
            mergedZoneIds.clear();
            claimed = bfsHorizontal(start, registry, level, mergedZoneIds);
        }

        // Both BFS modes leaked — treat as opening to outside.
        if (claimed == null) {
            int newOpenings = 0;
            for (Direction dir : Direction.values()) {
                if (interiorBlocks.contains(start.relative(dir))) newOpenings++;
            }
            openingArea += newOpenings;
            SpaceRegion sr = registry.getRegion(id);
            if (sr != null) sr.adjustOpeningArea(newOpenings);
            return ExpansionResult.EMPTY;
        }

        // BFS didn't leak but found nothing expandable — multi-block structure
        // (e.g. bed) where the other half is still solid because BreakEvent fires
        // before Minecraft removes it. Signal the caller to retry one tick later.
        if (claimed.size() <= 1) {
            return ExpansionResult.DEFER;
        }

        // --- Phase 2: apply changes ---
        for (BlockPos pos : claimed) {
            ChunkAccess chunk = level.getChunk(pos);
            ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
            data.setBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15, ClassificationState.INSIDE);
            registry.mapBlockToRegion(pos, this.id);
        }
        interiorBlocks.addAll(claimed);
        claimAdjacentEntryBlocks(claimed, level, registry);
        volume += claimed.size();
        openingArea = recomputeOpeningArea(level);
        SpaceRegion sr = registry.getRegion(id);
        if (sr != null) sr.adjustOpeningArea(openingArea - sr.getOpeningArea());

        // Merge adjacent zones encountered during BFS
        Set<UUID> merged = new HashSet<>();
        ZoneRegistry zoneRegistry = ZoneRegistry.get(level);
        for (UUID mergedId : mergedZoneIds) {
            Zone other = zoneRegistry.getZoneById(mergedId);
            if (other != null && !other.equals(this) && !other.isDissolved()) {
                mergeWith(other, level);
                merged.add(mergedId);
            }
        }

        ZoneSavedData.get(level).saveZone(id, getAllOwnedBlocks(), openingArea,
                zoneRegistry.getCustomName(id));

        LOGGER.debug("[ZONE] Expanded {} by {} blocks", id.toString().substring(0, 8), claimed.size());
        return new ExpansionResult(merged, claimed.size(), false);
    }

    /**
     * 3D bounded BFS. Returns null if the BFS leaks (open space).
     */
    @Nullable
    private Set<BlockPos> bfs3D(BlockPos start, SpaceRegionRegistry registry,
                                ServerLevel level, Set<UUID> mergedZoneIds) {
        Set<BlockPos> claimed = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        claimed.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (claimed.contains(neighbor) || interiorBlocks.contains(neighbor)) continue;
                UUID otherId = registry.getRegionIdAt(neighbor);
                if (otherId != null && !otherId.equals(this.id)) {
                    mergedZoneIds.add(otherId);
                    continue;
                }
                if (!level.getBlockState(neighbor).isAir()) continue;
                // Stop at air that the classifier already marked as OUTSIDE —
                // expansion has reached unbounded open space.
                ChunkAccess neighborChunk = level.getChunk(neighbor);
                ChunkClassificationData nd = ChunkClassificationAttachment.get(neighborChunk);
                if (nd.getBlockState(neighbor.getX() & 15, neighbor.getY(),
                        neighbor.getZ() & 15) == ClassificationState.OUTSIDE) return null;
                if (claimed.size() >= BFS_LIMIT) return null; // leaked
                claimed.add(neighbor);
                queue.add(neighbor);
            }
        }
        return claimed;
    }

    /**
     * Horizontal-only bounded BFS (stays on start.getY()). Returns null if it leaks.
     */
    @Nullable
    private Set<BlockPos> bfsHorizontal(BlockPos start, SpaceRegionRegistry registry,
                                        ServerLevel level, Set<UUID> mergedZoneIds) {
        Set<BlockPos> claimed = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        claimed.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(dir);
                if (claimed.contains(neighbor) || interiorBlocks.contains(neighbor)) continue;
                UUID otherId = registry.getRegionIdAt(neighbor);
                if (otherId != null && !otherId.equals(this.id)) {
                    mergedZoneIds.add(otherId);
                    continue;
                }
                if (!level.getBlockState(neighbor).isAir()) continue;
                // Stop at air that the classifier already marked as OUTSIDE —
                // expansion has reached unbounded open space.
                ChunkAccess neighborChunk = level.getChunk(neighbor);
                ChunkClassificationData nd = ChunkClassificationAttachment.get(neighborChunk);
                if (nd.getBlockState(neighbor.getX() & 15, neighbor.getY(),
                        neighbor.getZ() & 15) == ClassificationState.OUTSIDE) return null;
                if (claimed.size() >= BFS_LIMIT) return null; // leaked
                claimed.add(neighbor);
                queue.add(neighbor);
            }
        }
        return claimed;
    }

    /**
     * Removes {@code placedPos} from the interior, then flood-fills to detect splits.
     * Returns new Zones for disconnected components (empty list if no split).
     * Keeps the largest component for this zone.
     */
    public List<Zone> tryShrink(BlockPos placedPos, ServerLevel level) {
        if (dissolved) return List.of();
        if (!interiorBlocks.contains(placedPos)) return List.of();

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        registry.unmapBlock(placedPos);
        interiorBlocks.remove(placedPos);
        ownedEntryBlocks.remove(placedPos);
        coreBlocks.remove(placedPos);
        volume--;

        ChunkAccess chunk = level.getChunk(placedPos);
        ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
        data.setBlockState(placedPos.getX() & 15, placedPos.getY(), placedPos.getZ() & 15, ClassificationState.SOLID);

        if (interiorBlocks.isEmpty()) {
            dissolve(level);
            return List.of();
        }

        List<Set<BlockPos>> components = findConnectedComponents(level);
        if (components.size() <= 1) {
            // No split — just update the shrunk zone
            openingArea = recomputeOpeningArea(level);
            SpaceRegion sr = registry.getRegion(id);
            if (sr != null) sr.adjustOpeningArea(openingArea - sr.getOpeningArea());
            ZoneSavedData.get(level).saveZone(id, getAllOwnedBlocks(), openingArea,
                    ZoneRegistry.get(level).getCustomName(id));
            return List.of();
        }

        // Sort by size descending — keep the largest for this zone
        components.sort((a, b) -> Integer.compare(b.size(), a.size()));
        Set<BlockPos> keep = components.get(0);
        interiorBlocks.clear();
        interiorBlocks.addAll(keep);
        // Clean up core blocks not in the kept component
        coreBlocks.retainAll(keep);
        // Clean up entry blocks not adjacent to the kept component
        ownedEntryBlocks.removeIf(entryPos ->
                Arrays.stream(Direction.values())
                        .noneMatch(dir -> keep.contains(entryPos.relative(dir)))
        );
        volume = keep.size();
        openingArea = recomputeOpeningArea(level);
        SpaceRegion srSplit = registry.getRegion(id);
        if (srSplit != null) srSplit.adjustOpeningArea(openingArea - srSplit.getOpeningArea());
        ZoneSavedData.get(level).saveZone(id, getAllOwnedBlocks(), openingArea,
                ZoneRegistry.get(level).getCustomName(id));

        // Create new zones for smaller components (createFromBootstrap saves them)
        List<Zone> newZones = new ArrayList<>();
        for (int i = 1; i < components.size(); i++) {
            Set<BlockPos> component = components.get(i);
            Zone newZone = createFromBootstrap(UUID.randomUUID(), component,
                    computeOpeningAreaFor(component, level), level);
            if (newZone != null) {
                newZones.add(newZone);
            } else {
                // Invalid component — unmap its blocks
                for (BlockPos pos : component) {
                    registry.unmapBlock(pos);
                    ChunkAccess c = level.getChunk(pos);
                    ChunkClassificationData d = ChunkClassificationAttachment.get(c);
                    d.setBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15, ClassificationState.SOLID);
                }
            }
        }
        return newZones;
    }

    /**
     * True if any interior air block touches a door, trapdoor, stairs, or slab.
     * Pass a non-null {@code excludedNeighbor} to pretend that position is already
     * removed — used when checking entry viability during a BreakEvent, which fires
     * before Minecraft actually removes the block.
     */
    public boolean hasLiveEntry(ServerLevel level, @Nullable BlockPos excludedNeighbor) {
        Set<BlockPos> allOwnedBlocks = getAllOwnedBlocks();
        ZoneEntry entry = findPrimaryEntry(union(coreBlocks, ownedEntryBlocks), allOwnedBlocks, level, excludedNeighbor);
        if (entry == null) {
            entry = findPrimaryEntry(allOwnedBlocks, allOwnedBlocks, level, excludedNeighbor);
        }
        return entry != null;
    }

    public boolean hasLiveEntry(ServerLevel level) {
        return hasLiveEntry(level, null);
    }

    /**
     * Testable overload that takes an entry predicate instead of a Level.
     * Production passes a lambda calling {@code level::getBlockState}; tests pass
     * a set-membership check (e.g. {@code doorPositions::contains}).
     */
    boolean hasLiveEntry(java.util.function.Predicate<BlockPos> isEntry,
                         @Nullable BlockPos excludedNeighbor) {
        for (BlockPos pos : interiorBlocks) {
            if (!pos.equals(excludedNeighbor) && isEntry.test(pos)) return true;
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (neighbor.equals(excludedNeighbor)) continue;
                if (isEntry.test(neighbor)) return true;
            }
        }
        return false;
    }

    /**
     * Unmaps all interior blocks from the registry, clears bitfield entries,
     * and removes this zone from ZoneRegistry.
     */
    public void dissolve(ServerLevel level) {
        if (dissolved) return;
        dissolved = true;

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        for (BlockPos pos : interiorBlocks) {
            registry.unmapBlock(pos);
            ChunkAccess chunk = level.getChunk(pos);
            ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
            data.setBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15, ClassificationState.SOLID);
        }
        for (BlockPos pos : ownedEntryBlocks) {
            registry.unmapBlock(pos);
            ChunkAccess chunk = level.getChunk(pos);
            ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
            data.setBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15, ClassificationState.SOLID);
        }

        SpaceRegion region = registry.getRegion(id);
        if (region != null) {
            registry.removeRegion(id);
        }

        coreBlocks.clear();
        ownedEntryBlocks.clear();
        boundaryBlocksByY.clear();
        mainEntryway = null;

        ZoneRegistry.get(level).dissolveZone(id);
        ZoneSavedData.get(level).removeZone(id);
        LOGGER.debug("[ZONE] Dissolved zone {}", id.toString().substring(0, 8));
    }

    /**
     * Test-only: dissolves the zone given registries directly, bypassing Level lookups.
     * Skips chunk bitfield cleanup (tests don't have ChunkClassificationData).
     */
    void dissolveForTest(SpaceRegionRegistry registry, ZoneRegistry zoneRegistry) {
        if (dissolved) return;
        dissolved = true;
        for (BlockPos pos : interiorBlocks) {
            registry.unmapBlock(pos);
        }
        for (BlockPos pos : ownedEntryBlocks) {
            registry.unmapBlock(pos);
        }
        SpaceRegion region = registry.getRegion(id);
        if (region != null) registry.removeRegion(id);
        zoneRegistry.dissolveZone(id);
    }

    /**
     * Absorbs all interior blocks from {@code other} into this zone.
     * The other zone is dissolved.
     */
    public void mergeWith(Zone other, ServerLevel level) {
        if (other.equals(this) || other.isDissolved()) return;

        interiorBlocks.addAll(other.interiorBlocks);
        ownedEntryBlocks.addAll(other.ownedEntryBlocks);
        coreBlocks.addAll(other.coreBlocks);
        // Prefer surviving zone's main entryway if set, otherwise take other's
        if (mainEntryway == null && other.mainEntryway != null) {
            mainEntryway = other.mainEntryway;
        }
        volume += other.volume;

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);

        // Merge SpaceRegions first — transfers volume and remaps blockToRegion/regionToBlocks
        // in one step before we clear other's block set. registry.mergeRegions also removes
        // other.id from the regions map, so removeRegion below becomes a safe no-op.
        SpaceRegion myRegion = registry.getRegion(id);
        SpaceRegion otherRegion = registry.getRegion(other.id);
        if (myRegion != null && otherRegion != null) {
            registry.mergeRegions(id, other.id);
        } else {
            // SpaceRegion missing for one side — fall back to per-block remapping
            for (BlockPos pos : other.getAllOwnedBlocks()) {
                registry.mapBlockToRegion(pos, this.id);
            }
        }

        openingArea = recomputeOpeningArea(level);

        ZoneRegistry zoneRegistry = ZoneRegistry.get(level);
        zoneRegistry.transferMetadata(other.id, this.id);

        // Dissolve other without cleanup (blocks already transferred above)
        other.dissolved = true;
        other.interiorBlocks.clear();
        other.ownedEntryBlocks.clear();
        registry.removeRegion(other.id); // no-op if mergeRegions already removed it
        zoneRegistry.dissolveZone(other.id);

        ZoneSavedData savedData = ZoneSavedData.get(level);
        savedData.saveZone(this.id, this.getAllOwnedBlocks(), this.openingArea,
                zoneRegistry.getCustomName(this.id));
        savedData.removeZone(other.id);

        LOGGER.debug("[ZONE] Merged {} into {}", other.id.toString().substring(0, 8),
                id.toString().substring(0, 8));
    }

    // ---- Factory ----

    /**
     * Creates a Zone from bootstrap classification output.
     * Validates the zone has a player block and a live entry point.
     * Registers blocks in the registry and the zone in ZoneRegistry.
     * Returns null if the region is invalid (no entry, pure natural cave).
     */
    public static Zone createFromBootstrap(UUID id, Set<BlockPos> blocks,
                                           int openingArea, ServerLevel level) {
        if (blocks.isEmpty()) return null;

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        Zone zone = new Zone(id, blocks, openingArea);
        zone.claimAdjacentEntryBlocks(blocks, level, registry);

        // Register all blocks. SpaceRegion starts with volume=0 so that each
        // mapBlockToRegion call increments it by exactly one — keeping SpaceRegion.volume
        // in sync with regionToBlocks.size() from the very first mapping.
        if (registry.getRegion(id) == null) {
            registry.registerRegion(new SpaceRegion(id, ClassificationState.INSIDE,
                    0, zone.openingArea));
        }
        for (BlockPos pos : zone.getAllOwnedBlocks()) {
            registry.mapBlockToRegion(pos, id);
            // Ensure bitfield is INSIDE
            ChunkAccess chunk = level.getChunk(pos);
            ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
            data.setBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15, ClassificationState.INSIDE);
        }
        zone.openingArea = zone.recomputeOpeningArea(level);
        SpaceRegion registeredRegion = registry.getRegion(id);
        if (registeredRegion != null) {
            registeredRegion.adjustOpeningArea(zone.openingArea - registeredRegion.getOpeningArea());
        }

        // Validate: skip sealed natural caves (no entry, all natural walls)
        if (!zone.hasLiveEntry(level) && zone.openingArea == 0) {
            for (BlockPos pos : zone.getAllOwnedBlocks()) {
                registry.unmapBlock(pos);
                ChunkAccess chunk = level.getChunk(pos);
                ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
                data.setBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15, ClassificationState.SOLID);
            }
            registry.removeRegion(id);
            return null;
        }

        // Ensure minimum playable height
        zone.ensureMinimumHeight(level);

        // Identify core blocks (base-level boundary blocks) and primary entry point
        Set<BlockPos> coreBlocks = identifyCoreBlocks(zone.interiorBlocks, level);
        zone.addCoreBlocks(coreBlocks);
        Set<BlockPos> allOwnedBlocks = zone.getAllOwnedBlocks();
        ZoneEntry mainEntry = findPrimaryEntry(union(coreBlocks, zone.ownedEntryBlocks), allOwnedBlocks, level);
        // Fall back to all interior blocks so internal connectors such as
        // central stairs/landings can act as the entryway.
        if (mainEntry == null) {
            mainEntry = findPrimaryEntry(allOwnedBlocks, allOwnedBlocks, level);
        }
        if (mainEntry != null) {
            zone.setMainEntry(mainEntry);
        }

        ZoneRegistry zr = ZoneRegistry.get(level);
        zr.registerZone(zone);
        ZoneSavedData.get(level).saveZone(id, zone.getAllOwnedBlocks(), zone.openingArea,
                zr.getCustomName(id));
        LOGGER.debug("[ZONE] Bootstrapped zone {} (vol={}, open={}, h={}, coreBlocks={}, entry={})",
                id.toString().substring(0, 8), zone.volume, zone.openingArea, zone.getHeight(),
                coreBlocks.size(), mainEntry != null ? mainEntry.primaryBlock().toShortString() : "none");
        return zone;
    }

    // ---- Helpers ----

    private void claimAdjacentEntryBlocks(Set<BlockPos> blocks,
                                          ServerLevel level,
                                          SpaceRegionRegistry registry) {
        for (BlockPos pos : blocks) {
            for (Direction dir : Direction.values()) {
                addEntryBlockAndDoorParts(ownedEntryBlocks, pos.relative(dir), level, registry, id);
            }
        }
    }

    private static void addEntryBlockAndDoorParts(Set<BlockPos> entries,
                                                  BlockPos pos,
                                                  ServerLevel level,
                                                  SpaceRegionRegistry registry,
                                                  UUID zoneId) {
        var state = level.getBlockState(pos);
        if (!isEntryBlock(state)) return;
        addClaimableEntryBlock(entries, pos, registry, zoneId);

        if (state.is(BlockTags.DOORS)) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).is(BlockTags.DOORS)) {
                addClaimableEntryBlock(entries, above, registry, zoneId);
            }
            BlockPos below = pos.below();
            if (level.getBlockState(below).is(BlockTags.DOORS)) {
                addClaimableEntryBlock(entries, below, registry, zoneId);
            }
        }
    }

    private static void addClaimableEntryBlock(Set<BlockPos> entries,
                                               BlockPos pos,
                                               SpaceRegionRegistry registry,
                                               UUID zoneId) {
        UUID owner = registry.getRegionIdAt(pos);
        if (owner == null || owner.equals(zoneId)) {
            entries.add(pos);
        }
    }

    private static Set<BlockPos> union(Set<BlockPos> first, Set<BlockPos> second) {
        Set<BlockPos> out = new HashSet<>(first);
        out.addAll(second);
        return out;
    }

    private List<Set<BlockPos>> findConnectedComponents(ServerLevel level) {
        Set<BlockPos> remaining = new HashSet<>(interiorBlocks);
        List<Set<BlockPos>> components = new ArrayList<>();
        while (!remaining.isEmpty()) {
            BlockPos seed = remaining.iterator().next();
            remaining.remove(seed);
            Set<BlockPos> component = new HashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>();
            queue.add(seed);
            component.add(seed);
            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = pos.relative(dir);
                    if (remaining.remove(neighbor)) {
                        component.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
            components.add(component);
        }
        return components;
    }

    int recomputeOpeningArea(ServerLevel level) {
        return computeOpeningAreaFor(interiorBlocks, level);
    }

    /**
     * Recomputes opening area from current world state and syncs the delta to the
     * SpaceRegion. Call this whenever a solid block adjacent to the zone is placed or
     * removed without the zone's own interior changing (e.g. a wall block closing a hole).
     */
    public void refreshOpeningArea(ServerLevel level) {
        int fresh = recomputeOpeningArea(level);
        SpaceRegion sr = SpaceRegionRegistry.get(level).getRegion(id);
        if (sr != null) sr.adjustOpeningArea(fresh - sr.getOpeningArea());
        openingArea = fresh;
    }

    // Opening area = faces of interior blocks that touch air outside the interior set.
    // Vertical openings around stairs/slabs/trapdoors/ladders are internal connectors,
    // not outdoor exposure, so they do not reduce enclosure.
    public static int computeOpeningAreaFor(Set<BlockPos> blocks, ServerLevel level) {
        return computeOpeningAreaFor(blocks,
                pos -> level.getBlockState(pos).isAir(),
                pos -> isConnectorBlock(level.getBlockState(pos)));
    }

    public static int computeRawOpeningAreaFor(Set<BlockPos> blocks, ServerLevel level) {
        return computeRawOpeningAreaFor(blocks, pos -> level.getBlockState(pos).isAir());
    }

    static int computeRawOpeningAreaFor(Set<BlockPos> blocks, Predicate<BlockPos> isAir) {
        int openings = 0;
        for (BlockPos pos : blocks) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (isAir.test(neighbor) && !blocks.contains(neighbor)) {
                    openings++;
                }
            }
        }
        return openings;
    }

    static int computeOpeningAreaFor(Set<BlockPos> blocks, Predicate<BlockPos> isAir,
                                     Predicate<BlockPos> isConnectorBlock) {
        int openings = 0;
        for (BlockPos pos : blocks) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (isAir.test(neighbor)
                        && !blocks.contains(neighbor)
                        && !isInternalConnectorOpening(pos, neighbor, dir, isConnectorBlock)) {
                    openings++;
                }
            }
        }
        return openings;
    }

    public static boolean isEntryBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.STAIRS)
                || state.is(BlockTags.SLABS);
    }

    private static boolean isConnectorBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.STAIRS)
                || state.is(BlockTags.SLABS)
                || state.is(BlockTags.CLIMBABLE);
    }

    private static boolean isInternalConnectorOpening(BlockPos interiorPos, BlockPos airNeighbor, Direction dir,
                                                      Predicate<BlockPos> isConnectorBlock) {
        if (dir.getAxis() != Direction.Axis.Y) return false;

        for (BlockPos origin : List.of(interiorPos, airNeighbor)) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (isConnectorBlock.test(origin.offset(dx, dy, dz))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // ---- Getters ----

    public UUID getId() {
        return id;
    }

    public Set<BlockPos> getInteriorBlocks() {
        return Collections.unmodifiableSet(interiorBlocks);
    }

    public Set<BlockPos> getOwnedEntryBlocks() {
        return Collections.unmodifiableSet(ownedEntryBlocks);
    }

    public void addOwnedEntryBlocks(Set<BlockPos> blocks) {
        ownedEntryBlocks.addAll(blocks);
    }

    public Set<BlockPos> getAllOwnedBlocks() {
        Set<BlockPos> all = new HashSet<>(interiorBlocks);
        all.addAll(ownedEntryBlocks);
        return Collections.unmodifiableSet(all);
    }

    public int getVolume() {
        return volume;
    }

    public int getOpeningArea() {
        return openingArea;
    }

    public boolean isDissolved() {
        return dissolved;
    }

    public boolean containsPos(BlockPos pos) {
        return interiorBlocks.contains(pos);
    }

    /**
     * The lowest Y level of any interior block — one above the floor.
     */
    public int getMinY() {
        int min = Integer.MAX_VALUE;
        for (BlockPos pos : interiorBlocks) {
            if (pos.getY() < min) min = pos.getY();
        }
        return min;
    }

    /**
     * The highest Y level of any interior block.
     */
    public int getMaxY() {
        int max = Integer.MIN_VALUE;
        for (BlockPos pos : interiorBlocks) {
            if (pos.getY() > max) max = pos.getY();
        }
        return max == Integer.MIN_VALUE ? 0 : max;
    }

    /**
     * Height of the room (maxY - minY).
     */
    public int getHeight() {
        if (interiorBlocks.isEmpty()) return 0;
        return getMaxY() - getMinY();
    }

    /**
     * If room height is 1 block or less, expands upward by claiming air blocks above.
     * Supports intentional low-height designs (crawlspaces, tunnels) by giving them
     * at least 2 blocks of vertical space. Returns true if expansion occurred and was possible.
     * <p>
     * Raycasts upward from each floor block to verify at least 2 consecutive air blocks exist
     * before attempting expansion.
     */
    public boolean ensureMinimumHeight(ServerLevel level) {
        if (interiorBlocks.isEmpty()) return false;

        int minY = getMinY();

        // Raycast upward from each floor block to find expandable columns.
        // Only process blocks at floor level (minY) — upper rows are already at their intended height.
        // All-or-nothing among uncovered floor positions: if any is blocked, abort the whole expansion.
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        Set<BlockPos> toAdd = new HashSet<>();

        for (BlockPos pos : interiorBlocks) {
            if (pos.getY() != minY) continue; // Only check floor-level blocks

            BlockPos firstAbove = pos.above();

            // Already has interior coverage at this column — leave it alone
            if (interiorBlocks.contains(firstAbove)) continue;

            BlockPos secondAbove = firstAbove.above();

            boolean firstClear = level.getBlockState(firstAbove).isAir()
                    && registry.getRegionIdAt(firstAbove) == null;
            boolean secondClear = level.getBlockState(secondAbove).isAir()
                    && registry.getRegionIdAt(secondAbove) == null;

            if (firstClear && secondClear) {
                toAdd.add(firstAbove);
            } else {
                // A short position can't expand — abort to keep height uniform
                LOGGER.debug("[ZONE] {} unable to expand height — blocked at {}",
                        id.toString().substring(0, 8), pos.toShortString());
                return false;
            }
        }

        if (toAdd.isEmpty()) {
            return false;
        }

        // Add new blocks to interior
        for (BlockPos pos : toAdd) {
            interiorBlocks.add(pos);
            volume++;
            registry.mapBlockToRegion(pos, id);
            ChunkAccess chunk = level.getChunk(pos);
            ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
            data.setBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15, ClassificationState.INSIDE);
        }

        LOGGER.debug("[ZONE] {} expanded upward ({} air blocks claimed)",
                id.toString().substring(0, 8), toAdd.size());
        return true;
    }

    // ---- Grace period state ----

    public void enterGracePeriod(long expiresAt) {
        this.gracePeriodExpiresAt = expiresAt;
        LOGGER.debug("[ZONE] {} entered grace period (expires at tick {})", id.toString().substring(0, 8), expiresAt);
    }

    public void exitGracePeriod() {
        if (gracePeriodExpiresAt >= 0L) {
            LOGGER.debug("[ZONE] {} exited grace period", id.toString().substring(0, 8));
        }
        this.gracePeriodExpiresAt = -1L;
    }

    public boolean isInGracePeriod() {
        return gracePeriodExpiresAt >= 0L;
    }

    public long getGracePeriodExpiresAt() {
        return gracePeriodExpiresAt;
    }

    public boolean isGracePeriodExpired(long currentGameTime) {
        return isInGracePeriod() && gracePeriodExpiresAt <= currentGameTime;
    }

    // ---- Core blocks (base-level boundary blocks) & entry point ----

    public Set<BlockPos> getCoreBlocks() {
        return Collections.unmodifiableSet(coreBlocks);
    }

    public void addCoreBlock(BlockPos pos) {
        coreBlocks.add(pos);
    }

    public void addCoreBlocks(Set<BlockPos> blocks) {
        coreBlocks.addAll(blocks);
    }

    public boolean isCoreBlock(BlockPos pos) {
        return coreBlocks.contains(pos);
    }

    public boolean removeCoreBlock(BlockPos pos) {
        return coreBlocks.remove(pos);
    }

    @Nullable
    public BlockPos getMainEntryway() {
        return mainEntryway != null ? mainEntryway.primaryBlock() : null;
    }

    @Nullable
    public ZoneEntry getMainEntry() {
        return mainEntryway;
    }

    public void setMainEntryway(@Nullable BlockPos pos) {
        this.mainEntryway = pos != null
                ? new ZoneEntry(pos, Set.of(pos), EntryKind.UNKNOWN, EntryRole.UNKNOWN, 0)
                : null;
    }

    public void setMainEntry(@Nullable ZoneEntry entry) {
        this.mainEntryway = entry;
    }

    /**
     * Validates the current main entryway is still a valid entry block.
     * If not, searches for and assigns a new one from core blocks.
     * Returns true if a valid entry exists (current or newly found), false if none available.
     */
    public boolean validateAndUpdateMainEntry(ServerLevel level) {
        Set<BlockPos> allOwnedBlocks = getAllOwnedBlocks();

        // We already know where the entry is — just check if it's still there
        if (mainEntryway != null) {
            ZoneEntry refreshed = scoreEntryCandidate(mainEntryway.primaryBlock(), allOwnedBlocks, level);
            if (refreshed != null && refreshed.role() != EntryRole.INTERNAL_CONNECTOR) {
                mainEntryway = refreshed;
                return true;
            }
        }

        // Entry gone — search adjacents of core blocks for a replacement
        ZoneEntry newEntry = findPrimaryEntry(union(coreBlocks, ownedEntryBlocks), allOwnedBlocks, level);
        // Fall back to all interior blocks so internal connectors such as
        // central stairs/landings can act as the entryway.
        if (newEntry == null) {
            newEntry = findPrimaryEntry(allOwnedBlocks, allOwnedBlocks, level);
        }
        if (newEntry != null) {
            mainEntryway = newEntry;
            LOGGER.debug("[ZONE] {} reassigned main entryway to {} ({}, score={})",
                    id.toString().substring(0, 8), newEntry.primaryBlock().toShortString(),
                    newEntry.role(), newEntry.score());
            return true;
        }

        mainEntryway = null;
        return false;
    }

    // ---- Boundary row tracking for dynamic height expansion ----

    /**
     * Register boundary blocks organized by Y level. Call during zone evaluation.
     */
    public void updateBoundaryRows(ServerLevel level) {
        boundaryBlocksByY.clear();
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);

        for (BlockPos pos : interiorBlocks) {
            // Check if this block is on the boundary (touches outside air)
            // Consider 8-directional adjacency (including diagonals) for convex/concave walls
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(dir);
                if (!interiorBlocks.contains(neighbor) && level.getBlockState(neighbor).isAir()) {
                    boundaryBlocksByY.computeIfAbsent(pos.getY(), k -> new HashSet<>()).add(pos);
                    break;
                }
            }
            // Also check diagonals
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue; // Skip center
                    BlockPos diagonal = pos.offset(dx, 0, dz);
                    if (!interiorBlocks.contains(diagonal) && level.getBlockState(diagonal).isAir()) {
                        boundaryBlocksByY.computeIfAbsent(pos.getY(), k -> new HashSet<>()).add(pos);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Checks if the given Y level's boundary row is complete (no air gaps to outside).
     * Accounts for convex/concave walls by checking 8-directional adjacency (including diagonals).
     * If complete and space exists above, raycasts and expands. Returns true if expansion occurred.
     * Only fires when the zone is still at minimum height — not on subsequent row breaks/repairs.
     */
    public boolean tryExpandFromCompletedRow(int y, ServerLevel level) {
        if (getHeight() > 1) return false; // Already at minimum height, don't expand further

        Set<BlockPos> rowBlocks = boundaryBlocksByY.get(y);
        if (rowBlocks == null || rowBlocks.isEmpty()) return false;

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);

        // Check if row is complete — all blocks have no air neighbors outside (8-directional)
        for (BlockPos pos : rowBlocks) {
            // Check 4-directional orthogonal neighbors
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(dir);
                if (!interiorBlocks.contains(neighbor) && level.getBlockState(neighbor).isAir()) {
                    return false; // Row has air gaps, not complete
                }
            }
            // Check 4 diagonal neighbors
            for (int dx = -1; dx <= 1; dx += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    BlockPos diagonal = pos.offset(dx, 0, dz);
                    if (!interiorBlocks.contains(diagonal) && level.getBlockState(diagonal).isAir()) {
                        return false; // Diagonal gap, not complete
                    }
                }
            }
        }

        // Row is complete — raycast upward from each position.
        // Validate 2 clear air blocks exist but only claim the first (one level up).
        Set<BlockPos> toAdd = new HashSet<>();
        for (BlockPos pos : rowBlocks) {
            BlockPos firstAbove = pos.above();
            BlockPos secondAbove = firstAbove.above();

            boolean firstClear = level.getBlockState(firstAbove).isAir()
                    && !interiorBlocks.contains(firstAbove)
                    && registry.getRegionIdAt(firstAbove) == null;
            boolean secondClear = level.getBlockState(secondAbove).isAir()
                    && !interiorBlocks.contains(secondAbove)
                    && registry.getRegionIdAt(secondAbove) == null;

            if (firstClear && secondClear) {
                toAdd.add(firstAbove); // Only claim one level up
            }
        }

        if (toAdd.isEmpty()) return false;

        // Add new blocks to interior
        for (BlockPos pos : toAdd) {
            interiorBlocks.add(pos);
            volume++;
            registry.mapBlockToRegion(pos, id);
            ChunkAccess chunk = level.getChunk(pos);
            ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
            data.setBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15, ClassificationState.INSIDE);
        }

        LOGGER.debug("[ZONE] {} expanded height from completed row at y={} ({} blocks claimed)",
                id.toString().substring(0, 8), y, toAdd.size());
        return true;
    }

    /**
     * Checks if all highest interior blocks are directly capped by solid ceiling blocks,
     * then validates interior is fully sealed via flood-fill (no air gaps to outside).
     * If sealed, marks zone as sealed and claims ceiling blocks. Returns true if sealed.
     */
    public boolean checkAndSealCeiling(ServerLevel level) {
        if (sealed || interiorBlocks.isEmpty()) return false;

        int maxY = getMaxY();
        Set<BlockPos> topBlocks = new HashSet<>();

        // Collect all blocks at the highest Y level
        for (BlockPos pos : interiorBlocks) {
            if (pos.getY() == maxY) {
                topBlocks.add(pos);
            }
        }

        if (topBlocks.isEmpty()) return false;

        // Check if all top blocks have solid ceiling directly above
        Set<BlockPos> ceilingBlocks = new HashSet<>();
        for (BlockPos pos : topBlocks) {
            BlockPos above = pos.above();
            if (!level.getBlockState(above).isAir()) {
                ceilingBlocks.add(above);
            } else if (isInternalConnectorOpening(pos, above, Direction.UP,
                    p -> isConnectorBlock(level.getBlockState(p)))) {
                continue;
            } else {
                return false; // Found an open top, not sealed
            }
        }

        // Ceiling looks complete — validate interior is fully sealed via flood-fill
        if (!isInteriorFullySealed(level)) {
            LOGGER.debug("[ZONE] {} ceiling exists but interior has air gaps to outside", id.toString().substring(0, 8));
            return false;
        }

        // Interior is sealed — mark as sealed.
        // Ceiling blocks are tracked via the sealed flag and isInteriorFullySealed();
        // they are NOT mapped into SpaceRegionRegistry because they are solid blocks,
        // not interior air.
        sealed = true;

        LOGGER.debug("[ZONE] {} sealed with ceiling ({} ceiling blocks)",
                id.toString().substring(0, 8), ceilingBlocks.size());
        return true;
    }

    /**
     * Flood-fills from interior blocks to verify no air reaches outside the zone.
     * Returns true if interior is fully contained (sealed), false if air leaks out.
     */
    private boolean isInteriorFullySealed(ServerLevel level) {
        if (interiorBlocks.isEmpty()) return false;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        // Start from an arbitrary interior block
        BlockPos seed = interiorBlocks.iterator().next();
        queue.add(seed);
        visited.add(seed);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();

            // Check all 6 neighbors for air that leaks outside
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);

                if (visited.contains(neighbor)) continue;

                // If neighbor is solid, stop
                if (!level.getBlockState(neighbor).isAir()) continue;

                // If neighbor is in this zone's interior, continue flood-fill
                if (interiorBlocks.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    continue;
                }

                if (isInternalConnectorOpening(pos, neighbor, dir,
                        p -> isConnectorBlock(level.getBlockState(p)))) {
                    continue;
                }

                // Neighbor is air but NOT in this zone and not an internal connector — air leaked outside!
                return false;
            }
        }

        // Flood-fill completed without finding any outside leaks
        return true;
    }

    public boolean isSealed() {
        return sealed;
    }

    /**
     * Identifies all core blocks: base-level boundary blocks touching outside air (excluding floor).
     */
    public static Set<BlockPos> identifyCoreBlocks(Set<BlockPos> interiorBlocks, ServerLevel level) {
        if (interiorBlocks.isEmpty()) return Set.of();

        Set<BlockPos> coreBlocks = new HashSet<>();
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BlockPos pos : interiorBlocks) {
            int py = pos.getY();
            if (py < minY) minY = py;
            if (py > maxY) maxY = py;
        }

        // For 1-high rooms, include minY blocks as core blocks so the entry door
        // can be found. For taller rooms, skip the floor level.
        boolean oneHigh = (maxY == minY);

        for (BlockPos pos : interiorBlocks) {
            if (!oneHigh && pos.getY() == minY) continue;

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (!interiorBlocks.contains(neighbor) && level.getBlockState(neighbor).isAir()) {
                    coreBlocks.add(pos);
                    break;
                }
            }
        }

        return coreBlocks;
    }

    /**
     * Finds the primary entry point — an owned or adjacent entry block near any core block.
     */
    @Nullable
    public static BlockPos findPrimaryEntry(Set<BlockPos> coreBlocks, ServerLevel level) {
        ZoneEntry entry = findPrimaryEntry(coreBlocks, coreBlocks, level);
        return entry != null ? entry.primaryBlock() : null;
    }

    @Nullable
    private static ZoneEntry findPrimaryEntry(Set<BlockPos> candidates,
                                              Set<BlockPos> zoneBlocks,
                                              ServerLevel level) {
        return findPrimaryEntry(candidates, zoneBlocks, level, null);
    }

    @Nullable
    private static ZoneEntry findPrimaryEntry(Set<BlockPos> candidates,
                                              Set<BlockPos> zoneBlocks,
                                              ServerLevel level,
                                              @Nullable BlockPos excludedEntryBlock) {
        ZoneEntry best = null;
        Set<BlockPos> checked = new HashSet<>();
        for (BlockPos pos : candidates) {
            best = chooseBetterEntry(best, scoreEntryCandidate(pos, zoneBlocks, level), checked, excludedEntryBlock);
            for (Direction dir : Direction.values()) {
                best = chooseBetterEntry(best, scoreEntryCandidate(pos.relative(dir), zoneBlocks, level), checked, excludedEntryBlock);
            }
        }
        return best;
    }

    @Nullable
    private static ZoneEntry chooseBetterEntry(@Nullable ZoneEntry best,
                                               @Nullable ZoneEntry candidate,
                                               Set<BlockPos> checked,
                                               @Nullable BlockPos excludedEntryBlock) {
        if (candidate == null) return best;
        if (excludedEntryBlock != null && candidate.blocks().contains(excludedEntryBlock)) return best;
        if (!checked.add(candidate.primaryBlock())) return best;
        if (candidate.role() == EntryRole.INTERNAL_CONNECTOR) return best;
        if (best == null || candidate.score() > best.score()) return candidate;
        return best;
    }

    @Nullable
    private static ZoneEntry scoreEntryCandidate(BlockPos entry,
                                                 Set<BlockPos> zoneBlocks,
                                                 ServerLevel level) {
        var state = level.getBlockState(entry);
        EntryKind kind = EntryKind.fromState(state);
        if (kind == EntryKind.UNKNOWN) return null;

        Set<BlockPos> blocks = collectEntryStructure(entry, kind, level);
        EntryRole role = classifyEntryRole(blocks, zoneBlocks, level, kind);
        int score = kind.baseScore + role.scoreBonus;
        if (zoneBlocks.contains(entry)) score -= 25;
        return new ZoneEntry(entry, blocks, kind, role, score);
    }

    private static Set<BlockPos> collectEntryStructure(BlockPos entry, EntryKind kind, ServerLevel level) {
        Set<BlockPos> blocks = new HashSet<>();
        blocks.add(entry);
        if (kind == EntryKind.DOOR) {
            if (level.getBlockState(entry.above()).is(BlockTags.DOORS)) blocks.add(entry.above());
            if (level.getBlockState(entry.below()).is(BlockTags.DOORS)) blocks.add(entry.below());
        }
        return blocks;
    }

    private static EntryRole classifyEntryRole(Set<BlockPos> entryBlocks,
                                               Set<BlockPos> zoneBlocks,
                                               ServerLevel level,
                                               EntryKind kind) {
        if (kind == EntryKind.DOOR) return EntryRole.EXTERNAL_ENTRY;

        boolean owned = entryBlocks.stream().anyMatch(zoneBlocks::contains);
        boolean touchesOtherZone = false;
        boolean touchesNonZoneAir = false;
        boolean verticalTransition = false;
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);

        for (BlockPos entry : entryBlocks) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = entry.relative(dir);
                if (zoneBlocks.contains(neighbor)) continue;

                UUID neighborZone = registry.getRegionIdAt(neighbor);
                if (neighborZone != null) {
                    touchesOtherZone = true;
                    if (dir.getAxis() == Direction.Axis.Y) verticalTransition = true;
                } else if (level.getBlockState(neighbor).isAir()) {
                    touchesNonZoneAir = true;
                    if (dir.getAxis() == Direction.Axis.Y) verticalTransition = true;
                }
            }
        }

        if (touchesOtherZone) return verticalTransition ? EntryRole.VERTICAL_CONNECTOR : EntryRole.ROOM_TO_ROOM;
        if (touchesNonZoneAir) return verticalTransition ? EntryRole.VERTICAL_CONNECTOR : EntryRole.EXTERNAL_ENTRY;
        if (owned) return EntryRole.INTERNAL_CONNECTOR;
        return EntryRole.EXTERNAL_ENTRY;
    }

    /*
     * Compatibility overload for older callers/tests that only know candidate blocks.
     * Without the full zone block set, owned connectors cannot be distinguished from
     * internal connectors, so this uses the candidate set as the best available shape.
     */
    @Nullable
    private static ZoneEntry findPrimaryEntryCompat(Set<BlockPos> coreBlocks, ServerLevel level) {
        return findPrimaryEntry(coreBlocks, coreBlocks, level);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Zone zone = (Zone) o;
        return id.equals(zone.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // ---- Record types ----

    public record ExpansionResult(Set<UUID> mergedZones, int blocksClaimed, boolean deferred) {
        public static final ExpansionResult EMPTY = new ExpansionResult(Set.of(), 0, false);
        /**
         * Sent when expansion found nothing but didn't leak — multi-block structure
         * where the other half is still solid. Caller should retry one tick later.
         */
        public static final ExpansionResult DEFER = new ExpansionResult(Set.of(), 0, true);
    }

    public record ZoneEntry(BlockPos primaryBlock,
                            Set<BlockPos> blocks,
                            EntryKind kind,
                            EntryRole role,
                            int score) {
        public ZoneEntry {
            blocks = Set.copyOf(blocks);
        }
    }

    public enum EntryKind {
        DOOR(1000),
        STAIR(700),
        TRAPDOOR(600),
        SLAB(500),
        UNKNOWN(0);

        private final int baseScore;

        EntryKind(int baseScore) {
            this.baseScore = baseScore;
        }

        static EntryKind fromState(net.minecraft.world.level.block.state.BlockState state) {
            if (state.is(BlockTags.DOORS)) return DOOR;
            if (state.is(BlockTags.STAIRS)) return STAIR;
            if (state.is(BlockTags.TRAPDOORS)) return TRAPDOOR;
            if (state.is(BlockTags.SLABS)) return SLAB;
            return UNKNOWN;
        }
    }

    public enum EntryRole {
        EXTERNAL_ENTRY(300),
        ROOM_TO_ROOM(250),
        VERTICAL_CONNECTOR(225),
        INTERNAL_CONNECTOR(-1000),
        UNKNOWN(0);

        private final int scoreBonus;

        EntryRole(int scoreBonus) {
            this.scoreBonus = scoreBonus;
        }
    }
}
