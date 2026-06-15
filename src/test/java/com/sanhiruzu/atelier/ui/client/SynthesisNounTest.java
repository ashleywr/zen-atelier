package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutcome;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.RequirementStatus;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlanner;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.engine.OutcomePreview;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisNounTest {
    @Test
    void findsNounsFromNamespacedIdsAndPlainAliases() {
        assertThat(SynthesisNoun.find("water")).contains(SynthesisNoun.WATER);
        assertThat(SynthesisNoun.find("zen_atelier:binding")).contains(SynthesisNoun.BINDING);
        assertThat(SynthesisNoun.find("zen_atelier:spark_reagent")).contains(SynthesisNoun.SPARK);
        assertThat(SynthesisNoun.find("zen_atelier:sparking")).contains(SynthesisNoun.SPARK);
    }

    @Test
    void importantPrototypeNounsHaveStableEmotiveColors() {
        assertThat(SynthesisNoun.WATER.color()).isEqualTo(0xFF76B7E8);
        assertThat(SynthesisNoun.BINDING.color()).isEqualTo(0xFFB06CD7);
        assertThat(SynthesisNoun.SPARK.color()).isEqualTo(0xFFFFC857);
    }

    @Test
    void synthesisNeedsPreferRecipeFamiliesOverElementQualifiers() {
        ReagentQuery query = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:binding"))
                .minElements(Map.of("water", 1))
                .build();
        RequirementStatus status = new RequirementStatus(new SynthesisRequirement(query, 20), 12, 8, false);

        assertThat(SynthesisStationText.summarizeQuery(status)).isEqualTo("Binding");
        assertThat(SynthesisStationText.requirementLine(status)).isEqualTo("12/20 Binding");
        assertThat(SynthesisStationText.elementBudget(query)).isEqualTo("Water 1");
    }

    @Test
    void synthesisElementBudgetCombinesRecipeElementRequirements() {
        ReagentQuery wateryOrganic = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:organic"))
                .minElements(Map.of("water", 1))
                .build();
        ReagentQuery wateryBinding = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:binding"))
                .minElements(Map.of("water", 1))
                .build();
        ReagentQuery earthy = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:earthy"))
                .minElements(Map.of("earth", 1))
                .build();

        assertThat(SynthesisStationText.compactElementBudget(java.util.List.of(wateryOrganic, wateryBinding, earthy)))
                .isEqualTo("Earth 1, Water 2");
    }

    @Test
    void synthesisStateDebugJsonSeparatesRequirementsFromElements() {
        ReagentQuery wateryOrganic = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:organic"))
                .minElements(Map.of("water", 1))
                .build();
        ReagentQuery wateryBinding = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:binding"))
                .minElements(Map.of("water", 1))
                .build();
        SynthesisRequirement organicRequirement = new SynthesisRequirement(wateryOrganic, 35);
        SynthesisRequirement bindingRequirement = new SynthesisRequirement(wateryBinding, 20);
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:frost_globe",
                "bombs",
                List.of(organicRequirement, bindingRequirement),
                2,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:frost_globe", 1, 1, 1, List.of())),
                        List.of()
                ))
        );
        SynthesisPlan plan = new SynthesisPlan(
                profile,
                List.of(
                        new RequirementStatus(organicRequirement, 35, 0, true),
                        new RequirementStatus(bindingRequirement, 5, 15, false)
                ),
                OutcomePreview.forSynthesis(profile.outcomes(), 0),
                false
        );

        String json = SynthesisState.minimal(profile, plan).toDebugJson();

        assertThat(json).contains("\"label\": \"Organic\"");
        assertThat(json).contains("\"label\": \"Binding\"");
        assertThat(json).contains("\"budget\": {\n      \"water\": 2\n    }");
    }

    @Test
    void emptyStoragePaletteDoesNotExposeGeneratedDebugStock() {
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:test",
                List.of(new SynthesisRequirement(ReagentQuery.any(), 1)),
                2,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:test_output", 1, 1, 1, List.of())),
                        List.of()
                ))
        );
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, new ReagentContainer(), 0);

        SynthesisState state = new SynthesisSpatialPrototype().buildState(
                java.util.Optional.of(plan),
                List.of(),
                List.of()
        );

        assertThat(state.palette().entries()).isEmpty();
    }

    @Test
    void repeatedPlacementOfOneAvailableReagentDoesNotInflateDebugAvailability() {
        ReagentStack reagent = new ReagentStack(
                "zen_atelier:small_reagent",
                10,
                1,
                30,
                50,
                0,
                Map.of("water", 1),
                List.of(),
                Set.of()
        );
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:needs_more_than_one_stack",
                List.of(new SynthesisRequirement(ReagentQuery.any(), 15)),
                2,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:test_output", 1, 1, 1, List.of())),
                        List.of()
                ))
        );
        ReagentContainer available = new ReagentContainer();
        available.insert(reagent);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, available, 0);
        SynthesisSpatialPrototype spatial = new SynthesisSpatialPrototype();

        spatial.mouseClicked(14, 246, 0, false, java.util.Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));
        spatial.mouseClicked(183, 85, 0, false, java.util.Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));
        spatial.mouseClicked(14, 246, 0, false, java.util.Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));
        spatial.mouseClicked(201, 85, 0, false, java.util.Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));

        SynthesisState state = spatial.buildState(java.util.Optional.of(plan), List.of(reagent), List.of());

        assertThat(state.canSynthesize()).isFalse();
        assertThat(state.requirements()).first()
                .extracting(SynthesisState.RequirementLine::available)
                .isEqualTo(10);
    }
}
