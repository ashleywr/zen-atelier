package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;

import java.util.List;

public record SynthesisExecutionResult(
        SynthesisResult result,
        List<ReagentStack> consumedReagents
) {
    public SynthesisExecutionResult {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        consumedReagents = List.copyOf(consumedReagents);
    }
}
