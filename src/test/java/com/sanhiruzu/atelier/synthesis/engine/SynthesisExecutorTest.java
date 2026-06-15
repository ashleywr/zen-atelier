package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.ApparatusState;
import com.sanhiruzu.atelier.synthesis.core.AttemptContext;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.RoomAlchemyContext;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SynthesisExecutorTest {
    private final SynthesisExecutor executor = new SynthesisExecutor();

    @Test
    void consumesRequiredReagentsAndReturnsResult() {
        ReagentContainer container = new ReagentContainer();
        container.insert(reagent("zen_atelier:abrasive_reagent", 40, "sharp"));
        container.insert(reagent("zen_atelier:binding_reagent", 30, "binding"));

        SynthesisExecutionResult execution = executor.execute(sampleProfile(), container, context(), 1L);

        assertThat(execution.result().outputs()).singleElement()
                .extracting(SynthesisOutput::outputId)
                .isEqualTo("zen_atelier:crude_mining_coating");
        assertThat(execution.consumedReagents()).extracting(ReagentStack::amount).containsExactly(30, 20);
        assertThat(container.totalAmount(ReagentQuery.any())).isEqualTo(20);
    }

    @Test
    void insufficientReagentsDoNotMutateContainer() {
        ReagentContainer container = new ReagentContainer();
        container.insert(reagent("zen_atelier:abrasive_reagent", 20, "sharp"));

        assertThatThrownBy(() -> executor.execute(sampleProfile(), container, context(), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required reagents");
        assertThat(container.totalAmount(ReagentQuery.any())).isEqualTo(20);
    }

    @Test
    void executionRespectsContextCaps() {
        ReagentContainer container = new ReagentContainer();
        container.insert(reagent("zen_atelier:abrasive_reagent", 40, "sharp"));
        container.insert(reagent("zen_atelier:binding_reagent", 30, "binding"));

        SynthesisExecutionResult execution = executor.execute(sampleProfile(), container, new AttemptContext(
                new ApparatusState("zen_atelier:crude_cauldron", 1, 0),
                RoomAlchemyContext.none(),
                6,
                0
        ), 1L);

        assertThat(execution.result().effectiveTierCap()).isEqualTo(1);
    }

    @Test
    void consumesElementSupportSeparatelyFromFamilyRequirements() {
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:separate_elements",
                List.of(
                        new SynthesisRequirement(ReagentQuery.builder()
                                .requiredCategories(Set.of("zen_atelier:organic"))
                                .minElements(Map.of("water", 1))
                                .build(), 35),
                        new SynthesisRequirement(ReagentQuery.builder()
                                .requiredCategories(Set.of("zen_atelier:binding"))
                                .minElements(Map.of("water", 1))
                                .build(), 20)
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
        container.insert(reagentWithCategories("zen_atelier:taun_herb", 35, Set.of("zen_atelier:organic"), Map.of()));
        container.insert(reagentWithCategories("zen_atelier:string", 20, Set.of("zen_atelier:binding"), Map.of()));
        container.insert(reagentWithCategories("zen_atelier:aqua_gel", 2, Set.of("zen_atelier:filler"), Map.of("water", 2)));

        SynthesisExecutionResult execution = executor.execute(profile, container, context(), 1L);

        assertThat(execution.consumedReagents()).extracting(ReagentStack::reagentId)
                .containsExactly("zen_atelier:taun_herb", "zen_atelier:string", "zen_atelier:aqua_gel");
        assertThat(execution.consumedReagents()).extracting(ReagentStack::amount)
                .containsExactly(35, 20, 1);
        assertThat(container.totalAmount(ReagentQuery.any())).isEqualTo(1);
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
                        List.of(new SynthesisOutput("zen_atelier:crude_mining_coating", 1, 2, 40, List.of())),
                        List.of()
                ))
        );
    }

    private static ReagentStack reagent(String id, int amount, String element) {
        return new ReagentStack(id, amount, 2, 30, 50, 0, Map.of(element, 2), List.of(), Set.of());
    }

    private static ReagentStack reagentWithCategories(String id, int amount, Set<String> categories, Map<String, Integer> elements) {
        return new ReagentStack(
                id,
                categories,
                amount,
                1,
                30,
                50,
                0,
                elements,
                List.of(),
                com.sanhiruzu.atelier.synthesis.core.ReagentShape.SINGLE,
                Set.of()
        );
    }

    private static AttemptContext context() {
        return new AttemptContext(
                new ApparatusState("zen_atelier:copper_cauldron", 2, 0),
                RoomAlchemyContext.none(),
                2,
                0
        );
    }
}
