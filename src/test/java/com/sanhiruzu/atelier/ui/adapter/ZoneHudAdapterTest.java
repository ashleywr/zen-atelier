package com.sanhiruzu.atelier.ui.adapter;

import com.sanhiruzu.atelier.space.ClassificationState;
import com.sanhiruzu.atelier.space.zone.OutdoorZoneData;
import com.sanhiruzu.atelier.space.zone.RoomData;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ZoneHudAdapterTest {
    @Test
    void insideClassificationCreatesInsideSnapshot() {
        Optional<ZoneHudAdapter.ZoneHudSnapshot> snapshot =
                ZoneHudAdapter.snapshotForClassification(ClassificationState.INSIDE, 2, 4, -3);

        assertTrue(snapshot.isPresent());
        assertEquals("Inside Space", snapshot.get().name());
        assertEquals("2 detected region(s)", snapshot.get().activeProfiles());
        assertEquals(60, snapshot.get().score());
    }

    @Test
    void partialClassificationCreatesPartialSnapshot() {
        Optional<ZoneHudAdapter.ZoneHudSnapshot> snapshot =
                ZoneHudAdapter.snapshotForClassification(ClassificationState.PARTIAL, 0, 4, -3);

        assertTrue(snapshot.isPresent());
        assertEquals("Partial Space", snapshot.get().name());
        assertEquals("Zone backend pending", snapshot.get().activeProfiles());
        assertEquals(30, snapshot.get().score());
    }

    @Test
    void nonInteriorClassificationDoesNotCreateSnapshot() {
        assertTrue(ZoneHudAdapter.snapshotForClassification(ClassificationState.SOLID, 1, 0, 0).isEmpty());
        assertTrue(ZoneHudAdapter.snapshotForClassification(ClassificationState.OUTSIDE, 1, 0, 0).isEmpty());
    }

    @Test
    void snapshotIdIsStableForSameChunk() {
        ZoneHudAdapter.ZoneHudSnapshot first =
                ZoneHudAdapter.snapshotForClassification(ClassificationState.INSIDE, 1, 4, -3).orElseThrow();
        ZoneHudAdapter.ZoneHudSnapshot second =
                ZoneHudAdapter.snapshotForClassification(ClassificationState.PARTIAL, 5, 4, -3).orElseThrow();

        assertEquals(first.id(), second.id());
    }

    // --- snapshotFromZoneData ---

    @Test
    void roomDataWithoutZoneTypeShowsIndoorSpace() {
        RoomData room = new RoomData(UUID.randomUUID(), 80, 0.9f, Map.of(), 0.72f);
        ZoneHudAdapter.ZoneHudSnapshot snapshot = ZoneHudAdapter.snapshotFromZoneData(room);

        assertEquals("Room", snapshot.name());
        assertEquals(72, snapshot.score());
        assertEquals("", snapshot.activeProfiles());
        assertFalse(snapshot.id().toString().isBlank());
    }

    @Test
    void roomDataWithZoneTypeShowsFormattedName() {
        RoomData room = new RoomData(UUID.randomUUID(), 100, 1.0f, Map.of(), 1.0f);
        room.setZoneTypeId(ResourceLocation.fromNamespaceAndPath("zen_atelier", "furnace_room"));
        ZoneHudAdapter.ZoneHudSnapshot snapshot = ZoneHudAdapter.snapshotFromZoneData(room);

        assertEquals("Furnace room", snapshot.name());
        assertEquals(100, snapshot.score());
    }

    @Test
    void outdoorZoneDataShowsOutdoorWithZeroScore() {
        OutdoorZoneData outdoor = new OutdoorZoneData(UUID.randomUUID(), 0, 0.0f, null);
        ZoneHudAdapter.ZoneHudSnapshot snapshot = ZoneHudAdapter.snapshotFromZoneData(outdoor);

        assertEquals("Outdoor", snapshot.name());
        assertEquals(0, snapshot.score());
    }

    @Test
    void roomDataNameWithDegradedStatus() {
        RoomData degradedRoom = new RoomData(UUID.randomUUID(), 100, 0.5f, Map.of(), 0.75f);
        degradedRoom.setDegraded(true);
        degradedRoom.setZoneTypeId(ResourceLocation.fromNamespaceAndPath("zen_atelier", "church"));
        ZoneHudAdapter.ZoneHudSnapshot snapshot = ZoneHudAdapter.snapshotFromZoneData(degradedRoom);

        // Enclosure 0.5 = at boundary, so "Exposed" (>=0.5) in activeProfiles.
        // The name itself is just the zone type; degraded prefix moved to the mixed-use line.
        assertEquals("Church", snapshot.name());
        assertEquals(75, snapshot.score());
        assertEquals("Exposed", snapshot.activeProfiles());
    }
}
