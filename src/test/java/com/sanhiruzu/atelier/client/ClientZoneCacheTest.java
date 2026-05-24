package com.sanhiruzu.atelier.client;

import com.sanhiruzu.atelier.network.SyncZoneGridPayload;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClientZoneCacheTest {

    @BeforeEach
    void clear() {
        ClientZoneCache.clear();
    }

    @Test
    void storeZonePopulatesCache() {
        UUID id = UUID.randomUUID();
        ClientZoneCache.storeZone(roomPayload(id, 0, 0, 15, 15));
        assertThat(ClientZoneCache.getZone(id)).isNotNull();
    }

    @Test
    void storeZoneSetsExtentOnZoneData() {
        UUID id = UUID.randomUUID();
        ClientZoneCache.storeZone(roomPayload(id, 10, 20, 25, 40));
        ZoneData zone = ClientZoneCache.getZone(id);
        assertThat(zone).isNotNull();
        assertThat(zone.hasSpatialExtent()).isTrue();
        assertThat(zone.getMinX()).isEqualTo(10);
        assertThat(zone.getMinZ()).isEqualTo(20);
        assertThat(zone.getMaxX()).isEqualTo(25);
        assertThat(zone.getMaxZ()).isEqualTo(40);
    }

    @Test
    void getZoneAtReturnsZoneWhoseBoundsContainPosition() {
        UUID id = UUID.randomUUID();
        ClientZoneCache.storeZone(roomPayload(id, 0, 0, 15, 15));
        ZoneData found = ClientZoneCache.getZoneAt(new BlockPos(8, 64, 8));
        assertThat(found).isNotNull();
        assertThat(found.getRegionId()).isEqualTo(id);
    }

    @Test
    void getZoneAtReturnsNullWhenPositionIsOutsideAllZones() {
        ClientZoneCache.storeZone(roomPayload(UUID.randomUUID(), 0, 0, 15, 15));
        assertThat(ClientZoneCache.getZoneAt(new BlockPos(100, 64, 100))).isNull();
    }

    @Test
    void getZoneAtReturnsNullWhenCacheIsEmpty() {
        assertThat(ClientZoneCache.getZoneAt(new BlockPos(0, 64, 0))).isNull();
    }

    @Test
    void getZoneAtPrefersIndoorOverOutdoorForSamePosition() {
        UUID indoorId = UUID.randomUUID();
        UUID outdoorId = UUID.randomUUID();
        ClientZoneCache.storeZone(roomPayload(indoorId, 0, 0, 15, 15));
        ClientZoneCache.storeZone(outdoorPayload(outdoorId, 0, 0, 15, 15));
        ZoneData result = ClientZoneCache.getZoneAt(new BlockPos(8, 64, 8));
        assertThat(result).isInstanceOf(RoomData.class);
        assertThat(result.getRegionId()).isEqualTo(indoorId);
    }

    @Test
    void getZoneAtFallsBackToOutdoorWhenNoIndoorZoneContainsPosition() {
        UUID outdoorId = UUID.randomUUID();
        ClientZoneCache.storeZone(outdoorPayload(outdoorId, 0, 0, 15, 15));
        ZoneData result = ClientZoneCache.getZoneAt(new BlockPos(8, 64, 8));
        assertThat(result).isNotNull();
        assertThat(result.getRegionId()).isEqualTo(outdoorId);
    }

    @Test
    void clearEmptiesCache() {
        ClientZoneCache.storeZone(roomPayload(UUID.randomUUID(), 0, 0, 15, 15));
        ClientZoneCache.clear();
        assertThat(ClientZoneCache.getZoneAt(new BlockPos(8, 64, 8))).isNull();
    }

    @Test
    void storingZoneWithSameIdOverwritesPrevious() {
        UUID id = UUID.randomUUID();
        ClientZoneCache.storeZone(roomPayload(id, 0, 0, 15, 15));
        ClientZoneCache.storeZone(roomPayload(id, 100, 100, 115, 115));
        ZoneData zone = ClientZoneCache.getZone(id);
        assertThat(zone.getMinX()).isEqualTo(100);
    }

    // --- helpers ---

    private SyncZoneGridPayload roomPayload(UUID id, int minX, int minZ, int maxX, int maxZ) {
        return new SyncZoneGridPayload(id, false, 100, 0.9f, 0.7f, null, false, null, null,
                minX, 60, minZ, maxX, 70, maxZ);
    }

    private SyncZoneGridPayload outdoorPayload(UUID id, int minX, int minZ, int maxX, int maxZ) {
        return new SyncZoneGridPayload(id, true, 0, 0f, 0f, null, false, null, null,
                minX, 60, minZ, maxX, 70, maxZ);
    }
}
