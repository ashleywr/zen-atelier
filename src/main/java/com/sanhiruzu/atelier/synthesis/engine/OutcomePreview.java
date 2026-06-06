package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public record OutcomePreview(List<OutcomeWeight> weights) {
    public OutcomePreview {
        weights = List.copyOf(weights);
    }

    public static OutcomePreview forExtraction(List<ExtractionOutcome> outcomes, int risk) {
        return build(outcomes, ExtractionOutcome::outcomeClass, ExtractionOutcome::weight, risk);
    }

    public static OutcomePreview forSynthesis(List<SynthesisOutcome> outcomes, int risk) {
        return build(outcomes, SynthesisOutcome::outcomeClass, SynthesisOutcome::weight, risk);
    }

    public double probabilityOf(OutcomeClass outcomeClass) {
        return weights.stream()
                .filter(weight -> weight.outcomeClass() == outcomeClass)
                .mapToDouble(OutcomeWeight::probability)
                .sum();
    }

    public double successProbability() {
        return weights.stream()
                .filter(weight -> weight.outcomeClass().successful())
                .mapToDouble(OutcomeWeight::probability)
                .sum();
    }

    public double failureProbability() {
        return 1.0 - successProbability();
    }

    private static <T> OutcomePreview build(
            List<T> outcomes,
            Function<T, OutcomeClass> outcomeClass,
            ToIntFunction<T> weight,
            int risk
    ) {
        if (outcomes.isEmpty()) {
            throw new IllegalArgumentException("outcomes must not be empty");
        }
        List<Integer> adjusted = outcomes.stream()
                .map(outcome -> RiskModel.adjustedWeight(outcomeClass.apply(outcome), weight.applyAsInt(outcome), risk))
                .toList();
        int total = adjusted.stream().mapToInt(Integer::intValue).sum();

        List<OutcomeWeight> preview = new ArrayList<>();
        for (int i = 0; i < outcomes.size(); i++) {
            T outcome = outcomes.get(i);
            int baseWeight = weight.applyAsInt(outcome);
            int adjustedWeight = adjusted.get(i);
            preview.add(new OutcomeWeight(
                    outcomeClass.apply(outcome),
                    baseWeight,
                    adjustedWeight,
                    adjustedWeight / (double) total
            ));
        }
        return new OutcomePreview(preview);
    }
}
