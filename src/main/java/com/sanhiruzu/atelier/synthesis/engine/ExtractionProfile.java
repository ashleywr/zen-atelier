package com.sanhiruzu.atelier.synthesis.engine;

import java.util.List;

public record ExtractionProfile(
        String id,
        String sourceKey,
        int sourceTierCap,
        List<ExtractionOutcome> outcomes
) {
    public ExtractionProfile {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (sourceKey == null || sourceKey.isBlank()) {
            throw new IllegalArgumentException("sourceKey must not be blank");
        }
        if (sourceTierCap <= 0) {
            throw new IllegalArgumentException("sourceTierCap must be positive");
        }
        if (outcomes.isEmpty()) {
            throw new IllegalArgumentException("outcomes must not be empty");
        }
        outcomes = List.copyOf(outcomes);
    }
}
