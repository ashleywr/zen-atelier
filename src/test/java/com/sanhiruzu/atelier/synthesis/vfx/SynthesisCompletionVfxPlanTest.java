package com.sanhiruzu.atelier.synthesis.vfx;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisCompletionVfxPlanTest {
    @Test
    void successPlanHasResonantCompletionLayers() {
        SynthesisCompletionVfxPlan plan = SynthesisCompletionVfxPlan.forOutcome(OutcomeClass.SUCCESS);

        assertThat(plan.corePulse()).isGreaterThanOrEqualTo(10);
        assertThat(plan.outerRing()).isGreaterThan(plan.innerRing());
        assertThat(plan.motes()).isGreaterThanOrEqualTo(24);
        assertThat(plan.liftSparks()).isGreaterThanOrEqualTo(12);
        assertThat(plan.afterglow()).isGreaterThanOrEqualTo(12);
        assertThat(plan.outerRadius()).isGreaterThan(plan.innerRadius());
        assertThat(plan.liftHeight()).isGreaterThan(0.4D);
    }

    @Test
    void perfectSuccessFeelsRicherThanNormalSuccess() {
        SynthesisCompletionVfxPlan normal = SynthesisCompletionVfxPlan.forOutcome(OutcomeClass.SUCCESS);
        SynthesisCompletionVfxPlan perfect = SynthesisCompletionVfxPlan.forOutcome(OutcomeClass.PERFECT_SUCCESS);

        assertThat(perfect.motes()).isGreaterThan(normal.motes());
        assertThat(perfect.outerRing()).isGreaterThan(normal.outerRing());
        assertThat(perfect.liftHeight()).isGreaterThan(normal.liftHeight());
    }

    @Test
    void unsuccessfulPlanStaysCompactForFailureFeedback() {
        SynthesisCompletionVfxPlan failure = SynthesisCompletionVfxPlan.forOutcome(OutcomeClass.RECOVERABLE_FAILURE);

        assertThat(failure.corePulse()).isLessThan(8);
        assertThat(failure.outerRing()).isZero();
        assertThat(failure.innerRing()).isZero();
        assertThat(failure.liftSparks()).isZero();
        assertThat(failure.afterglow()).isLessThanOrEqualTo(8);
    }

    @Test
    void failurePlansScaleBySeverityAndSalvage() {
        SynthesisCompletionVfxPlan dud = SynthesisCompletionVfxPlan.forOutcome(OutcomeClass.DUD);
        SynthesisCompletionVfxPlan recoverable = SynthesisCompletionVfxPlan.forOutcome(OutcomeClass.RECOVERABLE_FAILURE);
        SynthesisCompletionVfxPlan messy = SynthesisCompletionVfxPlan.forOutcome(OutcomeClass.MESSY_FAILURE);
        SynthesisCompletionVfxPlan catastrophic = SynthesisCompletionVfxPlan.forOutcome(OutcomeClass.CATASTROPHIC_FAILURE);

        assertThat(recoverable.salvageGlints()).isGreaterThan(dud.salvageGlints());
        assertThat(messy.smoke()).isGreaterThan(recoverable.smoke());
        assertThat(catastrophic.crackle()).isGreaterThan(messy.crackle());
        assertThat(catastrophic.smoke()).isGreaterThan(dud.smoke());
    }
}
