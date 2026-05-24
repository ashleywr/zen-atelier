package com.sanhiruzu.atelier.client;

import com.sanhiruzu.atelier.network.SyncZoneGridPayload;
import com.sanhiruzu.atelier.space.zone.OutdoorZoneData;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientZoneCache {
    private static final Map<UUID, ZoneData> ZONE_CACHE = new HashMap<>();

    public static void storeZone(SyncZoneGridPayload payload) {
        boolean isNewZone = !ZONE_CACHE.containsKey(payload.zoneId());

        ZoneData data;
        if (payload.isOutdoor()) {
            data = new OutdoorZoneData(payload.zoneId(), payload.volume(), payload.enclosureScore(), BlockPos.ZERO);
        } else {
            RoomData room = new RoomData(payload.zoneId(), payload.volume(), payload.enclosureScore(), Map.of(), payload.quality());
            room.setZoneTypeId(payload.zoneTypeId());
            room.setDegraded(payload.degraded());
            if (payload.epithetName() != null) {
                room.setEpithetName(payload.epithetName());
            }
            if (payload.generatedName() != null) {
                room.setGeneratedName(payload.generatedName());
            }
            data = room;
        }
        data.setSpatialExtent(payload.minX(), payload.minY(), payload.minZ(), payload.maxX(), payload.maxY(), payload.maxZ());
        ZONE_CACHE.put(payload.zoneId(), data);

        if (isNewZone) {
            ZoneVfxManager.onZoneRegistered(payload.minX(), payload.minY(), payload.minZ(), payload.maxX(), payload.maxY(), payload.maxZ());
        }
    }

    @Nullable
    public static ZoneData getZone(UUID zoneId) {
        return ZONE_CACHE.get(zoneId);
    }

    @Nullable
    public static ZoneData getZoneAt(BlockPos pos) {
        ZoneData outdoor = null;
        for (ZoneData zone : ZONE_CACHE.values()) {
            if (zone.contains(pos)) {
                if (!zone.isOutdoor()) return zone;
                outdoor = zone;
            }
        }
        return outdoor;
    }

    public static java.util.Collection<ZoneData> getAllZones() {
        return ZONE_CACHE.values();
    }

    public static void clear() {
        ZONE_CACHE.clear();
    }

    public static void remove(UUID zoneId) {
        ZONE_CACHE.remove(zoneId);
    }
}
