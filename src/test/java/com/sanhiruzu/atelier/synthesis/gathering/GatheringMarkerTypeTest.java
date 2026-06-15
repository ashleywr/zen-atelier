package com.sanhiruzu.atelier.synthesis.gathering;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatheringMarkerTypeTest {
    @Test
    void resolvesSerializedNamesAndDefaultsUnknownValuesToForage() {
        assertThat(GatheringMarkerType.fromSerializedName("forage")).isEqualTo(GatheringMarkerType.FORAGE);
        assertThat(GatheringMarkerType.fromSerializedName("ore")).isEqualTo(GatheringMarkerType.ORE);
        assertThat(GatheringMarkerType.fromSerializedName("strike")).isEqualTo(GatheringMarkerType.STRIKE);

        assertThat(GatheringMarkerType.fromSerializedName("")).isEqualTo(GatheringMarkerType.FORAGE);
        assertThat(GatheringMarkerType.fromSerializedName("missing")).isEqualTo(GatheringMarkerType.FORAGE);
    }

    @Test
    void exposesStableSerializedNames() {
        assertThat(GatheringMarkerType.FORAGE.serializedName()).isEqualTo("forage");
        assertThat(GatheringMarkerType.ORE.serializedName()).isEqualTo("ore");
        assertThat(GatheringMarkerType.STRIKE.serializedName()).isEqualTo("strike");
    }
}
