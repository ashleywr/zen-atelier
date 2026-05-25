package com.sanhiruzu.atelier.space.zone;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoomChangeSchedulerTest {
    @Test
    void firstTickEstablishesCurrentDayWithoutResolving() {
        RoomChangeScheduler scheduler = new RoomChangeScheduler();

        assertFalse(scheduler.shouldResolve(3L, false));
        assertFalse(scheduler.shouldResolve(3L, false));
    }

    @Test
    void dayRolloverResolvesDirtyRoomInterpretation() {
        RoomChangeScheduler scheduler = new RoomChangeScheduler();

        assertFalse(scheduler.shouldResolve(0L, false));
        assertFalse(scheduler.shouldResolve(0L, false));
        assertTrue(scheduler.shouldResolve(1L, false));
        assertFalse(scheduler.shouldResolve(1L, false));
    }

    @Test
    void debugModeResolvesWithoutWaitingForDayRollover() {
        RoomChangeScheduler scheduler = new RoomChangeScheduler();

        assertFalse(scheduler.shouldResolve(0L, false));
        assertTrue(scheduler.shouldResolve(0L, true));
        assertTrue(scheduler.shouldResolve(0L, true));
    }

    @Test
    void clearRemovesQueuedRoomsAndResetsDayTracking() {
        RoomChangeScheduler scheduler = new RoomChangeScheduler();
        scheduler.markDirtyForTest(UUID.randomUUID());
        scheduler.shouldResolve(5L, false);

        scheduler.clear();

        assertEquals(0, scheduler.dirtyCountForTest());
        assertFalse(scheduler.shouldResolve(5L, false));
    }
}
