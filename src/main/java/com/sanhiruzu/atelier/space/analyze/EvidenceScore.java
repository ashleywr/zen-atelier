package com.sanhiruzu.atelier.space.analyze;

public record EvidenceScore(
        double floorScore,
        double shelterScore,
        double boundaryScore,
        double accessScore,
        double humanBuiltScore,
        double furnitureScore,
        double openTerrainPenalty,
        double naturalCavePenalty,
        String primaryReason
) {
    public double total() {
        return floorScore + shelterScore + boundaryScore + accessScore
             + humanBuiltScore + furnitureScore
             - openTerrainPenalty - naturalCavePenalty;
    }
}
