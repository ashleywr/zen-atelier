package com.sanhiruzu.atelier.synthesis.vfx;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;

public record SynthesisCompletionVfxPlan(
        int corePulse,
        int innerRing,
        int outerRing,
        int motes,
        int liftSparks,
        int afterglow,
        double innerRadius,
        double outerRadius,
        double liftHeight,
        int smoke,
        int salvageGlints,
        int crackle
) {
    public static SynthesisCompletionVfxPlan forOutcome(OutcomeClass outcomeClass) {
        if (!outcomeClass.successful()) {
            return switch (outcomeClass) {
                case DUD -> new SynthesisCompletionVfxPlan(2, 0, 0, 3, 0, 3, 0.0D, 0.0D, 0.0D, 10, 0, 0);
                case RECOVERABLE_FAILURE -> new SynthesisCompletionVfxPlan(6, 0, 0, 8, 0, 8, 0.0D, 0.0D, 0.0D, 24, 8, 1);
                case MESSY_FAILURE -> new SynthesisCompletionVfxPlan(7, 0, 0, 10, 0, 8, 0.0D, 0.0D, 0.0D, 38, 3, 4);
                case CATASTROPHIC_FAILURE -> new SynthesisCompletionVfxPlan(7, 0, 0, 12, 0, 8, 0.0D, 0.0D, 0.0D, 54, 0, 12);
                default -> new SynthesisCompletionVfxPlan(6, 0, 0, 8, 0, 8, 0.0D, 0.0D, 0.0D, 24, 4, 2);
            };
        }

        int bonus = switch (outcomeClass) {
            case PERFECT_SUCCESS -> 10;
            case MUTATED_SUCCESS, UNSTABLE_SUCCESS -> 5;
            case PARTIAL_SUCCESS -> -4;
            default -> 0;
        };
        return new SynthesisCompletionVfxPlan(
                12 + Math.max(0, bonus / 2),
                18 + Math.max(0, bonus),
                28 + Math.max(0, bonus + 2),
                28 + Math.max(0, bonus * 2),
                14 + Math.max(0, bonus),
                14 + Math.max(0, bonus),
                0.34D,
                0.62D + Math.max(0, bonus) * 0.012D,
                0.56D + Math.max(0, bonus) * 0.018D,
                0,
                0,
                0
        );
    }
}
