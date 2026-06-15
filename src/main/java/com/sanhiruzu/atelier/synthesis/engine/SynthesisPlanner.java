package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;

import java.util.ArrayList;
import java.util.List;

public final class SynthesisPlanner {
    public SynthesisPlan plan(SynthesisAttemptInput input) {
        return plan(input.effectiveProfile(), input.reagents(), input.effectiveRisk());
    }

    public SynthesisPlan plan(SynthesisProfile profile, ReagentContainer container, int risk) {
        ReagentContainer remaining = new ReagentContainer();
        for (var entry : container.entries()) {
            remaining.insert(entry);
        }

        List<RequirementStatus> statuses = new ArrayList<>();
        for (SynthesisRequirement requirement : profile.requirements()) {
            var reagentQuery = SynthesisRequirementMatcher.reagentQuery(requirement.query());
            int available = remaining.totalAmount(reagentQuery);
            int missing = Math.max(0, requirement.amount() - available);
            boolean satisfied = missing == 0 && !remaining.extract(reagentQuery, requirement.amount()).isEmpty();
            statuses.add(new RequirementStatus(requirement, available, missing, satisfied));
        }

        return new SynthesisPlan(
                profile,
                statuses,
                OutcomePreview.forSynthesis(profile.outcomes(), risk),
                SynthesisRequirementMatcher.elementBudgetSatisfied(profile.requirements(), container.entries())
        );
    }
}
