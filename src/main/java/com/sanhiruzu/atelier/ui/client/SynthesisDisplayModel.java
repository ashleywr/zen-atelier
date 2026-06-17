package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirementMatcher;

import java.util.List;
import java.util.Map;

record SynthesisDisplayModel(
        List<Line> essences,
        List<Line> elements,
        List<TextLine> traits,
        List<TextLine> resonance
) {
    static SynthesisDisplayModel from(
            SynthesisPlan plan,
            List<ReagentStack> currentInputs,
            List<String> traitLines,
            List<String> resonanceLines
    ) {
        List<Line> essences = plan.requirements().stream()
                .map(status -> new Line(
                        SynthesisStationText.summarizeQuery(status),
                        status.availableAmount(),
                        status.requirement().amount(),
                        status.satisfied(),
                        true
                ))
                .toList();
        Map<String, Integer> elementBudget = SynthesisRequirementMatcher.elementBudget(plan.profile().requirements());
        Map<String, Integer> elementTotals = SynthesisRequirementMatcher.elementTotals(currentInputs);
        List<Line> elements = elementBudget.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    int available = elementTotals.getOrDefault(entry.getKey(), 0);
                    return new Line(
                            SynthesisNoun.label(entry.getKey()),
                            available,
                            entry.getValue(),
                            available >= entry.getValue(),
                            true
                    );
                })
                .toList();
        return new SynthesisDisplayModel(
                essences,
                elements,
                textLinesOrDefault(traitLines, "None active"),
                textLinesOrDefault(resonanceLines, "None")
        );
    }

    private static List<TextLine> textLinesOrDefault(List<String> lines, String emptyText) {
        if (lines.isEmpty()) {
            return List.of(new TextLine(emptyText, false));
        }
        return lines.stream()
                .map(line -> new TextLine(line, true))
                .toList();
    }

    record Line(String label, int available, int required, boolean satisfied, boolean requiredBlocker) {
    }

    record TextLine(String text, boolean active) {
    }
}
