package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisPlannerTest {
    private final SynthesisPlanner planner = new SynthesisPlanner();

    @Test
    void reportsSatisfiedRequirementsAndOdds() {
        ReagentContainer container = new ReagentContainer();
        container.insert(reagent("zen_atelier:abrasive_reagent", 40, "sharp"));
        container.insert(reagent("zen_atelier:binding_reagent", 30, "binding"));

        SynthesisPlan plan = planner.plan(sampleProfile(), container, 0);

        assertThat(plan.canSynthesize()).isTrue();
        assertThat(plan.requirements()).extracting(RequirementStatus::satisfied).containsExactly(true, true);
        assertThat(plan.preview().successProbability()).isGreaterThan(0.0);
    }

    @Test
    void reportsMissingAmount() {
        ReagentContainer container = new ReagentContainer();
        container.insert(reagent("zen_atelier:abrasive_reagent", 20, "sharp"));

        SynthesisPlan plan = planner.plan(sampleProfile(), container, 0);

        assertThat(plan.canSynthesize()).isFalse();
        assertThat(plan.requirements()).extracting(RequirementStatus::missingAmount).containsExactly(10, 20);
    }

    @Test
    void doesNotReuseSameReagentForOverlappingRequirements() {
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:overlap",
                List.of(
                        new SynthesisRequirement(ReagentQuery.any(), 50),
                        new SynthesisRequirement(ReagentQuery.any(), 50)
                ),
                2,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:output", 1, 1, 1, List.of())),
                        List.of()
                ))
        );
        ReagentContainer container = new ReagentContainer();
        container.insert(reagent("zen_atelier:any_reagent", 60, "generic"));

        SynthesisPlan plan = planner.plan(profile, container, 0);

        assertThat(plan.canSynthesize()).isFalse();
        assertThat(plan.requirements()).extracting(RequirementStatus::availableAmount).containsExactly(60, 10);
        assertThat(plan.requirements()).extracting(RequirementStatus::missingAmount).containsExactly(0, 40);
    }

    private static SynthesisProfile sampleProfile() {
        return new SynthesisProfile(
                "zen_atelier:crude_mining_coating",
                List.of(
                        new SynthesisRequirement(ReagentQuery.builder()
                                .reagentIds(Set.of("zen_atelier:abrasive_reagent"))
                                .minElements(Map.of("sharp", 1))
                                .build(), 30),
                        new SynthesisRequirement(ReagentQuery.builder()
                                .reagentIds(Set.of("zen_atelier:binding_reagent"))
                                .minElements(Map.of("binding", 1))
                                .build(), 20)
                ),
                2,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:crude_mining_coating", 1, 1, 40, List.of())),
                        List.of()
                ))
        );
    }

    private static ReagentStack reagent(String id, int amount, String element) {
        return new ReagentStack(
                id,
                amount,
                1,
                30,
                50,
                0,
                Map.of(element, 2),
                List.of(),
                Set.of()
        );
    }
}
