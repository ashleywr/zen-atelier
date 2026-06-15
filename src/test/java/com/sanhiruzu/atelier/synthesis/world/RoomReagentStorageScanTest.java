package com.sanhiruzu.atelier.synthesis.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomReagentStorageScanTest {
    @Test
    void scanRadiusIsBounded() {
        assertEquals(8, RoomReagentStorage.STORAGE_SCAN_RADIUS);
    }
}
