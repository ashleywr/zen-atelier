package com.sanhiruzu.atelier.resources;

import com.sanhiruzu.atelier.data.Signals;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentSignalRegistryTest {
    @Test
    void environmentSignalsAreRegistered() {
        assertThat(Signals.predicates().keySet()).containsAll(List.of(
                "dirt_like",
                "mossy",
                "rough_stone",
                "metallurgy",
                "kitchen",
                "lava_heat",
                "smoke_source",
                "damp_source",
                "dust_source"
        ));
    }
}
