package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.network.*;
import com.sanhiruzu.atelier.space.*;
import com.sanhiruzu.atelier.space.settlement.SettlementDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

public class ZoneRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("ZenAtelier/Zones");
    private static final long GRACE_PERIOD_TICKS = 1200L; // 60 seconds at 20 ticks/s
    private static final long GRACE_REDUCTION_PER_BREAK = 200L; // 10 seconds shaved per additional block break during grace period
    private static final long DIRTY_ZONE_RECHECK_DELAY_TICKS = 40L;
    private static final Map<String, ZoneRegistry> INSTANCES = new HashMap<>();

    // Player-facing interpretation of zones. Raw geometry/lifecycle lives in zoneEntities.
    private final Map<UUID, ZoneData> cache = new HashMap<>();
    private final Map<UUID, Zone> zoneEntities = new HashMap<>();
    final Map<UUID, String> customNames = new HashMap<>();
    @Nullable
    private ZoneSavedData savedData;
    // zoneId → game tick at which the zone should be fully dissolved if not recovered
    private final Map<UUID, Long> gracePeriodExpiry = new HashMap<>();
    // chunkKey → (zoneId → blocks in that chunk): populated during zone restoration so
    // bootstrapChunk can pre-mark INSIDE before the classifier clears the bitfield.
    private final Map<Long, Map<UUID, Set<BlockPos>>> restoredByChunk = new HashMap<>();
    private final Set<UUID> restoredZoneIds = new HashSet<>();
    // Deferred expansions for multi-block structures (beds, etc.) where BreakEvent
    // fires before Minecraft removes all blocks. Processed on the next tick.
    private final List<DeferredExpansion> deferredExpansions = new ArrayList<>();
    private final Map<Long, Long> dirtyZoneRecheckChunks = new HashMap<>();
    private final RoomChangeScheduler roomChangeScheduler = new RoomChangeScheduler();
    private boolean restored = false;

    public static ZoneRegistry get(Level level) {
        String key = level.dimension().location().toString();
        ZoneRegistry registry = INSTANCES.computeIfAbsent(key, k -> new ZoneRegistry());
        if (registry.savedData == null && level instanceof ServerLevel serverLevel) {
            registry.savedData = ZoneSavedData.get(serverLevel);
            if (!registry.restored) {
                registry.restored = true;
                registry.restoreZonesFromSavedData(serverLevel);
            }
        }
        return registry;
    }

    /**
     * Test-only: creates a fresh registry keyed by {@code key}, bypassing the Level lookup.
     */
    public static ZoneRegistry createForTest(String key) {
        ZoneRegistry fresh = new ZoneRegistry();
        INSTANCES.put(key, fresh);
        return fresh;
    }

    /**
     * Test-only: inserts a ZoneData entry directly into the cache.
     */
    void putForTest(UUID regionId, ZoneData data) {
        cache.put(regionId, data);
    }

    @Nullable
    public ZoneData getRoom(UUID regionId) {
        return cache.get(regionId);
    }

    /**
     * @deprecated Use {@link #getRoom(UUID)} for player-facing room data.
     */
    @Deprecated
    @Nullable
    public ZoneData getZone(UUID regionId) {
        return getRoom(regionId);
    }

    @Nullable
    public ZoneData getOrEvaluateRoom(UUID regionId, Level level) {
        ZoneData cached = cache.get(regionId);
        if (cached != null && !cached.isDisabled()) return cached;
        ZoneData result = evaluate(regionId, level);
        if (result != null) {
            if (cached != null && cached.isInitialized() && cached.hasSpatialExtent()) {
                result.setSpatialExtent(cached.getMinX(), cached.getMinY(), cached.getMinZ(),
                        cached.getMaxX(), cached.getMaxY(), cached.getMaxZ());
            }
            result.setInitialized(true);
            cache.put(regionId, result);
            if (cached != null && cached.isDisabled()) {
                result.setDisabled(false, -1L);
                transferMetadata(regionId, regionId);
            }
        }
        return result;
    }

    /**
     * @deprecated Use {@link #getOrEvaluateRoom(UUID, Level)} for player-facing room data.
     */
    @Deprecated
    @Nullable
    public ZoneData getOrEvaluate(UUID regionId, Level level) {
        return getOrEvaluateRoom(regionId, level);
    }

    // ---- Zone entity management ----

    public void registerZone(Zone zone) {
        zoneEntities.put(zone.getId(), zone);
    }

    @Nullable
    public Zone getZoneById(UUID id) {
        return zoneEntities.get(id);
    }

    public Collection<Zone> getAllZones() {
        return zoneEntities.values();
    }

    public void dissolveZone(UUID id) {
        zoneEntities.remove(id);
        cache.remove(id);
        customNames.remove(id);
        restoredZoneIds.remove(id);
    }

    public void mergeZones(UUID survivor, UUID eliminated) {
        zoneEntities.remove(eliminated);
        cache.remove(eliminated);
        restoredZoneIds.remove(eliminated);
    }

    // ---- Grace period tracking ----

    public boolean isInGracePeriod(UUID zoneId) {
        Zone zone = zoneEntities.get(zoneId);
        return zone != null && zone.isInGracePeriod();
    }

    public void addToGracePeriod(UUID zoneId, long expiresAt) {
        Zone zone = zoneEntities.get(zoneId);
        if (zone != null) {
            zone.enterGracePeriod(expiresAt);
        }
        gracePeriodExpiry.put(zoneId, expiresAt);
    }

    public long removeFromGracePeriod(UUID zoneId) {
        Zone zone = zoneEntities.get(zoneId);
        if (zone != null) {
            zone.exitGracePeriod();
        }
        Long val = gracePeriodExpiry.remove(zoneId);
        return val != null ? val : 0L;
    }

    public long getGracePeriodExpiry(UUID zoneId) {
        return gracePeriodExpiry.getOrDefault(zoneId, 0L);
    }

    /**
     * Removes and returns all zone IDs whose grace period has expired.
     */
    public List<UUID> expireGracePeriods(long currentTime) {
        List<UUID> expired = new ArrayList<>();
        gracePeriodExpiry.entrySet().removeIf(e -> {
            if (e.getValue() <= currentTime) {
                expired.add(e.getKey());
                return true;
            }
            return false;
        });
        return expired;
    }

    public void disable(UUID regionId, long gameTime) {
        ZoneData zone = cache.get(regionId);
        if (zone != null) {
            zone.setDisabled(true, gameTime);
        }
    }

    public List<UUID> expireDisabledZones(long currentGameTime, long gracePeriodTicks) {
        List<UUID> expired = new ArrayList<>();
        cache.entrySet().removeIf(e -> {
            if (e.getValue().isExpired(currentGameTime, gracePeriodTicks)) {
                expired.add(e.getKey());
                customNames.remove(e.getKey());
                return true;
            }
            return false;
        });
        return expired;
    }

    // ---- Zone lifecycle (moved from ClassificationScheduler) ----

    /**
     * Called each tick by the scheduler. Expires any zones whose grace period
     * has elapsed and dissolves them.
     */
    public void expireDisabledZones(ServerLevel level) {
        List<UUID> expired = expireGracePeriods(level.getGameTime());
        for (UUID zoneId : expired) {
            dissolveGracePeriodZone(zoneId, level);
        }
    }

    /**
     * Dissolves a zone that is currently in the grace-period map.
     * Removes it from the grace period, calls {@link Zone#dissolve},
     * and broadcasts a remove packet with the spatial extent for client-side VFX.
     */
    private void dissolveGracePeriodZone(UUID zoneId, ServerLevel level) {
        removeFromGracePeriod(zoneId);
        ZoneData snapshot = getRoom(zoneId);

        Zone zone = getZoneById(zoneId);
        if (zone != null && !zone.isDissolved()) {
            zone.dissolve(level);
            LOGGER.debug("[ZONE] {} dissolved from grace period", zoneId.toString().substring(0, 8));
        }

        if (snapshot != null && snapshot.hasSpatialExtent()) {
            var payload = new RemoveZonePayload(
                    zoneId, snapshot.getMinX(), snapshot.getMinY(), snapshot.getMinZ(),
                    snapshot.getMaxX(), snapshot.getMaxY(), snapshot.getMaxZ()
            );
            for (ServerPlayer player : level.players()) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    /**
     * Enters a 60-second grace period instead of immediately dissolving the zone.
     * The zone's interior blocks remain mapped so UUID and name survive if the
     * player restores a valid entry before the timer expires.
     * <p>
     * If the zone is already in grace period, the remaining time is reduced by
     * {@link #GRACE_REDUCTION_PER_BREAK} per call so repeated block breaking
     * accelerates dissolution — this prevents a determined teardown from
     * leaving an orphaned zone.
     */
    public void enterGracePeriod(UUID zoneId, ServerLevel level) {
        // If the zone was never evaluated (no cached ZoneData), it was never a valid
        // player-facing zone — dissolve immediately.
        if (getRoom(zoneId) == null) {
            Zone zone = getZoneById(zoneId);
            if (zone != null && !zone.isDissolved()) {
                zone.dissolve(level);
            }
            return;
        }

        if (GRACE_PERIOD_TICKS == 0) {
            dissolveGracePeriodZone(zoneId, level);
            checkAllPlayerZones(level);
            return;
        }

        long currentTime = level.getGameTime();

        if (isInGracePeriod(zoneId)) {
            long currentExpiry = getGracePeriodExpiry(zoneId);
            long newExpiry = currentExpiry - GRACE_REDUCTION_PER_BREAK;

            if (newExpiry <= currentTime) {
                removeFromGracePeriod(zoneId);
                dissolveGracePeriodZone(zoneId, level);
            } else {
                addToGracePeriod(zoneId, newExpiry);
                int remainingTicks = (int) (newExpiry - currentTime);
                broadcastGracePeriod(zoneId, remainingTicks, level);
            }
            return;
        }

        long expiresAt = currentTime + GRACE_PERIOD_TICKS;
        addToGracePeriod(zoneId, expiresAt);
        disable(zoneId, currentTime);
        // Push the disabled zone data to clients before checkAllPlayerZones clears the
        // player's zone reference — the client needs context to display the grace period HUD.
        ZoneData disabledData = getRoom(zoneId);
        if (disabledData != null) sendRoomToPlayers(disabledData, level);
        broadcastGracePeriod(zoneId, (int) GRACE_PERIOD_TICKS, level);
        checkAllPlayerZones(level);
    }

    private void broadcastGracePeriod(UUID zoneId, int ticksRemaining, ServerLevel level) {
        var packet = new ZoneGracePeriodPayload(zoneId, ticksRemaining);
        for (Player player : level.players()) {
            if (player instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, packet);
            }
        }
    }

    // ---- Zone evaluation + sync ----

    public void queueRoomChange(Zone zone, ServerLevel level) {
        roomChangeScheduler.markDirty(zone, level, this);
    }

    public void tickRoomChanges(ServerLevel level) {
        roomChangeScheduler.tick(level, this);
    }

    public void markChunkForZoneRecheck(int chunkX, int chunkZ, ServerLevel level) {
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
        long readyAt = level.getGameTime() + DIRTY_ZONE_RECHECK_DELAY_TICKS;
        dirtyZoneRecheckChunks.put(chunkKey, readyAt);
    }

    public void processDirtyZoneRechecks(ServerLevel level) {
        if (dirtyZoneRecheckChunks.isEmpty()) return;

        long now = level.getGameTime();
        List<Long> readyChunks = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : dirtyZoneRecheckChunks.entrySet()) {
            if (entry.getValue() <= now) {
                readyChunks.add(entry.getKey());
            }
        }
        if (readyChunks.isEmpty()) return;

        for (long chunkKey : readyChunks) {
            dirtyZoneRecheckChunks.remove(chunkKey);
            net.minecraft.world.level.chunk.LevelChunk chunk =
                    level.getChunkSource().getChunkNow(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey));
            if (chunk == null) continue;

            recheckZonesTouchingChunk(chunk, level);
        }
    }

    public void evaluateRoomAndSyncNow(Zone zone, ServerLevel level) {
        // Ensure minimum playable height on every evaluation (expand/shrink may leave it at 1)
        zone.ensureMinimumHeight(level);

        // Update boundary row tracking for dynamic height expansion
        zone.updateBoundaryRows(level);

        // Check if ceiling is sealed and claim it
        zone.checkAndSealCeiling(level);

        // If in grace period, validate main entryway before recovery attempt
        boolean inGracePeriod = isInGracePeriod(zone.getId());
        if (inGracePeriod && !zone.validateAndUpdateMainEntry(level)) {
            LOGGER.debug("[ZONE] {} cannot recover — no valid main entryway", zone.getId().toString().substring(0, 8));
            // No valid entry found; let zone dissolve on grace period expiry or evaluate as-is
            checkAllPlayerZones(level);
            return;
        }

        ZoneData zoneData = reEvaluateRoom(zone.getId(), level);
        if (zoneData != null) {
            String zoneType = zoneData.isOutdoor() ? "Outdoor" : (zoneData instanceof RoomData ? "Room" : "Zone");
            boolean degraded = zoneData instanceof RoomData room && room.isDegraded();
            LOGGER.trace("[ZONE] {} as {} (vol={}, enc={}){}",
                    zoneData.getRegionId(), zoneType, zoneData.getVolume(),
                    String.format("%.2f", zoneData.getEnclosureScore()), degraded ? " [DEGRADED]" : "");
            if (inGracePeriod && removeFromGracePeriod(zone.getId()) != 0L) {
                LOGGER.debug("[ZONE] {} recovered from grace period", zone.getId().toString().substring(0, 8));
                broadcastGracePeriod(zone.getId(), 0, level); // 0 = cancelled
            }
            sendRoomToPlayers(zoneData, level);
        } else if (getRoom(zone.getId()) == null) {
            if (!zone.isDissolved()) {
                zone.dissolve(level);
            }
        }
        checkAllPlayerZones(level);
    }

    /**
     * @deprecated Use {@link #evaluateRoomAndSync(Zone, ServerLevel)}.
     */
    @Deprecated
    public void evaluateZoneAndSync(Zone zone, ServerLevel level) {
        evaluateRoomAndSyncNow(zone, level);
    }

    /**
     * Queues player-facing room interpretation. Use {@link #evaluateRoomAndSyncNow} only for initial load or debug bypasses.
     */
    public void evaluateRoomAndSync(Zone zone, ServerLevel level) {
        if (isInGracePeriod(zone.getId()) || getRoom(zone.getId()) == null) {
            evaluateRoomAndSyncNow(zone, level);
            return;
        }
        queueRoomChange(zone, level);
    }

    public void sendRoomToPlayers(ZoneData zoneData, ServerLevel level) {
        SyncZoneGridPayload payload = buildZonePayload(zoneData);
        for (Player player : level.players()) {
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, payload);
            }
        }
    }

    /**
     * @deprecated Use {@link #sendRoomToPlayers(ZoneData, ServerLevel)}.
     */
    @Deprecated
    public void sendZoneToPlayers(ZoneData zoneData, ServerLevel level) {
        sendRoomToPlayers(zoneData, level);
    }

    public void sendRoomTo(ServerPlayer player, ZoneData zoneData) {
        PacketDistributor.sendToPlayer(player, buildZonePayload(zoneData));
    }

    /**
     * @deprecated Use {@link #sendRoomTo(ServerPlayer, ZoneData)}.
     */
    @Deprecated
    public void sendZoneTo(ServerPlayer player, ZoneData zoneData) {
        sendRoomTo(player, zoneData);
    }

    private SyncZoneGridPayload buildZonePayload(ZoneData zoneData) {
        float quality = zoneData instanceof RoomData room ? room.getQuality() : 0f;
        var typeId = zoneData instanceof RoomData room ? room.getZoneTypeId() : null;
        boolean degraded = zoneData instanceof RoomData room && room.isDegraded();
        String epithetName = zoneData instanceof RoomData room ? room.getEpithetName() : null;
        String generatedName = zoneData instanceof RoomData room2 ? room2.getGeneratedName() : null;
        return new SyncZoneGridPayload(
                zoneData.getRegionId(),
                zoneData.isOutdoor(),
                zoneData.getVolume(),
                zoneData.getEnclosureScore(),
                quality,
                typeId,
                degraded,
                epithetName,
                generatedName,
                zoneData.getMinX(),
                zoneData.getMinY(),
                zoneData.getMinZ(),
                zoneData.getMaxX(),
                zoneData.getMaxY(),
                zoneData.getMaxZ()
        );
    }

    void sendQualityChangeToPlayers(int triggerX, int triggerY, int triggerZ, float oldQuality, float newQuality, ServerLevel level) {
        var payload = new ZoneQualityChangePayload(triggerX, triggerY, triggerZ, oldQuality, newQuality);
        for (Player player : level.players()) {
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, payload);
            }
        }
    }

    void syncToPlayers(ChunkAccess chunk, ChunkClassificationData data, ServerLevel level) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        var payload = new SyncChunkClassificationPayload(chunkX, chunkZ, data);
        for (Player player : level.players()) {
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, payload);
            }
        }
    }

    // ---- Player zone tracking ----

    public void checkAllPlayerZones(ServerLevel level) {
        for (Player player : level.players()) {
            if (player instanceof ServerPlayer sp) {
                updatePlayerZone(sp, level);
            }
        }
    }

    private void updatePlayerZone(ServerPlayer player, ServerLevel level) {
        ZoneData zoneData = SpaceQuery.getRoomAt(level, player.blockPosition());
        UUID newId = zoneData != null ? zoneData.getRegionId() : null;

        ZoneAttachment att = player.getData(ZoneAttachment.ZONE.get());
        Zone current = att.getCurrentZone();
        UUID currentId = current != null ? current.getId() : null;

        if (!Objects.equals(newId, currentId)) {
            att.setCurrentZone(newId != null ? new Zone(newId) : null);
            PacketDistributor.sendToPlayer(player, new SyncPlayerZonePayload(newId));
        }
    }

    // ---- Bootstrap — runs once per chunk on initial load ----

    /**
     * One-time bootstrap classification for a newly loaded chunk.
     * Flood-fills OUTSIDE/INSIDE, creates Zone entities from discovered regions,
     * merges touching zones at chunk boundaries, and syncs to players.
     */
    public void bootstrapChunk(ChunkAccess chunk, ServerLevel level) {
        bootstrapChunk(chunk, level, 0);
    }

    /**
     * Removes pre-cached restored-zone data for the given chunk so it won't be re-marked INSIDE.
     */
    public void clearRestoredDataForChunk(int chunkX, int chunkZ) {
        restoredByChunk.remove(ChunkPos.asLong(chunkX, chunkZ));
    }

    /**
     * Ensures saved zones regain player-facing RoomData after world load.
     * Runs from the scheduler, never from chunk load callbacks. Evaluation is
     * skipped until players exist and every chunk the evaluator may touch is
     * already loaded, avoiding load-time chunk recursion.
     */
    public void processRestoredZones(ServerLevel level) {
        if (restoredZoneIds.isEmpty() || level.players().isEmpty()) return;

        List<UUID> finished = new ArrayList<>();
        for (UUID zoneId : new ArrayList<>(restoredZoneIds)) {
            Zone zone = getZoneById(zoneId);
            if (zone == null || zone.isDissolved()) {
                finished.add(zoneId);
                continue;
            }
            if (getRoom(zoneId) != null) {
                finished.add(zoneId);
                continue;
            }
            if (!isRestoredZoneLoadedForEvaluation(zone, level)) continue;
            if (!savedInteriorStillAir(zone, level)) continue;

            premarkRestoredInterior(zone, level);
            evaluateRoomAndSyncNow(zone, level);
            if (getRoom(zoneId) != null) {
                finished.add(zoneId);
            }
        }
        restoredZoneIds.removeAll(finished);
    }

    private void bootstrapChunk(ChunkAccess chunk, ServerLevel level, int depth) {
        ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
        if (!data.isDirty()) return;

        SpaceRegionRegistry regionRegistry = SpaceRegionRegistry.get(level);

        // ---- 0. Pre-mark restored zone blocks as INSIDE before classification ----
        long chunkKey = ChunkPos.asLong(chunk.getPos().x, chunk.getPos().z);
        Map<UUID, Set<BlockPos>> restoredInChunk = restoredByChunk.remove(chunkKey);
        if (restoredInChunk != null) {
            for (Set<BlockPos> blocks : restoredInChunk.values()) {
                for (BlockPos pos : blocks) {
                    data.setBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15,
                            ClassificationState.INSIDE);
                }
            }
        }

        // ---- 1. Full flood-fill classification ----
        ChunkClassifier classifier = new ChunkClassifier();
        classifier.classify(chunk);
        Map<UUID, Set<BlockPos>> regionBlocks = classifier.getLastRegionBlocks();

        // ---- 2. Create Zone entities from classified regions ----
        Set<UUID> createdZoneIds = new HashSet<>();
        for (ClassifiedRegion cr : data.getRegions()) {
            Set<BlockPos> blocks = regionBlocks.get(cr.getId());
            if (blocks == null || blocks.isEmpty()) continue;

            boolean alreadyClaimed = blocks.stream()
                    .anyMatch(p -> regionRegistry.getRegionIdAt(p) != null);
            if (alreadyClaimed) continue;

            Zone zone = Zone.createFromBootstrap(cr.getId(), blocks, cr.getOpeningArea(), level);
            if (zone != null) {
                createdZoneIds.add(zone.getId());
            }
        }

        data.setDirty(false);
        data.getRegions().clear();

        // ---- 3. Merge touching zones across chunk boundaries ----
        mergeAdjacentZonesAtBoundaries(chunk, level);

        data.getRegions().clear();

        // ---- 3b. Clean up orphaned zones ----
        List<UUID> orphanedZones = new ArrayList<>();
        for (Zone zone : new ArrayList<>(getAllZones())) {
            if (zone.isDissolved()) continue;
            Set<BlockPos> blocks = regionRegistry.getBlocksInRegion(zone.getId());
            if (blocks.isEmpty()) {
                orphanedZones.add(zone.getId());
            }
        }
        for (UUID orphanId : orphanedZones) {
            Zone orphan = getZoneById(orphanId);
            if (orphan != null && !orphan.isDissolved()) {
                orphan.dissolve(level);
            }
        }

        // ---- 4. Evaluate new zones + sync ----
        SettlementDetector settlementDetector = new SettlementDetector(level);
        settlementDetector.scanChunk(chunk);

        for (UUID zoneId : createdZoneIds) {
            Zone zone = getZoneById(zoneId);
            if (zone == null || zone.isDissolved()) continue;
            evaluateRoomAndSyncNow(zone, level);
        }

        // ---- 4b. Evaluate restored zones whose blocks are in this chunk ----
        if (restoredInChunk != null) {
            for (UUID zoneId : restoredInChunk.keySet()) {
                Zone zone = getZoneById(zoneId);
                if (zone != null && !zone.isDissolved() && getRoom(zoneId) == null) {
                    evaluateRoomAndSyncNow(zone, level);
                }
            }
        }

        recheckZonesTouchingChunk(chunk, level);
        syncToPlayers(chunk, data, level);
        checkAllPlayerZones(level);

        // ---- 5. Bootstrap loaded neighbors so cross-chunk rooms are complete ----
        if (depth < 4) {
            int cx = chunk.getPos().x, cz = chunk.getPos().z;
            bootstrapNeighborIfLoaded(cx - 1, cz, depth + 1, level);
            bootstrapNeighborIfLoaded(cx + 1, cz, depth + 1, level);
            bootstrapNeighborIfLoaded(cx, cz - 1, depth + 1, level);
            bootstrapNeighborIfLoaded(cx, cz + 1, depth + 1, level);
        }
    }

    /**
     * Re-runs zone geometry checks after a dirty chunk has been classified.
     * This catches rooms whose enclosure became valid only after the latest block
     * updates were placed into the chunk snapshot, while preserving delayed
     * player-facing room interpretation for already-known rooms.
     */
    private void recheckZonesTouchingChunk(ChunkAccess chunk, ServerLevel level) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        List<Zone> affected = new ArrayList<>();

        for (Zone zone : new ArrayList<>(getAllZones())) {
            if (zone.isDissolved()) continue;
            if (touchesChunk(zone, chunkX, chunkZ)) {
                affected.add(zone);
            }
        }

        for (Zone zone : affected) {
            Zone liveZone = getZoneById(zone.getId());
            if (liveZone == null || liveZone.isDissolved()) continue;

            liveZone.refreshOpeningArea(level);
            liveZone.updateBoundaryRows(level);
            liveZone.checkAndSealCeiling(level);
            evaluateRoomAndSync(liveZone, level);
        }
    }

    private boolean touchesChunk(Zone zone, int chunkX, int chunkZ) {
        for (BlockPos pos : zone.getAllOwnedBlocks()) {
            if ((pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ) {
                return true;
            }
        }
        return false;
    }

    private void bootstrapNeighborIfLoaded(int chunkX, int chunkZ, int depth, ServerLevel level) {
        net.minecraft.world.level.chunk.LevelChunk neighbor =
                level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (neighbor == null) return;
        ChunkClassificationData neighborData = ChunkClassificationAttachment.get(neighbor);
        if (neighborData.isDirty()) {
            bootstrapChunk(neighbor, level, depth);
        }
    }

    private void mergeAdjacentZonesAtBoundaries(ChunkAccess chunk, ServerLevel level) {
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        int x = chunk.getPos().x, z = chunk.getPos().z;

        int[][] cardinals = {{x - 1, z}, {x + 1, z}, {x, z - 1}, {x, z + 1}};
        for (int[] coord : cardinals) {
            net.minecraft.world.level.chunk.LevelChunk neighbor =
                    level.getChunkSource().getChunkNow(coord[0], coord[1]);
            if (neighbor == null) continue;

            List<SpaceRegionRegistry.MergeEvent> merges = registry.stitch(level, chunk, neighbor, 0);
            if (merges.isEmpty()) continue;

            ChunkClassificationData neighborData = ChunkClassificationAttachment.get(neighbor);
            Set<UUID> mergedSurvivors = new HashSet<>();
            for (SpaceRegionRegistry.MergeEvent merge : merges) {
                Zone surviving = getZoneById(merge.survivingId());
                Zone eliminated = getZoneById(merge.eliminatedId());
                if (surviving != null && eliminated != null && !surviving.equals(eliminated)) {
                    surviving.mergeWith(eliminated, level);
                    mergedSurvivors.add(merge.survivingId());
                }

                SpaceRegion sr = registry.getRegion(merge.survivingId());
                int vol = sr != null ? sr.getVolume() : 0;
                int opening = sr != null ? sr.getOpeningArea() : 0;
                if (neighborData.replaceRegionId(merge.eliminatedId(), merge.survivingId(), vol, opening)) {
                    syncToPlayers(neighbor, neighborData, level);
                }
            }

            // Re-evaluate surviving zones: ensureMinimumHeight, boundary rows, ceiling seal, HUD sync.
            // The step-4 loop only covers zones born in this chunk's own classification, so surviving
            // zones from prior chunks would never get post-merge height/geometry updates without this.
            for (UUID survivorId : mergedSurvivors) {
                Zone survivor = getZoneById(survivorId);
                if (survivor != null && !survivor.isDissolved()) {
                    evaluateRoomAndSyncNow(survivor, level);
                }
            }
        }
    }

    // ---- Zone restoration from saved data ----

    private void restoreZonesFromSavedData(ServerLevel level) {
        if (savedData == null || savedData.getZones().isEmpty()) return;

        SpaceRegionRegistry regionRegistry = SpaceRegionRegistry.get(level);

        for (Map.Entry<UUID, ZoneSavedData.ZoneRecord> entry : savedData.getZones().entrySet()) {
            UUID id = entry.getKey();
            ZoneSavedData.ZoneRecord record = entry.getValue();
            if (record.blocks().isEmpty()) continue;

            SpaceRegion region = new SpaceRegion(id, ClassificationState.INSIDE, 0, record.openingArea());
            regionRegistry.registerRegion(region);
            for (BlockPos pos : record.blocks()) {
                regionRegistry.mapBlockToRegion(pos, id);
                long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
                restoredByChunk
                        .computeIfAbsent(chunkKey, k -> new HashMap<>())
                        .computeIfAbsent(id, k -> new HashSet<>())
                        .add(pos);
            }

            Set<BlockPos> restoredInterior = new HashSet<>();
            Set<BlockPos> restoredEntries = new HashSet<>();
            for (BlockPos pos : record.blocks()) {
                var state = level.getBlockState(pos);
                if (Zone.isEntryBlock(state)) {
                    restoredEntries.add(pos);
                } else {
                    restoredInterior.add(pos);
                }
            }
            Zone zone = new Zone(id, restoredInterior, record.openingArea());
            zone.addOwnedEntryBlocks(restoredEntries);
            registerZone(zone);
            restoredZoneIds.add(id);

            if (record.customName() != null) {
                restoreCustomName(id, record.customName());
            }
        }

        LOGGER.info("[ZONE] Restored {} zone(s) from saved data", savedData.getZones().size());
    }

    private boolean isRestoredZoneLoadedForEvaluation(Zone zone, ServerLevel level) {
        Set<Long> chunks = new HashSet<>();
        for (BlockPos pos : zone.getAllOwnedBlocks()) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos check = pos.offset(dx, 0, dz);
                    chunks.add(ChunkPos.asLong(check.getX() >> 4, check.getZ() >> 4));
                }
            }
        }
        for (long key : chunks) {
            if (level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) == null) {
                return false;
            }
        }
        return true;
    }

    private boolean savedInteriorStillAir(Zone zone, ServerLevel level) {
        for (BlockPos pos : zone.getInteriorBlocks()) {
            var state = level.getBlockState(pos);
            if (!state.isAir()) {
                return false;
            }
        }
        return true;
    }

    private void premarkRestoredInterior(Zone zone, ServerLevel level) {
        for (BlockPos pos : zone.getAllOwnedBlocks()) {
            net.minecraft.world.level.chunk.LevelChunk chunk =
                    level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null) continue;
            ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
            data.setBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15, ClassificationState.INSIDE);
        }
    }

    // ---- Deferred expansion ----

    public void scheduleDeferredExpansion(Zone zone, BlockPos pos) {
        deferredExpansions.add(new DeferredExpansion(zone, pos));
    }

    public void processDeferredExpansions(ServerLevel level) {
        if (deferredExpansions.isEmpty()) return;
        List<DeferredExpansion> batch = List.copyOf(deferredExpansions);
        deferredExpansions.clear();
        for (DeferredExpansion exp : batch) {
            Zone zone = getZoneById(exp.zone.getId());
            if (zone == null || zone.isDissolved()) continue;
            zone.tryExpand(exp.pos, level);
            if (!isInGracePeriod(zone.getId())) {
                evaluateRoomAndSync(zone, level);
            }
        }
    }

    private record DeferredExpansion(Zone zone, BlockPos pos) {
    }

    // ---- Existing evaluation / metadata ----

    /**
     * Re-evaluates a zone's quality, type, and volume from current world state,
     * then updates the cache. Preserves the spatial extent from the previous
     * cached entry so furniture placement never changes the zone's bounding box —
     * only structural reclassification can do that.
     * <p>
     * Use this instead of getOrEvaluate whenever the zone's contents changed
     * (furniture placed/removed, merges, splits).
     */
    @Nullable
    public ZoneData reEvaluateRoom(UUID regionId, Level level) {
        ZoneData previous = cache.get(regionId);
        ZoneData fresh = evaluate(regionId, level);
        if (fresh != null) {
            if (previous != null && previous.hasSpatialExtent()) {
                fresh.setSpatialExtent(previous.getMinX(), previous.getMinY(), previous.getMinZ(),
                        previous.getMaxX(), previous.getMaxY(), previous.getMaxZ());
            }
            fresh.setInitialized(true);
            cache.put(regionId, fresh);
        }
        return fresh;
    }

    /**
     * @deprecated Use {@link #reEvaluateRoom(UUID, Level)}.
     */
    @Deprecated
    @Nullable
    public ZoneData reEvaluate(UUID regionId, Level level) {
        return reEvaluateRoom(regionId, level);
    }

    public void remove(UUID regionId) {
        cache.remove(regionId);
        zoneEntities.remove(regionId);
        customNames.remove(regionId);
        restoredZoneIds.remove(regionId);
    }

    public void invalidateAll() {
        cache.clear();
        zoneEntities.clear();
        savedData = null;
        roomChangeScheduler.clear();
        restoredZoneIds.clear();
        restoredByChunk.clear();
        dirtyZoneRecheckChunks.clear();
    }

    public void setCustomName(UUID regionId, String name) {
        customNames.put(regionId, name);
        if (savedData != null) savedData.setCustomName(regionId, name);
    }

    /**
     * Restores a custom name from saved data without marking the saved data dirty again.
     */
    public void restoreCustomName(UUID regionId, String name) {
        customNames.put(regionId, name);
    }

    @Nullable
    public String getCustomName(UUID regionId) {
        return customNames.get(regionId);
    }

    public void transferMetadata(UUID from, UUID to) {
        String name = customNames.get(from);
        if (name != null) {
            customNames.put(to, name);
        }
    }

    public Set<UUID> getAllRoomIds() {
        return new HashSet<>(cache.keySet());
    }

    @Nullable
    public ZoneData getRoomAt(BlockPos pos) {
        ZoneData outdoor = null;
        for (ZoneData zone : cache.values()) {
            if (!zone.isDisabled() && zone.contains(pos)) {
                if (!zone.isOutdoor()) return zone;
                outdoor = zone;
            }
        }
        return outdoor;
    }

    /**
     * @deprecated Use {@link #getAllRoomIds()}.
     */
    @Deprecated
    public Set<UUID> getAllZoneIds() {
        return getAllRoomIds();
    }

    /**
     * @deprecated Use {@link #getRoomAt(BlockPos)}.
     */
    @Deprecated
    @Nullable
    public ZoneData getZoneAt(BlockPos pos) {
        return getRoomAt(pos);
    }

    @Nullable
    private ZoneData evaluate(UUID regionId, Level level) {
        SpaceRegion region = SpaceRegionRegistry.get(level).getRegion(regionId);
        if (region == null) return null;

        BlockPos samplePos = firstBlockOf(regionId, level);
        BlockPos evaluationPos = samplePos != null ? samplePos : BlockPos.ZERO;
        RoomData room = ZoneEvaluator.computeRoomData(region, level);
        ResourceLocation typeId = ZoneTypeRegistry.match(room, level, evaluationPos);
        room.setZoneTypeId(typeId);

        ZoneData result;
        final float INDOOR_ENCLOSURE_THRESHOLD = 0.85f;
        if (typeId != null) {
            ZoneType type = ZoneTypeRegistry.get(typeId);
            if (type != null && room.getEnclosureScore() < type.minimumEnclosureScore()) {
                room.setDegraded(true);
            }
        } else if (room.getEnclosureScore() < INDOOR_ENCLOSURE_THRESHOLD) {
            room.setDegraded(true);
        }

        if (region.getType() == ClassificationState.INSIDE || (typeId != null && region.getType() != ClassificationState.OUTSIDE)) {
            result = room;
        } else if (region.getType() == ClassificationState.OUTSIDE && typeId != null) {
            room.setDegraded(true);
            result = room;
        } else {
            result = ZoneEvaluator.computeOutdoorZoneDataWithFurniture(region, room, level, evaluationPos);
        }

        if (result instanceof RoomData roomData && typeId != null) {
            RoomTypeQualityProfile profile = getRoomTypeQualityProfile(typeId);
            if (profile != null) {
                var refinedBreakdown = QualityEvaluator.evaluate(roomData.getVolume(), roomData.getEnclosureScore(),
                        roomData.getFurnitureCounts(), roomData.getSignalCounts(), profile);
                roomData.setQualityBreakdown(refinedBreakdown);
                roomData = new RoomData(roomData.getRegionId(), roomData.getVolume(), roomData.getEnclosureScore(),
                        roomData.getFurnitureCounts(), roomData.getSignalCounts(), roomData.getSurfaceCounts(),
                        refinedBreakdown.totalQuality);
                roomData.setQualityBreakdown(refinedBreakdown);
                roomData.setZoneTypeId(typeId);
                if (room.isDegraded()) roomData.setDegraded(true);
                if (room.getEpithetName() != null) roomData.setEpithetName(room.getEpithetName());
                if (room.getGeneratedName() != null) roomData.setGeneratedName(room.getGeneratedName());
                result = roomData;
            }
        }

        if (!isValidPlayerBuiltZone(regionId, level, result)) {
            return null;
        }

        if (result instanceof RoomData finalRoom) {
            int qualityPct = Math.round(finalRoom.getQuality() * 100);
            com.sanhiruzu.atelier.zone.naming.RoomEpithetRegistry.findMatch(finalRoom, qualityPct)
                    .ifPresent(e -> finalRoom.setEpithetName(e.displayName()));
            String genName = RoomNameGenerator.generateName(finalRoom);
            if (genName != null) finalRoom.setGeneratedName(genName);
        }

        computeSpatialExtent(result, regionId, level);
        return result;
    }

    private boolean isValidPlayerBuiltZone(UUID regionId, Level level, ZoneData zoneData) {
        return isValidPlayerBuiltZone(regionId, SpaceRegionRegistry.get(level),
                pos -> {
                    var state = level.getBlockState(pos);
                    return state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)
                            || state.is(BlockTags.STAIRS) || state.is(BlockTags.SLABS);
                },
                zoneData);
    }

    static boolean isValidPlayerBuiltZone(UUID regionId, SpaceRegionRegistry registry,
                                          java.util.function.Predicate<BlockPos> isEntry, ZoneData zoneData) {
        Set<BlockPos> regionBlocks = registry.getBlocksInRegion(regionId);
        if (regionBlocks.size() < 2) return false;

        for (BlockPos interiorPos : regionBlocks) {
            if (isEntry.test(interiorPos)) return true;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                        BlockPos neighborPos = interiorPos.offset(dx, dy, dz);
                        if (isEntry.test(neighborPos)) return true;
                    }
                }
            }
        }

        return false;
    }

    @Nullable
    private static RoomTypeQualityProfile getRoomTypeQualityProfile(ResourceLocation typeId) {
        String path = typeId.getPath();
        if (path.contains("bedroom")) return RoomTypeQualityProfile.BEDROOM;
        if (path.contains("greenhouse") || path.contains("garden")) return RoomTypeQualityProfile.GREENHOUSE;
        if (path.contains("enchanting")) return RoomTypeQualityProfile.ENCHANTING;
        if (path.contains("smithy") || path.contains("smith")) return RoomTypeQualityProfile.SMITHY;
        return null;
    }

    private void computeSpatialExtent(ZoneData zone, UUID regionId, Level level) {
        computeSpatialExtent(zone, regionId, SpaceRegionRegistry.get(level),
                pos -> {
                    var state = level.getBlockState(pos);
                    return state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)
                            || state.is(BlockTags.STAIRS) || state.is(BlockTags.SLABS);
                });
    }

    static void computeSpatialExtent(ZoneData zone, UUID regionId, SpaceRegionRegistry registry,
                                     java.util.function.Predicate<BlockPos> isEntry) {
        Set<BlockPos> blocks = registry.getBlocksInRegion(regionId);
        if (blocks.isEmpty()) return;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : blocks) {
            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getY() < minY) minY = pos.getY();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getY() > maxY) maxY = pos.getY();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();
        }

        Set<BlockPos> entryBlocks = new HashSet<>();
        for (BlockPos interiorPos : blocks) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                        BlockPos neighborPos = interiorPos.offset(dx, dy, dz);
                        if (isEntry.test(neighborPos)) {
                            entryBlocks.add(neighborPos);
                        }
                    }
                }
            }
        }

        for (BlockPos pos : entryBlocks) {
            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getY() < minY) minY = pos.getY();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getY() > maxY) maxY = pos.getY();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();
        }

        zone.setSpatialExtent(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Nullable
    private BlockPos firstBlockOf(UUID regionId, Level level) {
        Set<BlockPos> blocks = SpaceRegionRegistry.get(level).getBlocksInRegion(regionId);
        return blocks.isEmpty() ? null : blocks.iterator().next();
    }
}
