package com.sanhiruzu.atelier.synthesis.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomAlchemyContextTest {
    @Test
    void neutralDoesNotConstrainTier() {
        RoomAlchemyContext ctx = RoomAlchemyContext.neutral();
        assertEquals(6, ctx.tierCap());
        assertEquals(0, ctx.quality());
        assertEquals(0, ctx.stability());
        assertEquals(0, ctx.riskBias());
    }
}
