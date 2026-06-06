package com.sanhiruzu.atelier.synthesis.menu;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutcome;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisStationMenuTest {
    @Test
    void recipeTiersUnlockWithAtelierProgression() {
        SynthesisProfile tier3 = profile("zen_atelier:tier3", 3);
        SynthesisProfile tier4 = profile("zen_atelier:tier4", 4);

        assertThat(SynthesisStationMenu.maxRecipeTier(SynthesisStationMenu.ROOM_CONTEXT_OUTSIDE)).isEqualTo(2);
        assertThat(SynthesisStationMenu.maxRecipeTier(SynthesisStationMenu.ROOM_CONTEXT_INDOOR)).isEqualTo(2);
        assertThat(SynthesisStationMenu.maxRecipeTier(SynthesisStationMenu.ROOM_CONTEXT_ATELIER)).isEqualTo(3);
        assertThat(SynthesisStationMenu.maxRecipeTier(SynthesisStationMenu.ROOM_CONTEXT_FINE_ATELIER)).isEqualTo(4);

        assertThat(SynthesisStationMenu.recipeUnlocked(tier3, SynthesisStationMenu.ROOM_CONTEXT_INDOOR)).isFalse();
        assertThat(SynthesisStationMenu.recipeUnlocked(tier3, SynthesisStationMenu.ROOM_CONTEXT_ATELIER)).isTrue();
        assertThat(SynthesisStationMenu.recipeUnlocked(tier4, SynthesisStationMenu.ROOM_CONTEXT_ATELIER)).isFalse();
        assertThat(SynthesisStationMenu.recipeUnlocked(tier4, SynthesisStationMenu.ROOM_CONTEXT_FINE_ATELIER)).isTrue();
    }

    @Test
    void roomContextReweightsOutcomes() {
        SynthesisProfile base = profile("zen_atelier:odds", 2);

        SynthesisProfile outside = SynthesisStationMenu.effectiveProfile(base, SynthesisStationMenu.ROOM_CONTEXT_OUTSIDE);
        assertThat(weight(outside, OutcomeClass.SUCCESS)).isEqualTo(1);
        assertThat(weight(outside, OutcomeClass.DUD)).isGreaterThan(weight(base, OutcomeClass.DUD));

        SynthesisProfile atelier = SynthesisStationMenu.effectiveProfile(base, SynthesisStationMenu.ROOM_CONTEXT_ATELIER);
        assertThat(weight(atelier, OutcomeClass.SUCCESS)).isEqualTo(weight(base, OutcomeClass.SUCCESS) * 2);
        assertThat(weight(atelier, OutcomeClass.DUD)).isLessThan(weight(base, OutcomeClass.DUD));

        SynthesisProfile fine = SynthesisStationMenu.effectiveProfile(base, SynthesisStationMenu.ROOM_CONTEXT_FINE_ATELIER);
        assertThat(weight(fine, OutcomeClass.SUCCESS)).isEqualTo(weight(base, OutcomeClass.SUCCESS) * 3);
        assertThat(weight(fine, OutcomeClass.DUD)).isLessThan(weight(atelier, OutcomeClass.DUD));
    }

    private static int weight(SynthesisProfile profile, OutcomeClass outcomeClass) {
        return profile.outcomes().stream()
                .filter(outcome -> outcome.outcomeClass() == outcomeClass)
                .findFirst()
                .map(SynthesisOutcome::weight)
                .orElseThrow();
    }

    private static SynthesisProfile profile(String id, int tier) {
        return new SynthesisProfile(
                id,
                List.of(new SynthesisRequirement(ReagentQuery.any(), 10)),
                tier,
                List.of(
                        new SynthesisOutcome(
                                OutcomeClass.SUCCESS,
                                100,
                                List.of(new SynthesisOutput("zen_atelier:test_output", 1, 1, 10, List.of())),
                                List.of()
                        ),
                        new SynthesisOutcome(
                                OutcomeClass.DUD,
                                30,
                                List.of(),
                                List.of()
                        )
                )
        );
    }
}
