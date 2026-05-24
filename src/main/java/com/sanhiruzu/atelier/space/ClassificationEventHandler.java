package com.sanhiruzu.atelier.space;

import com.sanhiruzu.atelier.network.SyncChunkClassificationPayload;
import com.sanhiruzu.atelier.space.zone.Zone;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

public class ClassificationEventHandler {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        ChunkClassificationData data = ChunkClassificationAttachment.get(event.getChunk());
        data.setDirty(true);

        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ClassificationScheduler scheduler = ClassificationTickHandler.getScheduler(serverLevel);
            ChunkPos pos = event.getChunk().getPos();
            if (scheduler.isNearAnyPlayer(pos.x, pos.z)) {
                scheduler.scheduleChunk(pos.x, pos.z);
            } else {
                scheduler.deferChunk(pos.x, pos.z);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ServerPlayer player = event.getPlayer();
        ChunkPos pos = event.getPos();
        ServerLevel level = event.getLevel();

        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) return;

        ZoneRegistry zoneRegistry = ZoneRegistry.get(level);

        ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
        PacketDistributor.sendToPlayer(player, new SyncChunkClassificationPayload(pos.x, pos.z, data));

        // Resend cached zone data for any zones whose spatial extent overlaps this chunk.
        // This ensures players receive zone data for zones that were restored at startup
        // before the player connected (evaluateRoomAndSync sent to nobody at that point).
        int chunkMinX = pos.getMinBlockX(), chunkMaxX = pos.getMaxBlockX();
        int chunkMinZ = pos.getMinBlockZ(), chunkMaxZ = pos.getMaxBlockZ();
        for (UUID zoneId : zoneRegistry.getAllRoomIds()) {
            ZoneData zoneData = zoneRegistry.getRoom(zoneId);
            if (zoneData != null && zoneData.hasSpatialExtent()
                    && zoneData.getMaxX() >= chunkMinX && zoneData.getMinX() <= chunkMaxX
                    && zoneData.getMaxZ() >= chunkMinZ && zoneData.getMinZ() <= chunkMaxZ) {
                zoneRegistry.sendRoomTo(player, zoneData);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        // ChunkClassificationData is persisted via NBT attachment automatically.
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor.isClientSide()) return;
        if (levelAccessor instanceof Level level) {
            ZoneRegistry.get(level).invalidateAll();
            SpaceRegionRegistry.get(level).clear(); // prevent stale block mappings on reload
        }
        if (levelAccessor instanceof ServerLevel serverLevel) {
            ClassificationTickHandler.removeScheduler(serverLevel);
        }
    }

    // ==========================================================================
    // Block break → zone expansion
    // ==========================================================================

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = event.getPos();
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor.isClientSide()) return;
        if (!(levelAccessor instanceof ServerLevel serverLevel)) return;
        markChangedChunkForZoneRecheck(serverLevel, pos);

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(serverLevel);
        ZoneRegistry zoneRegistry = ZoneRegistry.get(serverLevel);

        UUID zoneId = findAdjacentZoneId(registry, pos);
        if (zoneId != null) {
            Zone zone = zoneRegistry.getZoneById(zoneId);
            if (zone != null && !zone.isDissolved()) {
                // Breaking a core block (door or critical entry block) always triggers grace period.
                boolean isCoreBlock = zone.isCoreBlock(pos);

                // Check if breaking this block breaches the minimum enclosure:
                // any block at the lowest interior Y level (floor+1) that is
                // adjacent to the zone interior. A minimum room is 1-high walls
                // + door, so removing any block at that level deletes the room.
                boolean breachesMinimumEnclosure = pos.getY() == zone.getMinY();

                // Expand into the newly exposed position. tryExpand no longer requires
                // the block to already be air — BreakEvent fires before removal.
                Zone.ExpansionResult result = zone.tryExpand(pos, serverLevel);

                // Multi-block structure (e.g. bed): BreakEvent fires before Minecraft
                // removes the other half, so the BFS only found one block. Retry
                // one tick later when both halves are air.
                if (result == Zone.ExpansionResult.DEFER) {
                    zoneRegistry.scheduleDeferredExpansion(zone, pos);
                }

                // If expansion absorbed another zone, the combined zone is larger — fragility
                // checks computed before tryExpand (isCoreBlock, breachesMinimumEnclosure) used
                // stale geometry and must be skipped. Evaluate directly; the eval decides grace period.
                boolean mergeOccurred = !result.mergedZones().isEmpty();

                if (mergeOccurred) {
                    zoneRegistry.evaluateRoomAndSync(zone, serverLevel);
                } else if (zoneRegistry.isInGracePeriod(zone.getId())) {
                    // Already in grace period — any further break accelerates the timer.
                    zoneRegistry.enterGracePeriod(zone.getId(), serverLevel);
                } else if (isCoreBlock) {
                    zoneRegistry.enterGracePeriod(zone.getId(), serverLevel);
                } else if (breachesMinimumEnclosure) {
                    zoneRegistry.enterGracePeriod(zone.getId(), serverLevel);
                } else if (!zone.hasLiveEntry(serverLevel, pos)) {
                    zoneRegistry.enterGracePeriod(zone.getId(), serverLevel);
                } else {
                    zoneRegistry.evaluateRoomAndSync(zone, serverLevel);
                }
            }
        }
    }

    // ==========================================================================
    // Block place → zone shrinking / new enclosure detection
    // ==========================================================================

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        BlockPos pos = event.getPos();
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor.isClientSide()) return;
        if (!(levelAccessor instanceof ServerLevel serverLevel)) return;
        markChangedChunkForZoneRecheck(serverLevel, pos);

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(serverLevel);
        ZoneRegistry zoneRegistry = ZoneRegistry.get(serverLevel);

        // Check if placed inside an existing zone
        UUID zoneId = registry.getRegionIdAt(pos);
        if (zoneId != null) {
            Zone zone = zoneRegistry.getZoneById(zoneId);
            if (zone != null && !zone.isDissolved()) {
                // Zone shrinks — may split
                List<Zone> newZones = zone.tryShrink(pos, serverLevel);
                if (!zone.isDissolved()) {
                    zoneRegistry.evaluateRoomAndSync(zone, serverLevel);
                }
                for (Zone newZone : newZones) {
                    zoneRegistry.evaluateRoomAndSync(newZone, serverLevel);
                }

                // If placed block is an entry type adjacent to interior,
                // it might create a new entry point for an existing enclosure
                if (isEntryBlock(serverLevel.getBlockState(pos))) {
                    for (Direction dir : Direction.values()) {
                        BlockPos neighbor = pos.relative(dir);
                        UUID neighborZoneId = registry.getRegionIdAt(neighbor);
                        if (neighborZoneId != null && !neighborZoneId.equals(zoneId)) {
                            Zone neighborZone = zoneRegistry.getZoneById(neighborZoneId);
                            if (neighborZone != null && !neighborZone.isDissolved()) {
                                zoneRegistry.evaluateRoomAndSync(neighborZone, serverLevel);
                            }
                        }
                    }
                }
                return;
            }
        }

        // Not inside any zone — check if this placement completes a new enclosure.
        // EnclosureDetector runs a 3D bounded flood-fill from each air neighbor;
        // a non-null return means the air is fully bounded and can become a zone.
        if (!serverLevel.getBlockState(pos).isAir()) {
            UUID newZoneId = null;
            Set<BlockPos> enclosed = EnclosureDetector.detect(serverLevel, pos);
            if (enclosed != null) {
                // If the enclosed area is adjacent to an existing zone, expand that zone
                // into the new space rather than creating a separate one.  This handles
                // room extensions: player breaks a wall, builds new outer walls → the
                // existing zone grows to include the extension via its existing entry.
                // Grace-period zones can be recovered this way — expansion + evaluateRoomAndSync
                // will remove them from grace period.
                Zone adjacentZone = findAdjacentZoneForEnclosure(enclosed, registry, zoneRegistry);
                if (adjacentZone != null && !adjacentZone.isDissolved()) {
                    // Pick any seed in the enclosed set and let tryExpand BFS claim them all.
                    BlockPos seed = enclosed.iterator().next();
                    adjacentZone.tryExpand(seed, serverLevel);
                    zoneRegistry.evaluateRoomAndSync(adjacentZone, serverLevel);
                } else if (!isAdjacentToGracePeriodZone(enclosed, registry, zoneRegistry)) {
                    // Truly new enclosure — create a fresh zone.
                    if (isValidMinimalZone(enclosed, serverLevel)) {
                        int openingArea = Zone.computeOpeningAreaFor(enclosed, serverLevel);
                        Zone zone = Zone.createFromBootstrap(UUID.randomUUID(), enclosed, openingArea, serverLevel);
                        if (zone != null) {
                            newZoneId = zone.getId();
                            zoneRegistry.evaluateRoomAndSync(zone, serverLevel);
                        }
                    }
                }
            }

            // If an entry block was placed adjacent to a grace-period zone (e.g. door in a
            // different wall position), that may recover the zone without triggering
            // EnclosureDetector (which skips already-mapped interior blocks).
            if (isEntryBlock(serverLevel.getBlockState(pos))) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = pos.relative(dir);
                    UUID adjId = registry.getRegionIdAt(neighbor);
                    if (adjId != null && zoneRegistry.isInGracePeriod(adjId)) {
                        Zone adjZone = zoneRegistry.getZoneById(adjId);
                        if (adjZone != null && !adjZone.isDissolved()) {
                            zoneRegistry.evaluateRoomAndSync(adjZone, serverLevel);
                        }
                    }
                }
            }

            // A solid block placed adjacent to an existing zone may have closed off
            // previously-open faces (e.g. replacing a wall that was just broken).
            // Refresh the opening area so the SpaceRegion reflects the new geometry,
            // then re-evaluate so quality/enclosure score is immediately corrected.
            // Also check if this block completes a boundary row, enabling height expansion.
            Set<UUID> adjacentIds = new HashSet<>();
            for (Direction dir : Direction.values()) {
                UUID adjId = registry.getRegionIdAt(pos.relative(dir));
                if (adjId != null) adjacentIds.add(adjId);
            }
            for (UUID adjId : adjacentIds) {
                if (adjId.equals(newZoneId)) continue; // already evaluated at creation
                Zone adjZone = zoneRegistry.getZoneById(adjId);
                if (adjZone != null && !adjZone.isDissolved()) {
                    adjZone.refreshOpeningArea(serverLevel);
                    // Check if placement completed a boundary row at this Y level
                    adjZone.tryExpandFromCompletedRow(pos.getY(), serverLevel);
                    zoneRegistry.evaluateRoomAndSync(adjZone, serverLevel);
                }
            }
        }
    }

    // ==========================================================================
    // Explosion → zone expansion from destroyed blocks
    // ==========================================================================

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor.isClientSide()) return;
        if (!(levelAccessor instanceof ServerLevel serverLevel)) return;

        SpaceRegionRegistry registry = SpaceRegionRegistry.get(serverLevel);
        ZoneRegistry zoneRegistry = ZoneRegistry.get(serverLevel);
        Set<UUID> affectedZones = new HashSet<>();

        for (BlockPos pos : event.getAffectedBlocks()) {
            markChangedChunkForZoneRecheck(serverLevel, pos);
            UUID zoneId = registry.getRegionIdAt(pos);
            if (zoneId != null) {
                affectedZones.add(zoneId);
            }
            // Also check adjacent zones (explosion may expose new air)
            UUID adjId = findAdjacentZoneId(registry, pos);
            if (adjId != null) {
                affectedZones.add(adjId);
            }
        }

        for (UUID zoneId : affectedZones) {
            Zone zone = zoneRegistry.getZoneById(zoneId);
            if (zone == null || zone.isDissolved()) continue;

            // Try expansion from each exploded air position
            for (BlockPos pos : event.getAffectedBlocks()) {
                if (serverLevel.getBlockState(pos).isAir()) {
                    zone.tryExpand(pos, serverLevel);
                }
            }

            if (!zone.hasLiveEntry(serverLevel)) {
                zoneRegistry.enterGracePeriod(zone.getId(), serverLevel);
            } else {
                zoneRegistry.evaluateRoomAndSync(zone, serverLevel);
            }
        }
    }

    // ==========================================================================
    // Helpers
    // ==========================================================================

    private static void markChangedChunkForZoneRecheck(ServerLevel level, BlockPos pos) {
        ZoneRegistry zoneRegistry = ZoneRegistry.get(level);
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        markChunkForZoneRecheck(level, zoneRegistry, chunkX, chunkZ);

        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;
        if (localX == 0) markChunkForZoneRecheck(level, zoneRegistry, chunkX - 1, chunkZ);
        if (localX == 15) markChunkForZoneRecheck(level, zoneRegistry, chunkX + 1, chunkZ);
        if (localZ == 0) markChunkForZoneRecheck(level, zoneRegistry, chunkX, chunkZ - 1);
        if (localZ == 15) markChunkForZoneRecheck(level, zoneRegistry, chunkX, chunkZ + 1);
    }

    private static void markChunkForZoneRecheck(ServerLevel level,
                                                ZoneRegistry zoneRegistry,
                                                int chunkX,
                                                int chunkZ) {
        if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) return;
        zoneRegistry.markChunkForZoneRecheck(chunkX, chunkZ, level);
    }

    /**
     * Returns an existing Zone whose interior is directly adjacent to any block in
     * {@code enclosed}, or {@code null} if no such zone exists.
     *
     * <p>Used when a placed block completes a bounded enclosure that is contiguous
     * with an already-validated zone.  In that case we expand the existing zone
     * rather than creating a separate one — preserving the entry point and identity.</p>
     */
    @Nullable
    private static Zone findAdjacentZoneForEnclosure(Set<BlockPos> enclosed,
                                                     SpaceRegionRegistry registry,
                                                     ZoneRegistry zoneRegistry) {
        for (BlockPos pos : enclosed) {
            for (Direction dir : Direction.values()) {
                UUID adjId = registry.getRegionIdAt(pos.relative(dir));
                if (adjId == null) continue;
                Zone adj = zoneRegistry.getZoneById(adjId);
                if (adj != null && !adj.isDissolved()) return adj;
            }
        }
        return null;
    }

    private static UUID findAdjacentZoneId(SpaceRegionRegistry registry, BlockPos pos) {
        UUID found = null;
        for (BlockPos neighbor : getNeighbors(pos)) {
            UUID id = registry.getRegionIdAt(neighbor);
            if (id == null) continue;
            if (found == null) {
                found = id;
            } else if (!found.equals(id)) {
                // Block sits between two zones. Return the first one found — tryExpand
                // will encounter the second zone during BFS and merge it in naturally.
                return found;
            }
        }
        return found;
    }

    private static List<BlockPos> getNeighbors(BlockPos pos) {
        return Arrays.asList(
                pos.above(), pos.below(),
                pos.north(), pos.south(),
                pos.east(), pos.west()
        );
    }

    private static boolean isEntryBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.DOORS)
                || state.is(net.minecraft.tags.BlockTags.TRAPDOORS)
                || state.is(net.minecraft.tags.BlockTags.STAIRS)
                || state.is(net.minecraft.tags.BlockTags.SLABS);
    }

    static final int MIN_ZONE_VOLUME = 4;

    private static boolean isAdjacentToGracePeriodZone(Set<BlockPos> enclosed,
                                                       SpaceRegionRegistry registry,
                                                       ZoneRegistry zoneRegistry) {
        for (BlockPos pos : enclosed) {
            for (Direction dir : Direction.values()) {
                UUID adjId = registry.getRegionIdAt(pos.relative(dir));
                if (adjId != null && zoneRegistry.isInGracePeriod(adjId)) return true;
            }
        }
        return false;
    }

    static boolean isValidMinimalZone(Set<BlockPos> enclosed, ServerLevel level) {
        return enclosed.size() >= MIN_ZONE_VOLUME
                && hasAdjacentEntry(enclosed, level)
                && hasUnbrokenPerimeter(enclosed, level);
    }

    /**
     * At least one block adjacent (any face) to the enclosure must be a door/trapdoor/stair/slab.
     */
    private static boolean hasAdjacentEntry(Set<BlockPos> enclosed, ServerLevel level) {
        for (BlockPos pos : enclosed) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (enclosed.contains(neighbor)) continue;
                var state = level.getBlockState(neighbor);
                if (!state.isAir() && isEntryBlock(state)) return true;
            }
        }
        return false;
    }

    /**
     * Every horizontal neighbor of every interior block that lies outside the enclosure must be non-air.
     */
    private static boolean hasUnbrokenPerimeter(Set<BlockPos> enclosed, ServerLevel level) {
        for (BlockPos pos : enclosed) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(dir);
                if (!enclosed.contains(neighbor) && level.getBlockState(neighbor).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }
}
