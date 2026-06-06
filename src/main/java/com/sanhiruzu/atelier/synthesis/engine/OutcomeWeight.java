package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;

public record OutcomeWeight(
        OutcomeClass outcomeClass,
        int baseWeight,
        int adjustedWeight,
        double probability
) {
    public OutcomeWeight {
        if (outcomeClass == null) {
            throw new IllegalArgumentException("outcomeClass must not be null");
        }
        if (baseWeight <= 0) {
            throw new IllegalArgumentException("baseWeight must be positive");
        }
        if (adjustedWeight <= 0) {
            throw new IllegalArgumentException("adjustedWeight must be positive");
        }
        probability = Math.clamp(probability, 0.0, 1.0);
    }
}
