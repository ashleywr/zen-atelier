package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutcomePreviewTest {
    @Test
    void reportsExtractionProbabilities() {
        OutcomePreview preview = OutcomePreview.forExtraction(List.of(
                new ExtractionOutcome(OutcomeClass.SUCCESS, 80, List.of(), List.of()),
                new ExtractionOutcome(OutcomeClass.PERFECT_SUCCESS, 5, List.of(), List.of()),
                new ExtractionOutcome(OutcomeClass.RECOVERABLE_FAILURE, 15, List.of(), List.of())
        ), 0);

        assertThat(preview.successProbability()).isCloseTo(0.85, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(preview.failureProbability()).isCloseTo(0.15, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(preview.probabilityOf(OutcomeClass.PERFECT_SUCCESS)).isCloseTo(0.05, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void riskRaisesRareAndFailureProbabilityShare() {
        List<SynthesisOutcome> outcomes = List.of(
                new SynthesisOutcome(OutcomeClass.SUCCESS, 80, List.of(), List.of()),
                new SynthesisOutcome(OutcomeClass.PERFECT_SUCCESS, 5, List.of(), List.of()),
                new SynthesisOutcome(OutcomeClass.RECOVERABLE_FAILURE, 15, List.of(), List.of())
        );

        OutcomePreview safe = OutcomePreview.forSynthesis(outcomes, 0);
        OutcomePreview risky = OutcomePreview.forSynthesis(outcomes, 100);

        assertThat(risky.probabilityOf(OutcomeClass.PERFECT_SUCCESS))
                .isGreaterThan(safe.probabilityOf(OutcomeClass.PERFECT_SUCCESS));
        assertThat(risky.probabilityOf(OutcomeClass.RECOVERABLE_FAILURE))
                .isGreaterThan(safe.probabilityOf(OutcomeClass.RECOVERABLE_FAILURE));
        assertThat(risky.probabilityOf(OutcomeClass.SUCCESS))
                .isLessThan(safe.probabilityOf(OutcomeClass.SUCCESS));
    }

    @Test
    void rejectsEmptyOutcomeTables() {
        assertThatThrownBy(() -> OutcomePreview.forExtraction(List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
