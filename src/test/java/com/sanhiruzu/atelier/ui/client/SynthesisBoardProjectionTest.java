package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutcome;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlanner;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisBoardProjectionTest {
    @Test
    void craftabilityRequirementsAndPayloadUseSameBoundedPlacedReagents() {
        ReagentStack available = aquaGel(56);
        SynthesisProfile profile = profile(
                new SynthesisRequirement(ReagentQuery.builder()
                        .requiredCategories(Set.of("zen_atelier:organic"))
                        .minElements(Map.of("water", 1))
                        .build(), 35),
                new SynthesisRequirement(ReagentQuery.builder()
                        .requiredCategories(Set.of("zen_atelier:binding"))
                        .minElements(Map.of("water", 1))
                        .build(), 20)
        );
        ReagentContainer container = new ReagentContainer();
        container.insert(available);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, container, 0);
        SynthesisBoardSession session = new SynthesisBoardSession();

        session.addDebugPlacement(available, 0, 0);
        session.addDebugPlacement(available, 1, 0);

        SynthesisBoardProjection projection = SynthesisBoardProjection.from(
                session,
                Optional.of(plan),
                List.of(available),
                List.of()
        );

        assertThat(projection.placedReagents().entries()).singleElement()
                .extracting(ReagentStack::amount)
                .isEqualTo(56);
        assertThat(projection.payloadReagents()).isEqualTo(projection.placedReagents().entries());
        assertThat(projection.canSynthesize()).isTrue();
        assertThat(projection.requirements()).extracting(SynthesisState.RequirementLine::available)
                .containsExactly(56, 21);
    }

    @Test
    void debugStateUsesProjectionRequirementsAndCraftability() {
        ReagentStack reagent = ReagentStack.simple("zen_atelier:test_reagent", 20, 1);
        SynthesisProfile profile = profile(new SynthesisRequirement(ReagentQuery.any(), 20));
        ReagentContainer container = new ReagentContainer();
        container.insert(reagent);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, container, 0);
        SynthesisBoardSession session = new SynthesisBoardSession();
        session.addDebugPlacement(reagent, 0, 0);

        SynthesisBoardProjection projection = SynthesisBoardProjection.from(
                session,
                Optional.of(plan),
                List.of(reagent),
                List.of()
        );
        SynthesisState state = SynthesisState.fromProjection(
                projection,
                plan.profile(),
                SynthesisState.BoardState.EMPTY,
                SynthesisState.PaletteState.EMPTY
        );

        assertThat(state.canSynthesize()).isEqualTo(projection.canSynthesize());
        assertThat(state.requirements()).isEqualTo(projection.requirements());
        assertThat(state.elements().totals()).isEmpty();
    }

    static ReagentStack aquaGel(int amount) {
        return new ReagentStack(
                "zen_atelier:aqua_gel_reagent",
                Set.of("zen_atelier:organic", "zen_atelier:binding"),
                amount,
                1,
                45,
                52,
                16,
                Map.of("water", 2),
                List.of("zen_atelier:sticky"),
                ReagentShape.SQUARE_TWO,
                Set.of("zen_atelier:aqua_gel")
        );
    }

    static SynthesisProfile profile(SynthesisRequirement... requirements) {
        return new SynthesisProfile(
                "zen_atelier:projection_test",
                List.of(requirements),
                2,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:test_output", 1, 1, 1, List.of())),
                        List.of()
                ))
        );
    }
}
