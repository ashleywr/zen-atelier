package com.sanhiruzu.atelier.synthesis.core;

public record ApparatusState(
        String apparatusId,
        int tierCap,
        int stabilityBonus
) {
    public ApparatusState {
        if (apparatusId == null || apparatusId.isBlank()) {
            throw new IllegalArgumentException("apparatusId must not be blank");
        }
        tierCap = Math.clamp(tierCap, 1, 6);
        stabilityBonus = Math.clamp(stabilityBonus, -100, 100);
    }

    public static ApparatusState crude(String apparatusId) {
        return new ApparatusState(apparatusId, 1, 0);
    }
}
