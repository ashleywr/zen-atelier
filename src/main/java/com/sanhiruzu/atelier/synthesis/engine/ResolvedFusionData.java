package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.data.TraitFusionRule;

import java.util.LinkedHashSet;
import java.util.List;

public record ResolvedFusionData(
        List<String> fusedAffixes,
        int qualityBonus,
        int successWeightBonus,
        int resonanceCount
) {
    private static final int MAX_BOARD_RESONANCE = 49;
    public static final ResolvedFusionData EMPTY = new ResolvedFusionData(List.of(), 0, 0, 0);

    public ResolvedFusionData {
        fusedAffixes = List.copyOf(fusedAffixes);
        qualityBonus = Math.clamp(qualityBonus, 0, 100);
        successWeightBonus = Math.max(0, successWeightBonus);
        resonanceCount = Math.max(0, resonanceCount);
    }

    public static ResolvedFusionData fromRules(List<TraitFusionRule> uniqueRules, int rawResonanceCount) {
        if (uniqueRules.isEmpty()) {
            return EMPTY;
        }
        LinkedHashSet<String> affixes = new LinkedHashSet<>();
        int qualityBonus = 0;
        int successWeightBonus = 0;
        for (TraitFusionRule rule : uniqueRules) {
            rule.outputAffix().ifPresent(affixes::add);
            qualityBonus += rule.qualityBonus();
            successWeightBonus += rule.successWeightBonus();
        }
        int resonanceCount = Math.min(Math.clamp(rawResonanceCount, 0, MAX_BOARD_RESONANCE), uniqueRules.size() * 2);
        return new ResolvedFusionData(List.copyOf(affixes), qualityBonus, successWeightBonus, resonanceCount);
    }
}
