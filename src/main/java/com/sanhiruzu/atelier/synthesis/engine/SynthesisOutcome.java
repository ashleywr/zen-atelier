package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;

import java.util.List;

public record SynthesisOutcome(
        OutcomeClass outcomeClass,
        int weight,
        List<SynthesisOutput> outputs,
        List<ReagentStack> byproducts
) {
    public SynthesisOutcome {
        if (outcomeClass == null) {
            throw new IllegalArgumentException("outcomeClass must not be null");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
        outputs = List.copyOf(outputs);
        byproducts = List.copyOf(byproducts);
    }
}
