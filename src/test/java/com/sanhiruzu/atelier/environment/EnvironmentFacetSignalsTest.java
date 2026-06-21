package com.sanhiruzu.atelier.environment;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentFacetSignalsTest {
    @BeforeAll
    static void prepareMinecraftBlocks() {
        if (LoadingModList.get() == null) {
            LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void resetFacetSignals() {
        EnvironmentFacetSignals.clearProvidersForTest();
        EnvironmentFacetSignals.replaceMappings(EnvironmentFacetMappings.empty());
    }

    @Test
    void mappingsConvertFacetsToEnvironmentSignals() {
        EnvironmentFacetMappings mappings = EnvironmentFacetMappings.of(Map.of(
                "ami:decor.soft", Set.of("sleep_soft_bedding"),
                "ami:decor.plant", Set.of("plant", "sleep_natural_accent")
        ));

        assertThat(mappings.signalsFor(Set.of("ami:decor.plant", "ami:unknown")))
                .containsExactlyInAnyOrder("plant", "sleep_natural_accent");
    }

    @Test
    void registeredProvidersContributeMappedSignalsForBlockStates() {
        EnvironmentFacetSignals.replaceMappings(EnvironmentFacetMappings.of(Map.of(
                "ami:decor.soft", Set.of("sleep_soft_bedding"),
                "ami:material.wool", Set.of("wool")
        )));
        EnvironmentFacetSignals.registerProvider(state -> state.is(Blocks.WHITE_WOOL)
                ? Set.of("ami:decor.soft", "ami:material.wool")
                : Set.of());

        assertThat(EnvironmentFacetSignals.signalsFor(Blocks.WHITE_WOOL.defaultBlockState()))
                .containsExactlyInAnyOrder("sleep_soft_bedding", "wool");
        assertThat(EnvironmentFacetSignals.signalsFor(Blocks.STONE.defaultBlockState())).isEmpty();
    }

    @Test
    void mappingsRejectBlankFacetOrSignalNames() {
        assertThatThrownBy(() -> EnvironmentFacetMappings.of(Map.of("", Set.of("plant"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentFacetMappings.of(Map.of("ami:decor.plant", Set.of(""))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
