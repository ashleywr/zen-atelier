package com.sanhiruzu.atelier.synthesis.menu;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisResult;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisStationMenuTest {
    @Test
    void creativeSynthesisDoesNotAwardByproducts() {
        SynthesisResult result = new SynthesisResult(
                OutcomeClass.RECOVERABLE_FAILURE,
                List.of(),
                List.of(ReagentStack.simple("zen_atelier:sticky_residue", 10, 1)),
                1,
                com.sanhiruzu.atelier.synthesis.core.RollTrace.builder().build()
        );

        assertThat(SynthesisStationMenu.awardableByproducts(result, true)).isEmpty();
        assertThat(SynthesisStationMenu.awardableByproducts(result, false))
                .extracting(ReagentStack::reagentId)
                .containsExactly("zen_atelier:sticky_residue");
    }

    @Test
    void serverRejectsPlacedPayloadThatExceedsAvailableReagents() {
        ReagentContainer available = new ReagentContainer();
        ReagentStack reagent = ReagentStack.simple("zen_atelier:test_reagent", 10, 1);
        available.insert(reagent);

        assertThat(SynthesisStationMenu.placedAvailableInForTest(
                available,
                List.of(reagent, reagent)
        )).isFalse();
    }
}
