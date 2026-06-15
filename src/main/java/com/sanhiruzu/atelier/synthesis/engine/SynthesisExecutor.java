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
            consumed.addAll(container.extract(
                    SynthesisRequirementMatcher.reagentQuery(requirement.query()),
                    requirement.amount()));
        }
        consumed.addAll(extractElementSupport(profile, container, consumed));

        SynthesisResult result = engine.roll(new SynthesisAttempt(
                profile, consumed,
                context.apparatusTierCap(), context.roomTierCap(), context.configTierCap(),
                riskOverride, seed));
        return new SynthesisExecutionResult(result, consumed);
    }

    private static List<ReagentStack> extractElementSupport(
            SynthesisProfile profile,
            ReagentContainer container,
            List<ReagentStack> consumed
    ) {
        List<ReagentStack> support = new ArrayList<>();
        while (!SynthesisRequirementMatcher.elementBudgetSatisfied(profile.requirements(), combined(consumed, support))) {
            ReagentStack next = container.entries().stream()
                    .filter(stack -> SynthesisRequirementMatcher.contributesMissingElement(
                            stack, profile.requirements(), combined(consumed, support)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("missing required elements for " + profile.id()));
            List<ReagentStack> extracted = container.extract(exactSupportQuery(next), 1);
            if (extracted.isEmpty()) {
                throw new IllegalArgumentException("missing required elements for " + profile.id());
            }
            support.addAll(extracted);
        }
        return List.copyOf(support);
    }

    private static List<ReagentStack> combined(List<ReagentStack> first, List<ReagentStack> second) {
        ArrayList<ReagentStack> combined = new ArrayList<>(first.size() + second.size());
        combined.addAll(first);
        combined.addAll(second);
        return combined;
    }

    private static com.sanhiruzu.atelier.synthesis.storage.ReagentQuery exactSupportQuery(ReagentStack stack) {
        return com.sanhiruzu.atelier.synthesis.storage.ReagentQuery.builder()
                .reagentIds(java.util.Set.of(stack.reagentId()))
                .minTier(stack.tier())
                .maxTier(stack.tier())
                .minQuality(stack.quality())
                .minPurity(stack.purity())
                .maxInstability(stack.instability())
                .minElements(stack.elements())
                .requiredTraits(java.util.Set.copyOf(stack.traits()))
                .requiredSourceHints(stack.sourceHints())
                .build();
    }
}
