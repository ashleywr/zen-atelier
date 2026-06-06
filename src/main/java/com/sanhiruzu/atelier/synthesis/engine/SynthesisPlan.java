package com.sanhiruzu.atelier.synthesis.engine;

import java.util.List;

public record SynthesisPlan(
        SynthesisProfile profile,
        List<RequirementStatus> requirements,
        OutcomePreview preview
) {
    public SynthesisPlan {
        if (profile == null) {
            throw new IllegalArgumentException("profile must not be null");
        }
        requirements = List.copyOf(requirements);
        if (preview == null) {
            throw new IllegalArgumentException("preview must not be null");
        }
    }

    public boolean canSynthesize() {
        return requirements.stream().allMatch(RequirementStatus::satisfied);
    }
}
