package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.AttemptContext;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;

import java.util.ArrayList;
import java.util.List;

public final class SynthesisExecutor {
    private final SynthesisEngine engine = new SynthesisEngine();
    private final SynthesisPlanner planner = new SynthesisPlanner();

    public SynthesisExecutionResult execute(SynthesisAttemptInput input, long seed) {
        return execute(input.effectiveProfile(), input.reagents(), input.context(), input.effectiveRisk(), seed);
    }

    public SynthesisExecutionResult execute(
            SynthesisProfile profile,
            ReagentContainer container,
            AttemptContext context,
            long seed
    ) {
        return execute(profile, container, context, context.risk(), seed);
    }

    public SynthesisExecutionResult execute(
            SynthesisProfile profile,
            ReagentContainer container,
            AttemptContext context,
            int riskOverride,
            long seed
    ) {
        SynthesisPlan plan = planner.plan(profile, container, riskOverride);
        if (!plan.canSynthesize()) {
            throw new IllegalArgumentException("missing required reagents for " + profile.id());
        }

        List<ReagentStack> consumed = new ArrayList<>();
        for (SynthesisRequirement requirement : profile.requirements()) {
            consumed.addAll(container.extract(requirement.query(), requirement.amount()));
        }

        SynthesisResult result = engine.roll(new SynthesisAttempt(
                profile, consumed,
                context.apparatusTierCap(), context.roomTierCap(), context.configTierCap(),
                riskOverride, seed));
        return new SynthesisExecutionResult(result, consumed);
    }
}
