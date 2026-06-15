package com.sanhiruzu.atelier.synthesis.vfx.data;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TieredValueTest {
    @Test
    void scalarAppliesToAllTiers() {
        TieredValue v = TieredValue.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("1.5")).getOrThrow();
        assertThat(v.floatAt(0)).isEqualTo(1.5f);
        assertThat(v.floatAt(3)).isEqualTo(1.5f);
    }

    @Test
    void arrayMapsEachTierAndClamps() {
        TieredValue v = TieredValue.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("[4,7,11,16]")).getOrThrow();
        assertThat(v.intAt(0)).isEqualTo(4);
        assertThat(v.intAt(2)).isEqualTo(11);
        assertThat(v.intAt(-5)).isEqualTo(4);   // clamp low
        assertThat(v.intAt(99)).isEqualTo(16);  // clamp high
    }

    @Test
    void wrongLengthArrayIsAnError() {
        assertThat(TieredValue.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("[1,2,3]")).error()).isPresent();
    }
}
