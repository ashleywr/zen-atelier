package com.sanhiruzu.atelier.synthesis.data;

import java.util.Optional;

public record TraitFusionRule(
        String id,
        String traitA,
        String traitB,
        Optional<String> outputAffix,
        int qualityBonus,
        int successWeightBonus,
        int color
) {
    public TraitFusionRule {
        // Normalize pair so traitA <= traitB lexicographically
        if (traitA.compareTo(traitB) > 0) {
            String tmp = traitA;
            traitA = traitB;
            traitB = tmp;
        }
        qualityBonus = Math.clamp(qualityBonus, 0, 100);
        successWeightBonus = Math.max(0, successWeightBonus);
    }

    public String pairKey() {
        return TraitFusionRegistry.pairKey(traitA, traitB);
    }
}
