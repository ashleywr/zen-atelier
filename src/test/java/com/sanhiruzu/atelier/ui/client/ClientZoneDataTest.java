package com.sanhiruzu.atelier.ui.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClientZoneDataTest {
    @BeforeEach
    void setUp() {
        ClientZoneData.resetForTest();
    }

    @Test
    void updateStoresCurrentZoneDisplayData() {
        UUID id = UUID.randomUUID();

        ClientZoneData.update(id, "Bedroom", "Gilded Bedroom", "The Soft Crown", "Custom Room", "bedroom, luxury", 82, "Size:30% Encl:35% Furn:20% Craft:15%");

        assertTrue(ClientZoneData.isInZone());
        assertEquals(id, ClientZoneData.getCurrentZoneId());
        assertEquals("Bedroom", ClientZoneData.getCurrentZoneName());
        assertEquals("Gilded Bedroom", ClientZoneData.getCurrentGeneratedName());
        assertEquals("bedroom, luxury", ClientZoneData.getCurrentActiveProfiles());
        assertEquals(82, ClientZoneData.getCurrentZenScore());
    }

    @Test
    void updateNormalizesNullTextFields() {
        ClientZoneData.update(UUID.randomUUID(), null, null, null, null, null, 50, null);

        assertEquals("", ClientZoneData.getCurrentZoneName());
        assertEquals("", ClientZoneData.getCurrentGeneratedName());
        assertEquals("", ClientZoneData.getCurrentActiveProfiles());
    }

    @Test
    void updateClampsScoreToValidHudRange() {
        ClientZoneData.update(UUID.randomUUID(), "Low", "", "", "", "", -20, "");
        assertEquals(0, ClientZoneData.getCurrentZenScore());

        ClientZoneData.update(UUID.randomUUID(), "High", "", "", "", "", 250, "");
        assertEquals(100, ClientZoneData.getCurrentZenScore());
    }

    @Test
    void clearMarksPlayerOutOfZoneButKeepsSafeDisplayStrings() {
        ClientZoneData.update(UUID.randomUUID(), "Bedroom", "Gilded Bedroom", "The Soft Crown", "", "bedroom", 82, "Size:30% Encl:35% Furn:20% Craft:15%");

        ClientZoneData.clear();

        assertFalse(ClientZoneData.isInZone());
        assertNull(ClientZoneData.getCurrentZoneId());
        assertEquals("Bedroom", ClientZoneData.getCurrentZoneName());
    }

    @Test
    void displaySubtitleRequiresDifferentGeneratedNameAndSpecificName() {
        ClientZoneData.update(UUID.randomUUID(), "Bedroom", "Bedroom", "", "", "bedroom", 50, "");
        assertFalse(ClientZoneData.hasDisplaySubtitle());

        ClientZoneData.update(UUID.randomUUID(), "Bedroom", "Gilded Bedroom", "", "", "bedroom", 50, "");
        assertFalse(ClientZoneData.hasDisplaySubtitle());

        ClientZoneData.update(UUID.randomUUID(), "Bedroom", "Gilded Bedroom", "The Soft Crown", "", "bedroom", 50, "Size:30% Encl:35% Furn:20% Craft:15%");
        assertTrue(ClientZoneData.hasDisplaySubtitle());
    }

    @Test
    void roomHudRevealsOnEntryThenExpires() {
        ClientZoneData.update(UUID.randomUUID(), "Bedroom", "", "", "", "", 50, "");

        assertTrue(ClientZoneData.shouldShowRoomHud());

        for (int i = 0; i < 100; i++) {
            ClientZoneData.tick();
        }

        assertFalse(ClientZoneData.shouldShowRoomHud());
        assertTrue(ClientZoneData.isInZone());
    }

    @Test
    void roomHudRevealsWhenKeybindRequestsIt() {
        ClientZoneData.update(UUID.randomUUID(), "Bedroom", "", "", "", "", 50, "");
        for (int i = 0; i < 100; i++) {
            ClientZoneData.tick();
        }

        ClientZoneData.revealRoomHud();

        assertTrue(ClientZoneData.shouldShowRoomHud());
    }

    @Test
    void debugModeRefreshesRoomHudWhileInside() {
        ClientZoneData.update(UUID.randomUUID(), "Bedroom", "", "", "", "", 50, "");
        for (int i = 0; i < 100; i++) {
            ClientZoneData.tick();
        }

        ClientZoneData.setDebugMode(true);
        ClientZoneData.update(ClientZoneData.getCurrentZoneId(), "Bedroom", "", "", "", "", 50, "");

        assertTrue(ClientZoneData.shouldShowRoomHud());
    }

    @Test
    void debugModeDoesNotKeepRoomHudVisibleAfterLeaving() {
        ClientZoneData.update(UUID.randomUUID(), "Bedroom", "", "", "", "", 50, "");
        ClientZoneData.setDebugMode(true);
        ClientZoneData.clearSilently();

        for (int i = 0; i < 100; i++) {
            ClientZoneData.tick();
        }

        assertFalse(ClientZoneData.shouldShowRoomHud());
    }

    @Test
    void debugModeDoesNotShowRoomHudWithoutRoomSnapshot() {
        ClientZoneData.setDebugMode(true);

        assertFalse(ClientZoneData.shouldShowRoomHud());
    }

    @Test
    void forgetRoomSnapshotClearsStaleHudImmediately() {
        ClientZoneData.update(UUID.randomUUID(), "Bedroom", "", "", "", "", 50, "");

        ClientZoneData.forgetRoomSnapshot();

        assertFalse(ClientZoneData.shouldShowRoomHud());
        assertNull(ClientZoneData.getCurrentZoneId());
        assertEquals("", ClientZoneData.getCurrentZoneName());
    }
}
