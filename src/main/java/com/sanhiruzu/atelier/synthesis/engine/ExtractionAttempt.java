package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.AttemptContext;

public record ExtractionAttempt(
        ExtractionProfile profile,
        int sourceAmount,
        int apparatusTierCap,
        int roomTierCap,
        int configTierCap,
        int risk,
        long seed
) {
    public ExtractionAttempt(
            ExtractionProfile profile,
            int sourceAmount,
            AttemptContext context,
            long seed
    ) {
        this(profile, sourceAmount, context.apparatusTierCap(), context.roomTierCap(), context.configTierCap(), context.risk(), seed);
    }

    public ExtractionAttempt(
            ExtractionProfile profile,
            int sourceAmount,
            int apparatusTierCap,
            int roomTierCap,
            int configTierCap,
            long seed
    ) {
        this(profile, sourceAmount, apparatusTierCap, roomTierCap, configTierCap, 0, seed);
    }

    public ExtractionAttempt {
        if (profile == null) {
            throw new IllegalArgumentException("profile must not be null");
        }
        if (sourceAmount <= 0) {
            throw new IllegalArgumentException("sourceAmount must be positive");
        }
        risk = Math.clamp(risk, 0, 100);
    }
}
