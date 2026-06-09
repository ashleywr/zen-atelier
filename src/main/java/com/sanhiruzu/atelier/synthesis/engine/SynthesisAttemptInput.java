package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.AttemptContext;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;

public record SynthesisAttemptInput(
        SynthesisProfile effectiveProfile,
        ReagentContainer reagents,
        AttemptContext context,
        ResolvedFusionData fusion
) {
    public SynthesisAttemptInput {
        if (effectiveProfile == null) throw new IllegalArgumentException("effectiveProfile must not be null");
        if (reagents == null) throw new IllegalArgumentException("reagents must not be null");
        if (context == null) throw new IllegalArgumentException("context must not be null");
        if (fusion == null) throw new IllegalArgumentException("fusion must not be null");
    }

    public int effectiveRisk() {
        return Math.clamp(context.risk() + fusion.resonanceCount() * 15, 0, 100);
    }
}
