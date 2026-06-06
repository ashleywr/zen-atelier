package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskModelTest {
    @Test
    void zeroRiskLeavesWeightsUnchanged() {
        assertThat(RiskModel.adjustedWeight(OutcomeClass.PERFECT_SUCCESS, 10, 0)).isEqualTo(10);
        assertThat(RiskModel.adjustedWeight(OutcomeClass.MESSY_FAILURE, 10, 0)).isEqualTo(10);
        assertThat(RiskModel.adjustedWeight(OutcomeClass.SUCCESS, 10, 0)).isEqualTo(10);
    }

    @Test
    void riskRaisesRareSuccessAndFailureWeights() {
        assertThat(RiskModel.adjustedWeight(OutcomeClass.PERFECT_SUCCESS, 10, 100)).isGreaterThan(10);
        assertThat(RiskModel.adjustedWeight(OutcomeClass.MUTATED_SUCCESS, 10, 100)).isGreaterThan(10);
        assertThat(RiskModel.adjustedWeight(OutcomeClass.UNSTABLE_SUCCESS, 10, 100)).isGreaterThan(10);
        assertThat(RiskModel.adjustedWeight(OutcomeClass.RECOVERABLE_FAILURE, 10, 100)).isGreaterThan(10);
        assertThat(RiskModel.adjustedWeight(OutcomeClass.CATASTROPHIC_FAILURE, 10, 100)).isGreaterThan(10);
    }

    @Test
    void riskDoesNotRaiseRoutineSuccessOrDudWeights() {
        assertThat(RiskModel.adjustedWeight(OutcomeClass.SUCCESS, 10, 100)).isEqualTo(10);
        assertThat(RiskModel.adjustedWeight(OutcomeClass.PARTIAL_SUCCESS, 10, 100)).isEqualTo(10);
        assertThat(RiskModel.adjustedWeight(OutcomeClass.DUD, 10, 100)).isEqualTo(10);
    }

    @Test
    void clampsRisk() {
        assertThat(RiskModel.adjustedWeight(OutcomeClass.PERFECT_SUCCESS, 10, 200))
                .isEqualTo(RiskModel.adjustedWeight(OutcomeClass.PERFECT_SUCCESS, 10, 100));
        assertThat(RiskModel.adjustedWeight(OutcomeClass.PERFECT_SUCCESS, 10, -20)).isEqualTo(10);
    }

    @Test
    void rejectsNonPositiveWeight() {
        assertThatThrownBy(() -> RiskModel.adjustedWeight(OutcomeClass.SUCCESS, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
