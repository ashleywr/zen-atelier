package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.RollTrace;

import java.util.List;
import java.util.SplittableRandom;

public final class ExtractionOutcome {
    private final OutcomeClass outcomeClass;
    private final int weight;
    private final List<ReagentRollTemplate> reagents;
    private final List<ReagentRollTemplate> byproducts;

    public ExtractionOutcome(
            OutcomeClass outcomeClass,
            int weight,
            List<ReagentStack> reagents,
            List<ReagentStack> byproducts
    ) {
        this(outcomeClass, weight, fixed(reagents), fixed(byproducts), true);
    }

    private ExtractionOutcome(
            OutcomeClass outcomeClass,
            int weight,
            List<ReagentRollTemplate> reagents,
            List<ReagentRollTemplate> byproducts,
            boolean ignored
    ) {
        if (outcomeClass == null) {
            throw new IllegalArgumentException("outcomeClass must not be null");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
        this.outcomeClass = outcomeClass;
        this.weight = weight;
        this.reagents = List.copyOf(reagents);
        this.byproducts = List.copyOf(byproducts);
    }

    public static ExtractionOutcome fromTemplates(
            OutcomeClass outcomeClass,
            int weight,
            List<ReagentRollTemplate> reagents,
            List<ReagentRollTemplate> byproducts
    ) {
        return new ExtractionOutcome(outcomeClass, weight, reagents, byproducts, true);
    }

    public OutcomeClass outcomeClass() {
        return outcomeClass;
    }

    public int weight() {
        return weight;
    }

    public List<ReagentStack> reagents() {
        return reagents.stream()
                .map(template -> template.roll(new SplittableRandom(0L), 1, template.tier()))
                .toList();
    }

    public List<ReagentStack> byproducts() {
        return byproducts.stream()
                .map(template -> template.roll(new SplittableRandom(0L), 1, template.tier()))
                .toList();
    }

    List<ReagentStack> rollReagents(SplittableRandom random, int sourceAmount, int tierCap, RollTrace.Builder trace) {
        return rollStacks(reagents, random, sourceAmount, tierCap, trace, "reagent");
    }

    List<ReagentStack> rollByproducts(SplittableRandom random, int sourceAmount, int tierCap, RollTrace.Builder trace) {
        return rollStacks(byproducts, random, sourceAmount, tierCap, trace, "byproduct");
    }

    private static List<ReagentStack> rollStacks(
            List<ReagentRollTemplate> templates,
            SplittableRandom random,
            int sourceAmount,
            int tierCap,
            RollTrace.Builder trace,
            String label
    ) {
        return templates.stream()
                .map(template -> {
                    ReagentStack rolled = template.roll(random, sourceAmount, tierCap);
                    trace.add(label
                            + " "
                            + rolled.reagentId()
                            + " amount "
                            + rolled.amount()
                            + " tier "
                            + rolled.tier()
                            + " quality "
                            + rolled.quality()
                            + " purity "
                            + rolled.purity()
                            + " instability "
                            + rolled.instability());
                    return rolled;
                })
                .toList();
    }

    private static List<ReagentRollTemplate> fixed(List<ReagentStack> stacks) {
        return stacks.stream().map(ReagentRollTemplate::fixed).toList();
    }
}
