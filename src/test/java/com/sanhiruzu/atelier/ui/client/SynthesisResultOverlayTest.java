package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisResultOverlayTest {
    @Test
    void failedOutcomesUseSmokeImpactDelay() {
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.RECOVERABLE_FAILURE))
                .isEqualTo(SynthesisResultOverlay.FAILURE_IMPACT_TICKS);
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.MESSY_FAILURE))
                .isEqualTo(SynthesisResultOverlay.FAILURE_IMPACT_TICKS);
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.DUD))
                .isEqualTo(SynthesisResultOverlay.FAILURE_IMPACT_TICKS);
    }

    @Test
    void successfulOutcomesShowResultImmediately() {
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.SUCCESS)).isZero();
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.PERFECT_SUCCESS)).isZero();
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.UNSTABLE_SUCCESS)).isZero();
    }
}
