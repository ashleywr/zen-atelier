package com.sanhiruzu.atelier.api;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentEffectRegistryTest {
    private static EnvironmentSnapshot snapshot;

    @BeforeAll
    static void prepareMinecraftEntityTypes() {
        if (LoadingModList.get() == null) {
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        snapshot = new EnvironmentSnapshot(
                Map.of("bookshelf", 3, "carpet", 1, "industrial_blocks", 0),
                true,
                true,
                true,
                EnvironmentTemperatureBand.PLEASANT,
                Set.of(),
                Map.of(EntityType.CAT, 1)
        );
    }

    @Test
    void builtInConditionsMatchSnapshotFacts() {
        EnvironmentCondition condition = EnvironmentConditions.allOf(
                EnvironmentConditions.covered(),
                EnvironmentConditions.indoorsLike(),
                EnvironmentConditions.enclosed(),
                EnvironmentConditions.signalAtLeast("bookshelf", 2),
                EnvironmentConditions.signalAbsent("industrial_blocks"),
                EnvironmentConditions.temperature(EnvironmentTemperatureBand.PLEASANT),
                EnvironmentConditions.nearEntityAtLeast(EntityType.CAT, 1)
        );

        assertThat(condition.matches(snapshot)).isTrue();
        assertThat(EnvironmentConditions.signalAtLeast("bookshelf", 4).matches(snapshot)).isFalse();
    }

    @Test
    void effectFactoriesCreateTypedOutputs() {
        EnvironmentEffect speed = EnvironmentEffects.modifySpeed("metallurgy", 0.15f);
        EnvironmentEffect veto = EnvironmentEffects.veto("too_damp");

        assertThat(speed.type()).isEqualTo(EnvironmentEffectType.SPEED_MODIFIER);
        assertThat(speed.id()).isEqualTo("metallurgy");
        assertThat(speed.amount()).isEqualTo(0.15f);
        assertThat(veto.type()).isEqualTo(EnvironmentEffectType.VETO);
        assertThat(veto.id()).isEqualTo("too_damp");
        assertThat(veto.amount()).isZero();
    }

    @Test
    void rejectsBlankEffectIdsAndNegativeSignalThresholds() {
        assertThatThrownBy(() -> EnvironmentEffects.modifyYield("", 0.1f))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.signalAtLeast("bookshelf", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.temperature(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullConditionArrays() {
        assertThatThrownBy(() -> EnvironmentConditions.allOf((EnvironmentCondition[]) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.anyOf((EnvironmentCondition[]) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidConditionArgumentsAtFactoryTime() {
        assertThatThrownBy(() -> EnvironmentConditions.signalAtLeast(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.signalAtLeast("", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.signalAbsent(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.signalAbsent(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.nearEntityAtLeast(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.airHazard(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.allOf((EnvironmentCondition) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.anyOf((EnvironmentCondition) null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
