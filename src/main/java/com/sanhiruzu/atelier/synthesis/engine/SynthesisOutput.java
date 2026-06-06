package com.sanhiruzu.atelier.synthesis.engine;

import java.util.List;

public record SynthesisOutput(
        String outputId,
        int count,
        int tier,
        int quality,
        List<String> affixes
) {
    public SynthesisOutput {
        if (outputId == null || outputId.isBlank()) {
            throw new IllegalArgumentException("outputId must not be blank");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        tier = Math.clamp(tier, 1, 6);
        quality = Math.clamp(quality, 0, 100);
        affixes = List.copyOf(affixes);
    }

    public SynthesisOutput cappedAtTier(int tierCap) {
        return new SynthesisOutput(outputId, count, Math.min(tier, tierCap), quality, affixes);
    }
}
