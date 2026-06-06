package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.RollTrace;

import java.util.List;
import java.util.SplittableRandom;

public final class ExtractionEngine {
    public ExtractionResult roll(ExtractionAttempt attempt) {
        RollTrace.Builder trace = RollTrace.builder()
                .add("extraction profile " + attempt.profile().id())
                .add("source amount " + attempt.sourceAmount())
                .add("risk " + attempt.risk());
        OutcomePreview preview = OutcomePreview.forExtraction(attempt.profile().outcomes(), attempt.risk());
        trace.add("success probability " + formatProbability(preview.successProbability()));

        int effectiveCap = CapResolver.resolve(new CapContext(
                attempt.profile().sourceTierCap(),
                CapContext.UNBOUNDED,
                attempt.apparatusTierCap(),
                attempt.roomTierCap(),
                CapContext.UNBOUNDED,
                attempt.configTierCap()
        ), trace);

        SplittableRandom random = new SplittableRandom(attempt.seed());
        ExtractionOutcome outcome = choose(attempt.profile().outcomes(), attempt.risk(), random, trace);
        List<ReagentStack> reagents = outcome.rollReagents(random, attempt.sourceAmount(), effectiveCap, trace);
        List<ReagentStack> byproducts = outcome.rollByproducts(random, attempt.sourceAmount(), effectiveCap, trace);

        trace.add("selected outcome " + outcome.outcomeClass().name().toLowerCase());
        return new ExtractionResult(outcome.outcomeClass(), reagents, byproducts, effectiveCap, trace.build());
    }

    private ExtractionOutcome choose(List<ExtractionOutcome> outcomes, int risk, SplittableRandom random, RollTrace.Builder trace) {
        int totalWeight = outcomes.stream()
                .mapToInt(outcome -> RiskModel.adjustedWeight(outcome.outcomeClass(), outcome.weight(), risk))
                .sum();
        int roll = random.nextInt(totalWeight);
        trace.add("weight roll " + roll + " of " + totalWeight);

        int cursor = 0;
        for (ExtractionOutcome outcome : outcomes) {
            cursor += RiskModel.adjustedWeight(outcome.outcomeClass(), outcome.weight(), risk);
            if (roll < cursor) {
                return outcome;
            }
        }
        return outcomes.getLast();
    }

    private static String formatProbability(double probability) {
        return String.format(java.util.Locale.ROOT, "%.4f", probability);
    }
}
