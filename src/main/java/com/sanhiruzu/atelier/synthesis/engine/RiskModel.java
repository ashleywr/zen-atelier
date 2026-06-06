package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;

public final class RiskModel {
    private RiskModel() {
    }

    public static int adjustedWeight(OutcomeClass outcomeClass, int baseWeight, int risk) {
        if (baseWeight <= 0) {
            throw new IllegalArgumentException("baseWeight must be positive");
        }
        int clampedRisk = Math.clamp(risk, 0, 100);
        if (clampedRisk == 0) {
            return baseWeight;
        }

        return switch (outcomeClass) {
            case PERFECT_SUCCESS, MUTATED_SUCCESS -> baseWeight + bonus(baseWeight, clampedRisk, 50);
            case UNSTABLE_SUCCESS -> baseWeight + bonus(baseWeight, clampedRisk, 75);
            case RECOVERABLE_FAILURE, MESSY_FAILURE, CATASTROPHIC_FAILURE ->
                    baseWeight + bonus(baseWeight, clampedRisk, 25);
            case SUCCESS, PARTIAL_SUCCESS, DUD -> baseWeight;
        };
    }

    private static int bonus(int baseWeight, int risk, int divisor) {
        return Math.max(1, baseWeight * risk / divisor);
    }
}
