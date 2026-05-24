package com.sanhiruzu.atelier.space.zone;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Schedules player-facing room interpretation separately from raw zone geometry.
 *
 * <p>Zone geometry updates immediately when blocks change. Room interpretation
 * (quality, type, naming, and other player-facing meaning) is normally resolved
 * on the next Minecraft day. If any online player has Atelier debug enabled, the
 * same dirty rooms are resolved immediately for faster iteration.</p>
 */
public final class RoomChangeScheduler {
    private final Set<UUID> dirtyRooms = new HashSet<>();
    private long lastDayIndex = -1L;

    public void markDirty(Zone zone, ServerLevel level, ZoneRegistry registry) {
        if (zone.isDissolved()) return;
        dirtyRooms.add(zone.getId());
        if (isDebugEnabled(level)) {
            resolveDirtyRooms(level, registry);
        }
    }

    public void tick(ServerLevel level, ZoneRegistry registry) {
        long dayIndex = level.getDayTime() / 24000L;
        if (shouldResolve(dayIndex, isDebugEnabled(level))) {
            resolveDirtyRooms(level, registry);
        }
    }

    public void clear() {
        dirtyRooms.clear();
        lastDayIndex = -1L;
    }

    private void resolveDirtyRooms(ServerLevel level, ZoneRegistry registry) {
        if (dirtyRooms.isEmpty()) return;

        Set<UUID> batch = Set.copyOf(dirtyRooms);
        dirtyRooms.clear();

        for (UUID roomId : batch) {
            Zone zone = registry.getZoneById(roomId);
            if (zone == null || zone.isDissolved()) continue;
            registry.evaluateRoomAndSyncNow(zone, level);
        }
    }

    private static boolean isDebugEnabled(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.getPersistentData().getBoolean("spaceregion_debug")) {
                return true;
            }
        }
        return false;
    }

    boolean shouldResolve(long dayIndex, boolean debugEnabled) {
        if (lastDayIndex < 0L) {
            lastDayIndex = dayIndex;
            return false;
        }

        if (debugEnabled) {
            lastDayIndex = dayIndex;
            return true;
        }

        if (dayIndex != lastDayIndex) {
            lastDayIndex = dayIndex;
            return true;
        }

        return false;
    }

    void markDirtyForTest(UUID roomId) {
        dirtyRooms.add(roomId);
    }

    int dirtyCountForTest() {
        return dirtyRooms.size();
    }
}
