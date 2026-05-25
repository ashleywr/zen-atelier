package com.sanhiruzu.atelier.space.zone;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoneReconcilerTest {
    @Test
    void validFreshZoneActivatesWhenNoPreviousDataExists() {
        ZoneReconciliationDecision decision = ZoneReconciler.decide(
                null,
                roomData(),
                false);

        assertEquals(ZoneReconciliationAction.ACTIVATE, decision.action());
    }

    @Test
    void validFreshZoneUpdatesExistingData() {
        ZoneReconciliationDecision decision = ZoneReconciler.decide(
                roomData(),
                roomData(),
                false);

        assertEquals(ZoneReconciliationAction.UPDATE, decision.action());
    }

    @Test
    void validFreshZoneRecoversFromGracePeriod() {
        ZoneReconciliationDecision decision = ZoneReconciler.decide(
                disabledRoomData(),
                roomData(),
                true);

        assertEquals(ZoneReconciliationAction.RECOVER, decision.action());
    }

    @Test
    void invalidFreshZoneEntersGraceWhenPreviouslyValid() {
        ZoneReconciliationDecision decision = ZoneReconciler.decide(
                roomData(),
                null,
                false);

        assertEquals(ZoneReconciliationAction.ENTER_GRACE, decision.action());
    }

    @Test
    void invalidFreshZoneStaysInGraceWhenTimerAlreadyExists() {
        ZoneReconciliationDecision decision = ZoneReconciler.decide(
                disabledRoomData(),
                null,
                true);

        assertEquals(ZoneReconciliationAction.STAY_IN_GRACE, decision.action());
    }

    @Test
    void neverValidZoneDissolvesWhenFreshEvaluationFails() {
        ZoneReconciliationDecision decision = ZoneReconciler.decide(
                null,
                null,
                false);

        assertEquals(ZoneReconciliationAction.DISSOLVE, decision.action());
    }

    private static RoomData roomData() {
        RoomData data = new RoomData(UUID.randomUUID(), 12, 0.9f, Map.of(), 0.5f);
        data.setInitialized(true);
        return data;
    }

    private static RoomData disabledRoomData() {
        RoomData data = roomData();
        data.setDisabled(true, 100L);
        return data;
    }
}
