package com.sanhiruzu.atelier.synthesis.storage;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReagentContainerTest {
    @Test
    void insertMergesOnlyIdenticalProfiles() {
        ReagentContainer container = new ReagentContainer();

        container.insert(fireReagent(40, 3, 70, 60, 25, List.of("volatile")));
        container.insert(fireReagent(60, 3, 70, 60, 25, List.of("volatile")));
        container.insert(fireReagent(10, 3, 70, 60, 25, List.of("scorching")));

        assertThat(container.entries()).hasSize(2);
        assertThat(container.entries().getFirst().amount()).isEqualTo(100);
        assertThat(container.entries().getLast().amount()).isEqualTo(10);
    }

    @Test
    void queryMatchesByTierPurityElementsTraitsAndSourceHints() {
        ReagentStack stack = fireReagent(50, 3, 70, 65, 20, List.of("volatile", "expansive"));
        ReagentQuery query = ReagentQuery.builder()
                .reagentIds(Set.of("zen_atelier:fire_reagent"))
                .minTier(3)
                .minQuality(60)
                .minPurity(60)
                .maxInstability(25)
                .minElements(Map.of("fire", 2))
                .requiredTraits(Set.of("volatile"))
                .requiredSourceHints(Set.of("minecraft:blaze_powder"))
                .build();

        assertThat(query.matches(stack)).isTrue();
        assertThat(query.requiredTraits()).containsExactly("volatile");
    }

    @Test
    void queryMatchesFunctionalCategoriesSeparatelyFromElements() {
        ReagentStack stack = new ReagentStack(
                "zen_atelier:crystalline_dust",
                Set.of("zen_atelier:conductive", "zen_atelier:abrasive"),
                1,
                2,
                45,
                60,
                0,
                Map.of("fire", 2, "wind", 1),
                List.of("zen_atelier:redstone_resonance"),
                com.sanhiruzu.atelier.synthesis.core.ReagentShape.LINE_TWO,
                Set.of("minecraft:redstone_block")
        );

        ReagentQuery conductiveFire = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:conductive"))
                .minElements(Map.of("fire", 2))
                .build();
        ReagentQuery bindingFire = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:binding"))
                .minElements(Map.of("fire", 2))
                .build();

        assertThat(conductiveFire.matches(stack)).isTrue();
        assertThat(bindingFire.matches(stack)).isFalse();
    }

    @Test
    void debugUniversalReagentMatchesAnyRequirement() {
        ReagentStack universal = new ReagentStack(
                ReagentQuery.DEBUG_UNIVERSAL_REAGENT_ID,
                256,
                1,
                0,
                0,
                100,
                Map.of(),
                List.of(),
                Set.of()
        );
        ReagentQuery strict = ReagentQuery.builder()
                .reagentIds(Set.of("zen_atelier:binding_reagent"))
                .minTier(6)
                .minQuality(100)
                .minPurity(100)
                .maxInstability(0)
                .minElements(Map.of("binding", 9))
                .requiredTraits(Set.of("preserving"))
                .requiredSourceHints(Set.of("minecraft:honey_bottle"))
                .build();

        assertThat(strict.matches(universal)).isTrue();
    }

    @Test
    void searchReturnsBestMatchesFirst() {
        ReagentContainer container = new ReagentContainer();
        ReagentStack weak = fireReagent(10, 1, 80, 90, 0, List.of());
        ReagentStack strong = fireReagent(10, 3, 40, 20, 0, List.of());
        ReagentStack pure = fireReagent(10, 3, 40, 80, 0, List.of());

        container.insert(weak);
        container.insert(strong);
        container.insert(pure);

        assertThat(container.search(ReagentQuery.any()))
                .extracting(ReagentStack::purity)
                .containsExactly(80, 20, 90);
    }

    @Test
    void extractIsAllOrNothingWhenInsufficient() {
        ReagentContainer container = new ReagentContainer();
        container.insert(fireReagent(40, 2, 50, 50, 10, List.of()));

        List<ReagentStack> extracted = container.extract(ReagentQuery.any(), 50);

        assertThat(extracted).isEmpty();
        assertThat(container.totalAmount(ReagentQuery.any())).isEqualTo(40);
    }

    @Test
    void extractSplitsAcrossMatchingEntries() {
        ReagentContainer container = new ReagentContainer();
        container.insert(fireReagent(40, 2, 50, 50, 10, List.of("volatile")));
        container.insert(fireReagent(70, 3, 60, 60, 10, List.of("volatile")));
        container.insert(fireReagent(80, 3, 60, 60, 10, List.of("scorching")));

        ReagentQuery volatileFire = ReagentQuery.builder()
                .requiredTraits(Set.of("volatile"))
                .build();
        List<ReagentStack> extracted = container.extract(volatileFire, 90);

        assertThat(extracted).extracting(ReagentStack::amount).containsExactly(70, 20);
        assertThat(container.totalAmount(volatileFire)).isEqualTo(20);
        assertThat(container.totalAmount(ReagentQuery.builder()
                .requiredTraits(Set.of("scorching"))
                .build())).isEqualTo(80);
    }

    @Test
    void extractUsesSameBestMatchOrderAsSearch() {
        ReagentContainer container = new ReagentContainer();
        ReagentStack weak = fireReagent(10, 1, 80, 90, 0, List.of("volatile"));
        ReagentStack strong = fireReagent(10, 3, 40, 20, 0, List.of("volatile"));
        ReagentStack pure = fireReagent(10, 3, 40, 80, 0, List.of("volatile"));
        ReagentQuery query = ReagentQuery.builder()
                .requiredTraits(Set.of("volatile"))
                .build();

        container.insert(weak);
        container.insert(strong);
        container.insert(pure);

        List<ReagentStack> preview = container.search(query);
        List<ReagentStack> extracted = container.extract(query, 20);

        assertThat(extracted)
                .extracting(ReagentStack::purity)
                .containsExactly(
                        preview.get(0).purity(),
                        preview.get(1).purity()
                );
        assertThat(container.entries()).singleElement()
                .extracting(ReagentStack::purity)
                .isEqualTo(90);
    }

    @Test
    void entriesViewIsImmutable() {
        ReagentContainer container = new ReagentContainer();
        container.insert(fireReagent(10, 1, 10, 10, 0, List.of()));

        assertThatThrownBy(() -> container.entries().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void extractRejectsNonPositiveAmount() {
        ReagentContainer container = new ReagentContainer();

        assertThatThrownBy(() -> container.extract(ReagentQuery.any(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ReagentStack fireReagent(
            int amount,
            int tier,
            int quality,
            int purity,
            int instability,
            List<String> traits
    ) {
        return new ReagentStack(
                "zen_atelier:fire_reagent",
                amount,
                tier,
                quality,
                purity,
                instability,
                Map.of("fire", tier),
                traits,
                Set.of("minecraft:blaze_powder")
        );
    }
}
