package com.sanhiruzu.atelier.synthesis.core;

import com.sanhiruzu.atelier.api.EnvironmentAirHazard;
import com.sanhiruzu.atelier.api.EnvironmentEffects;
import com.sanhiruzu.atelier.api.EnvironmentSnapshot;
import com.sanhiruzu.atelier.api.EnvironmentTemperatureBand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentAlchemyContextTest {
    @Test
    void mapsSnapshotFactsIntoRoomSignalsWithoutConstrainingTier() {
        EnvironmentSnapshot snapshot = new EnvironmentSnapshot(
                Map.of("metallurgy", 2, "kitchen", 0),
                true,
                true,
                false,
                EnvironmentTemperatureBand.WARM,
                Set.of(EnvironmentAirHazard.SMOKY),
                Map.of()
        );

        RoomAlchemyContext context = EnvironmentAlchemyContext.from(
                snapshot,
                List.of(EnvironmentEffects.label("forge_work"))
        );

        assertThat(context.tierCap()).isEqualTo(6);
        assertThat(context.signals()).containsExactlyInAnyOrder(
                "metallurgy",
                "environment:covered",
                "environment:indoors_like",
                "environment:temperature/warm",
                "environment:air/smoky",
                "effect:forge_work"
        );
    }

    @Test
    void mapsRiskAndStabilityEffectsToRoomContextPoints() {
        RoomAlchemyContext context = EnvironmentAlchemyContext.from(
                EnvironmentSnapshot.AMBIENT,
                List.of(
                        EnvironmentEffects.modifyRisk("cluttered", 0.15f),
                        EnvironmentEffects.modifyRisk("orderly", -0.05f),
                        EnvironmentEffects.modifyStability("sheltered", 0.20f)
                )
        );

        assertThat(context.riskBias()).isEqualTo(10);
        assertThat(context.stability()).isEqualTo(20);
    }
}
