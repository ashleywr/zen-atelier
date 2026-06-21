package com.sanhiruzu.atelier.api;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentInfluenceResolverTest {
    private static final EnvironmentInfluenceProfile PROFILE = new EnvironmentInfluenceProfile(
            EnvironmentEffectType.COMFORT_MODIFIER,
            List.of(
                    new EnvironmentInfluenceCategory("shelter", "category.shelter", 0.35f, List.of("sleep_shelter")),
                    new EnvironmentInfluenceCategory("bedding", "category.bedding", 0.35f, List.of("sleep_bedding")),
                    new EnvironmentInfluenceCategory("decor", "category.decor", 0.35f, List.of("sleep_decor"))
            ),
            new EnvironmentInfluenceCategory("other", "category.other", 0.20f, List.of()),
            0.10f,
            List.of(0.45f, 0.90f, 1.25f)
    );

    @Test
    void repeatedSourcesAreCappedPerCategory() {
        EnvironmentInfluenceResult result = EnvironmentInfluenceResolver.resolve(List.of(
                EnvironmentEffects.modifyComfort("sleep_bedding_wool", 0.30f),
                EnvironmentEffects.modifyComfort("sleep_bedding_carpet", 0.30f),
                EnvironmentEffects.modifyComfort("sleep_bedding_blanket", 0.30f)
        ), PROFILE);

        assertThat(result.tier()).isZero();
        assertThat(result.category("bedding").contribution()).isEqualTo(0.35f);
        assertThat(result.category("bedding").rawContribution()).isEqualTo(0.90f);
        assertThat(result.category("bedding").capped()).isTrue();
        assertThat(result.category("bedding").diminished()).isTrue();
    }

    @Test
    void variedCategoriesReachHigherTierThanDuplicateCategory() {
        EnvironmentInfluenceResult result = EnvironmentInfluenceResolver.resolve(List.of(
                EnvironmentEffects.modifyComfort("sleep_shelter_roof", 0.30f),
                EnvironmentEffects.modifyComfort("sleep_bedding_wool", 0.30f),
                EnvironmentEffects.modifyComfort("sleep_decor_books", 0.30f)
        ), PROFILE);

        assertThat(result.tier()).isEqualTo(2);
        assertThat(result.score()).isEqualTo(1.10f);
        assertThat(result.activeCategoryCount()).isEqualTo(3);
    }

    @Test
    void uncategorizedEffectsUseFallbackCategory() {
        EnvironmentInfluenceResult result = EnvironmentInfluenceResolver.resolve(List.of(
                EnvironmentEffects.modifyComfort("modded_comfort", 0.50f)
        ), PROFILE);

        assertThat(result.category("other").contribution()).isEqualTo(0.20f);
        assertThat(result.category("other").capped()).isTrue();
        assertThat(result.category("other").effectIds()).containsExactly("modded_comfort");
    }

    @Test
    void ignoresMismatchedNegativeAndNullEffects() {
        EnvironmentInfluenceResult result = EnvironmentInfluenceResolver.resolve(Arrays.asList(
                EnvironmentEffects.modifySpeed("too_fast", 0.50f),
                EnvironmentEffects.modifyComfort("sleep_shelter_bad", -0.50f),
                null
        ), PROFILE);

        assertThat(result.score()).isZero();
        assertThat(result.tier()).isZero();
        assertThat(result.activeCategoryCount()).isZero();
    }

    @Test
    void thresholdsMustBeAscending() {
        assertThatThrownBy(() -> new EnvironmentInfluenceProfile(
                EnvironmentEffectType.COMFORT_MODIFIER,
                List.of(new EnvironmentInfluenceCategory("one", "category.one", 0.20f, List.of("one"))),
                null,
                0.0f,
                List.of(0.50f, 0.25f)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thresholds");
    }
}
