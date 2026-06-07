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

    public SynthesisOutput withAddedAffixes(List<String> extra) {
        if (extra.isEmpty()) {
            return this;
        }
        List<String> merged = new java.util.ArrayList<>(affixes);
        for (String a : extra) {
            if (!merged.contains(a)) {
                merged.add(a);
            }
        }
        return new SynthesisOutput(outputId, count, tier, quality, merged);
    }

    public SynthesisOutput withBoostedQuality(int delta) {
        if (delta == 0) {
            return this;
        }
        return new SynthesisOutput(outputId, count, tier, Math.clamp(quality + delta, 0, 100), affixes);
    }
}
