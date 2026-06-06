package com.sanhiruzu.atelier.integration.emi;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmiRecipeIdsTest {
    @Test
    void syntheticPrefixesPathForViewerOnlyRecipes() {
        ResourceLocation id = EmiRecipeIds.synthetic(ResourceLocation.fromNamespaceAndPath("zen_atelier", "amethyst_shard"));

        assertThat(id.getNamespace()).isEqualTo("zen_atelier");
        assertThat(id.getPath()).isEqualTo("/amethyst_shard");
    }

    @Test
    void syntheticDoesNotDoublePrefixAlreadySyntheticPath() {
        ResourceLocation id = EmiRecipeIds.synthetic(ResourceLocation.fromNamespaceAndPath("zen_atelier", "/info/synthesis_station"));

        assertThat(id.getPath()).isEqualTo("/info/synthesis_station");
    }
}
