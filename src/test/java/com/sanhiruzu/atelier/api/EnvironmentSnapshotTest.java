package com.sanhiruzu.atelier.api;

import net.minecraft.SharedConstants;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentSnapshotTest {
    @BeforeAll
    static void prepareMinecraftEntityTypes() {
        if (LoadingModList.get() == null) {
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void exposesSignalEntityAndDerivedFacts() {
        EnvironmentSnapshot snapshot = new EnvironmentSnapshot(
                Map.of("bookshelf", 3, "carpet", 1),
                true,
                true,
                false,
                EnvironmentTemperatureBand.PLEASANT,
                Set.of(EnvironmentAirHazard.SMOKY),
                Map.of(EntityType.CAT, 2)
        );

        assertThat(snapshot.countSignal("bookshelf")).isEqualTo(3);
        assertThat(snapshot.hasSignal("carpet")).isTrue();
        assertThat(snapshot.hasSignalAtLeast("bookshelf", 2)).isTrue();
        assertThat(snapshot.hasSignalAtLeast("bookshelf", 4)).isFalse();
        assertThat(snapshot.isCovered()).isTrue();
        assertThat(snapshot.isIndoorsLike()).isTrue();
        assertThat(snapshot.isEnclosed()).isFalse();
        assertThat(snapshot.temperatureBand()).isEqualTo(EnvironmentTemperatureBand.PLEASANT);
        assertThat(snapshot.hasAirHazard(EnvironmentAirHazard.SMOKY)).isTrue();
        assertThat(snapshot.nearEntity(EntityType.CAT)).isTrue();
        assertThat(snapshot.nearEntityAtLeast(EntityType.CAT, 2)).isTrue();
        assertThat(snapshot.nearEntityAtLeast(EntityType.FROG, 1)).isFalse();
    }

    @Test
    void clampsNegativeCountsAndRejectsInvalidArguments() {
        EnvironmentSnapshot snapshot = new EnvironmentSnapshot(
                Map.of("dirt_like", -4),
                false,
                false,
                false,
                EnvironmentTemperatureBand.COOL,
                Set.of(),
                Map.of(EntityType.FROG, -2)
        );

        assertThat(snapshot.countSignal("dirt_like")).isZero();
        assertThat(snapshot.nearEntityAtLeast(EntityType.FROG, 1)).isFalse();
        assertThatThrownBy(() -> snapshot.countSignal(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot.hasSignalAtLeast("carpet", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot.nearEntityAtLeast(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
