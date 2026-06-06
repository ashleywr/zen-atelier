package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.RollTrace;

import java.util.List;

public record SynthesisResult(
        OutcomeClass outcomeClass,
        List<SynthesisOutput> outputs,
        List<ReagentStack> byproducts,
        int effectiveTierCap,
        RollTrace trace
) {
    public SynthesisResult {
        if (outcomeClass == null) {
            throw new IllegalArgumentException("outcomeClass must not be null");
        }
        outputs = List.copyOf(outputs);
        byproducts = List.copyOf(byproducts);
    }

    public boolean successful() {
        return outcomeClass.successful();
    }
}
