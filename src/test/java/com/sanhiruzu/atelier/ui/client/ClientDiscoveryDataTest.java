package com.sanhiruzu.atelier.ui.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClientDiscoveryDataTest {
    @BeforeEach
    void setUp() {
        ClientDiscoveryData.clear();
    }

    @Test
    void updateStoresDiscoveredRoomScores() {
        ClientDiscoveryData.update(Map.of(
                "zen_atelier:bedroom", 88,
                "zen_atelier:greenhouse", 71
        ));

        assertTrue(ClientDiscoveryData.isDiscovered("zen_atelier:bedroom"));
        assertTrue(ClientDiscoveryData.isDiscovered("zen_atelier:greenhouse"));
        assertEquals(88, ClientDiscoveryData.getBestScore("zen_atelier:bedroom"));
        assertEquals(71, ClientDiscoveryData.getBestScore("zen_atelier:greenhouse"));
    }

    @Test
    void updateReplacesPreviousDiscoveryData() {
        ClientDiscoveryData.update(Map.of("zen_atelier:bedroom", 88));
        ClientDiscoveryData.update(Map.of("zen_atelier:atelier", 92));

        assertFalse(ClientDiscoveryData.isDiscovered("zen_atelier:bedroom"));
        assertTrue(ClientDiscoveryData.isDiscovered("zen_atelier:atelier"));
    }

    @Test
    void unknownRoomReturnsMissingScore() {
        assertFalse(ClientDiscoveryData.isDiscovered("zen_atelier:missing"));
        assertEquals(-1, ClientDiscoveryData.getBestScore("zen_atelier:missing"));
    }

    @Test
    void clearRemovesAllDiscoveries() {
        ClientDiscoveryData.update(Map.of("zen_atelier:bedroom", 88));

        ClientDiscoveryData.clear();

        assertFalse(ClientDiscoveryData.isDiscovered("zen_atelier:bedroom"));
        assertEquals(-1, ClientDiscoveryData.getBestScore("zen_atelier:bedroom"));
    }
}
