package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SynthesisRequirementMatcher {
    private SynthesisRequirementMatcher() {
    }

    public static ReagentQuery reagentQuery(ReagentQuery query) {
        return ReagentQuery.builder()
                .reagentIds(query.reagentIds())
                .minTier(query.minTier())
                .maxTier(query.maxTier())
                .minQuality(query.minQuality())
                .minPurity(query.minPurity())
                .maxInstability(query.maxInstability())
                .requiredCategories(query.requiredCategories())
                .requiredTraits(query.requiredTraits())
                .requiredSourceHints(query.requiredSourceHints())
                .build();
    }

    public static Map<String, Integer> elementBudget(List<SynthesisRequirement> requirements) {
        LinkedHashMap<String, Integer> budget = new LinkedHashMap<>();
        requirements.stream()
                .flatMap(requirement -> requirement.query().minElements().entrySet().stream())
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> budget.merge(entry.getKey(), entry.getValue(), Integer::sum));
        return Map.copyOf(budget);
    }

    public static Map<String, Integer> elementTotals(List<ReagentStack> reagents) {
        LinkedHashMap<String, Integer> totals = new LinkedHashMap<>();
        reagents.stream()
                .flatMap(reagent -> reagent.elements().entrySet().stream())
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> totals.merge(entry.getKey(), entry.getValue(), Integer::sum));
        return Map.copyOf(totals);
    }

    public static boolean elementBudgetSatisfied(List<SynthesisRequirement> requirements, List<ReagentStack> reagents) {
        Map<String, Integer> budget = elementBudget(requirements);
        if (budget.isEmpty()) {
            return true;
        }
        Map<String, Integer> totals = elementTotals(reagents);
        for (Map.Entry<String, Integer> required : budget.entrySet()) {
            if (totals.getOrDefault(required.getKey(), 0) < required.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static boolean contributesMissingElement(
            ReagentStack reagent,
            List<SynthesisRequirement> requirements,
            List<ReagentStack> currentInputs
    ) {
        Map<String, Integer> budget = elementBudget(requirements);
        if (budget.isEmpty()) {
            return false;
        }
        Map<String, Integer> totals = elementTotals(currentInputs);
        for (Map.Entry<String, Integer> required : budget.entrySet()) {
            int missing = required.getValue() - totals.getOrDefault(required.getKey(), 0);
            if (missing > 0 && reagent.elements().getOrDefault(required.getKey(), 0) > 0) {
                return true;
            }
        }
        return false;
    }
}
