package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlanner;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

record SynthesisBoardProjection(
        Optional<SynthesisPlan> sourcePlan,
        Optional<SynthesisPlan> placedPlan,
        ReagentContainer placedReagents,
        List<SynthesisState.RequirementLine> requirements,
        boolean canSynthesize,
        boolean elementBudgetSatisfied,
        List<ReagentStack> payloadReagents,
        double successProbability,
        double perfectProbability
) {
    static SynthesisBoardProjection from(
            SynthesisBoardSession session,
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents
    ) {
        return fromPlacedReagents(
                plan,
                session.placements().stream()
                        .map(placement -> placement.piece().reagent())
                        .toList(),
                storageReagents,
                inventoryReagents
        );
    }

    static SynthesisBoardProjection fromPlacedReagents(
            Optional<SynthesisPlan> plan,
            List<ReagentStack> placedReagents,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents
    ) {
        if (plan.isEmpty()) {
            return empty();
        }
        ReagentContainer placed = boundedPlacedReagents(placedReagents, storageReagents, inventoryReagents);
        SynthesisPlan placedPlan = new SynthesisPlanner().plan(plan.get().profile(), placed, 0);
        boolean hasPlaced = !placed.entries().isEmpty();
        return new SynthesisBoardProjection(
                plan,
                Optional.of(placedPlan),
                placed,
                SynthesisState.requirementLines(placedPlan),
                hasPlaced && placedPlan.canSynthesize(),
                placedPlan.elementBudgetSatisfied(),
                placed.entries(),
                hasPlaced ? placedPlan.preview().successProbability() : 0.0D,
                hasPlaced ? placedPlan.preview().probabilityOf(OutcomeClass.PERFECT_SUCCESS) : 0.0D
        );
    }

    static List<ReagentStack> payloadReagents(
            List<ReagentStack> placedReagents,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents
    ) {
        return boundedPlacedReagents(placedReagents, storageReagents, inventoryReagents).entries();
    }

    static SynthesisBoardProjection empty() {
        return new SynthesisBoardProjection(
                Optional.empty(),
                Optional.empty(),
                new ReagentContainer(),
                List.of(),
                false,
                false,
                List.of(),
                0.0D,
                0.0D
        );
    }

    private static ReagentContainer boundedPlacedReagents(
            List<ReagentStack> placedReagents,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents
    ) {
        ArrayList<ReagentStack> available = new ArrayList<>(storageReagents.size() + inventoryReagents.size());
        available.addAll(storageReagents);
        available.addAll(inventoryReagents);

        ReagentContainer bounded = new ReagentContainer();
        ArrayList<ReagentStack> accepted = new ArrayList<>();
        for (ReagentStack reagent : placedReagents) {
            int remainingAvailable = totalAmountForProfile(available, reagent) - totalAmountForProfile(accepted, reagent);
            if (remainingAvailable <= 0) {
                continue;
            }
            ReagentStack capped = reagent.withAmount(Math.min(reagent.amount(), remainingAvailable));
            accepted.add(capped);
            bounded.insert(capped);
        }
        return bounded;
    }

    private static int totalAmountForProfile(List<ReagentStack> reagents, ReagentStack profile) {
        return reagents.stream()
                .filter(reagent -> sameReagentProfile(reagent, profile))
                .mapToInt(ReagentStack::amount)
                .sum();
    }

    static boolean sameReagentProfile(ReagentStack left, ReagentStack right) {
        return left.reagentId().equals(right.reagentId())
                && left.categories().equals(right.categories())
                && left.tier() == right.tier()
                && left.quality() == right.quality()
                && left.purity() == right.purity()
                && left.instability() == right.instability()
                && left.elements().equals(right.elements())
                && left.traits().equals(right.traits())
                && left.shape().equals(right.shape())
                && left.sourceHints().equals(right.sourceHints());
    }
}
