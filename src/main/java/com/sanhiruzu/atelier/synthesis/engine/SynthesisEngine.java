package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.RollTrace;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;

import java.util.List;
import java.util.SplittableRandom;

public final class SynthesisEngine {
    public SynthesisResult roll(SynthesisAttempt attempt) {
        RollTrace.Builder trace = RollTrace.builder()
                .add("synthesis profile " + attempt.profile().id())
                .add("risk " + attempt.risk());
        OutcomePreview preview = OutcomePreview.forSynthesis(attempt.profile().outcomes(), attempt.risk());
        trace.add("success probability " + formatProbability(preview.successProbability()));

        validateRequirements(attempt, trace);
        int effectiveCap = CapResolver.resolve(new CapContext(
                CapContext.UNBOUNDED,
                highestInputTier(attempt.reagents()),
                attempt.apparatusTierCap(),
                attempt.roomTierCap(),
                attempt.profile().recipeTierCap(),
                attempt.configTierCap()
        ), trace);

        SynthesisOutcome outcome = choose(attempt.profile().outcomes(), attempt.risk(), attempt.seed(), trace);
        List<SynthesisOutput> outputs = outcome.outputs().stream()
                .map(output -> output.cappedAtTier(effectiveCap))
                .toList();
        List<ReagentStack> byproducts = outcome.byproducts().stream()
                .map(byproduct -> byproduct.cappedAtTier(effectiveCap))
                .toList();

        trace.add("selected outcome " + outcome.outcomeClass().name().toLowerCase());
        return new SynthesisResult(outcome.outcomeClass(), outputs, byproducts, effectiveCap, trace.build());
    }

    private void validateRequirements(SynthesisAttempt attempt, RollTrace.Builder trace) {
        ReagentContainer available = new ReagentContainer();
        for (ReagentStack reagent : attempt.reagents()) {
            available.insert(reagent);
        }

        for (SynthesisRequirement requirement : attempt.profile().requirements()) {
            int availableAmount = available.totalAmount(requirement.query());
            trace.add("requirement amount " + availableAmount + " of " + requirement.amount());
            if (availableAmount < requirement.amount()) {
                throw new IllegalArgumentException("missing required reagents for " + attempt.profile().id());
            }
            if (available.extract(requirement.query(), requirement.amount()).isEmpty()) {
                throw new IllegalArgumentException("missing required reagents for " + attempt.profile().id());
            }
        }
    }

    private int highestInputTier(List<ReagentStack> reagents) {
        return reagents.stream()
                .mapToInt(ReagentStack::tier)
                .max()
                .orElse(1);
    }

    private SynthesisOutcome choose(List<SynthesisOutcome> outcomes, int risk, long seed, RollTrace.Builder trace) {
        int totalWeight = outcomes.stream()
                .mapToInt(outcome -> RiskModel.adjustedWeight(outcome.outcomeClass(), outcome.weight(), risk))
                .sum();
        int roll = new SplittableRandom(seed).nextInt(totalWeight);
        trace.add("weight roll " + roll + " of " + totalWeight);

        int cursor = 0;
        for (SynthesisOutcome outcome : outcomes) {
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
