package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.AttemptContext;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;

import java.util.ArrayList;
import java.util.List;

public final class ExtractionExecutor {
    private final ExtractionEngine engine = new ExtractionEngine();

    public ExtractionExecutionResult execute(
            ExtractionProfile profile,
            int sourceAmount,
            ReagentContainer target,
            AttemptContext context,
            long seed
    ) {
        ExtractionResult result = engine.roll(new ExtractionAttempt(profile, sourceAmount, context, seed));
        List<ReagentStack> deposited = new ArrayList<>();
        deposited.addAll(result.reagents());
        deposited.addAll(result.byproducts());

        for (ReagentStack stack : deposited) {
            target.insert(stack);
        }

        return new ExtractionExecutionResult(result, sourceAmount, deposited);
    }
}
