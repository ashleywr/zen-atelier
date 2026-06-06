package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;

import java.util.List;

public record ExtractionExecutionResult(
        ExtractionResult result,
        int consumedSourceAmount,
        List<ReagentStack> depositedReagents
) {
    public ExtractionExecutionResult {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        if (consumedSourceAmount <= 0) {
            throw new IllegalArgumentException("consumedSourceAmount must be positive");
        }
        depositedReagents = List.copyOf(depositedReagents);
    }
}
