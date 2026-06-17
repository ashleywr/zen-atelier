package com.sanhiruzu.atelier.ui.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.RequirementStatus;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirementMatcher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record SynthesisState(
        String profileId,
        String category,
        String displayName,
        boolean canSynthesize,
        boolean elementBudgetSatisfied,
        SynthesisDisplayModel display,
        List<RequirementLine> requirements,
        ElementState elements,
        BoardState board,
        PaletteState palette
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static SynthesisState empty() {
        return new SynthesisState(
                "",
                "",
                "",
                false,
                false,
                new SynthesisDisplayModel(List.of(), List.of(), List.of(), List.of()),
                List.of(),
                new ElementState(Map.of(), Map.of(), Map.of(), false),
                BoardState.EMPTY,
                PaletteState.EMPTY
        );
    }

    static SynthesisState minimal(SynthesisProfile profile, SynthesisPlan plan) {
        return new SynthesisState(
                profile.id(),
                profile.category(),
                SynthesisStationText.profileName(profile),
                plan.canSynthesize(),
                plan.elementBudgetSatisfied(),
                SynthesisDisplayModel.from(plan, List.of(), List.of(), List.of()),
                requirementLines(plan),
                elementState(profile.requirements(), List.of(), plan.elementBudgetSatisfied()),
                BoardState.EMPTY,
                PaletteState.EMPTY
        );
    }

    static SynthesisState fromProjection(
            SynthesisBoardProjection projection,
            SynthesisProfile profile,
            BoardState board,
            PaletteState palette
    ) {
        return new SynthesisState(
                profile.id(),
                profile.category(),
                SynthesisStationText.profileName(profile),
                projection.canSynthesize(),
                projection.elementBudgetSatisfied(),
                SynthesisDisplayModel.from(
                        projection.placedPlan().or(() -> projection.sourcePlan()).orElseThrow(),
                        projection.placedReagents().entries(),
                        activeTraitLines(board),
                        activeResonanceLines(board)
                ),
                projection.requirements(),
                elementState(profile.requirements(), projection.placedReagents().entries(), projection.elementBudgetSatisfied()),
                board,
                palette
        );
    }

    static List<RequirementLine> requirementLines(SynthesisPlan plan) {
        return plan.requirements().stream()
                .map(status -> new RequirementLine(
                        SynthesisStationText.summarizeQuery(status),
                        status.availableAmount(),
                        status.requirement().amount(),
                        status.missingAmount(),
                        status.satisfied(),
                        SynthesisStationText.queryQualifier(status.requirement().query())
                ))
                .toList();
    }

    static ElementState elementState(
            List<SynthesisRequirement> requirements,
            List<ReagentStack> currentInputs,
            boolean satisfied
    ) {
        Map<String, Integer> budget = sortedCopy(SynthesisRequirementMatcher.elementBudget(requirements));
        Map<String, Integer> totals = sortedCopy(SynthesisRequirementMatcher.elementTotals(currentInputs));
        LinkedHashMap<String, Integer> missing = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : budget.entrySet()) {
            int amount = entry.getValue() - totals.getOrDefault(entry.getKey(), 0);
            if (amount > 0) {
                missing.put(entry.getKey(), amount);
            }
        }
        return new ElementState(budget, totals, missing, satisfied);
    }

    String toDebugJson() {
        return GSON.toJson(this);
    }

    private static Map<String, Integer> sortedCopy(Map<String, Integer> source) {
        LinkedHashMap<String, Integer> sorted = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }

    private static List<String> activeTraitLines(BoardState board) {
        LinkedHashMap<String, Integer> totals = new LinkedHashMap<>();
        for (PlacedReagent reagent : board.placedReagents()) {
            for (String trait : reagent.traits()) {
                totals.merge(trait, 1, Integer::sum);
            }
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> SynthesisNoun.label(entry.getKey()) + " +" + entry.getValue())
                .toList();
    }

    private static List<String> activeResonanceLines(BoardState board) {
        if (!board.fusions().isEmpty()) {
            return board.fusions();
        }
        if (board.resonanceRisk() > 0) {
            return List.of("Risk +" + board.resonanceRisk());
        }
        return List.of();
    }

    record RequirementLine(
            String label,
            int available,
            int required,
            int missing,
            boolean satisfied,
            String qualifier
    ) {
    }

    record ElementState(
            Map<String, Integer> budget,
            Map<String, Integer> totals,
            Map<String, Integer> missing,
            boolean satisfied
    ) {
    }

    record BoardState(
            int occupiedCells,
            int totalCells,
            int emptyCells,
            int qualityBonus,
            int perfectBonus,
            int resonanceRisk,
            double successProbability,
            double perfectProbability,
            List<PlacedReagent> placedReagents,
            List<String> fusions
    ) {
        static final BoardState EMPTY = new BoardState(
                0,
                0,
                0,
                0,
                0,
                0,
                0.0D,
                0.0D,
                List.of(),
                List.of()
        );
    }

    record PlacedReagent(
            String id,
            int amount,
            int tier,
            int quality,
            int purity,
            int instability,
            List<String> categories,
            Map<String, Integer> elements,
            List<String> traits,
            List<String> cells
    ) {
    }

    record PaletteState(
            String source,
            boolean needNow,
            boolean fusionReady,
            boolean fitsBoard,
            String shapeFilter,
            List<PaletteEntryState> entries
    ) {
        static final PaletteState EMPTY = new PaletteState("", false, false, false, "", List.of());
    }

    record PaletteEntryState(
            String id,
            int amount,
            int placedAmount,
            int remainingAmount,
            int placedCopies,
            boolean needNow,
            boolean fusionReady,
            boolean fitsBoard,
            int shapeCells,
            List<String> reasons
    ) {
    }
}
