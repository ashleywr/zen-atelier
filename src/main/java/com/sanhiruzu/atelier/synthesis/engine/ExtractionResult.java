package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.RollTrace;

import java.util.List;

public record ExtractionResult(
        OutcomeClass outcomeClass,
        List<ReagentStack> reagents,
        List<ReagentStack> byproducts,
        int effectiveTierCap,
        RollTrace trace
) {
    public ExtractionResult {
        reagents = List.copyOf(reagents);
        byproducts = List.copyOf(byproducts);
    }

    public boolean successful() {
        return outcomeClass.successful();
    }
}
