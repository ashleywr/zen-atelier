package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.AttemptContext;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;

import java.util.List;

public record SynthesisAttempt(
        SynthesisProfile profile,
        List<ReagentStack> reagents,
        int apparatusTierCap,
        int roomTierCap,
        int configTierCap,
        int risk,
        long seed
) {
    public SynthesisAttempt(
            SynthesisProfile profile,
            List<ReagentStack> reagents,
            AttemptContext context,
            long seed
    ) {
        this(profile, reagents, context.apparatusTierCap(), context.roomTierCap(), context.configTierCap(), context.risk(), seed);
    }

    public SynthesisAttempt(
            SynthesisProfile profile,
            List<ReagentStack> reagents,
            int apparatusTierCap,
            int roomTierCap,
            int configTierCap,
            long seed
    ) {
        this(profile, reagents, apparatusTierCap, roomTierCap, configTierCap, 0, seed);
    }

    public SynthesisAttempt {
        if (profile == null) {
            throw new IllegalArgumentException("profile must not be null");
        }
        reagents = List.copyOf(reagents);
        risk = Math.clamp(risk, 0, 100);
    }
}
