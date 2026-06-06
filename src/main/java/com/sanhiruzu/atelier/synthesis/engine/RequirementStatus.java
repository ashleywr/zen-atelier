package com.sanhiruzu.atelier.synthesis.engine;

public record RequirementStatus(
        SynthesisRequirement requirement,
        int availableAmount,
        int missingAmount,
        boolean satisfied
) {
    public RequirementStatus {
        if (requirement == null) {
            throw new IllegalArgumentException("requirement must not be null");
        }
        availableAmount = Math.max(0, availableAmount);
        missingAmount = Math.max(0, missingAmount);
    }
}
