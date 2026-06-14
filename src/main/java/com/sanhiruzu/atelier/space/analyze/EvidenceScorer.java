package com.sanhiruzu.atelier.space.analyze;

public final class EvidenceScorer {
    private static final int OPEN_TERRAIN_THRESHOLD = 500;
    private static final int LARGE_OPEN_THRESHOLD = 2000;

    private EvidenceScorer() {}

    public static EvidenceScore score(ZoneCandidate c) {
        double floorScore   = Math.min(1.0, c.totalWalkableCells() / 100.0) * 2.0;
        double shelterScore = c.shelterFraction() * 3.0;
        double humanBuilt   = c.playerBuiltFraction() * 2.5;
        double furniture    = Math.min(1.0, c.totalFurnitureCount() / 5.0) * 2.0;
        double access       = c.hasPortalAccess() ? 2.0 : 0.0;
        double boundary     = c.playerBuiltFraction() * c.shelterFraction() * 1.5;

        double openPenalty = 0.0;
        if (c.shelterFraction() < 0.2 && c.totalWalkableCells() > OPEN_TERRAIN_THRESHOLD) {
            openPenalty = 4.0 + (c.totalWalkableCells() > LARGE_OPEN_THRESHOLD ? 4.0 : 0.0);
        }
        double cavePenalty = 0.0;
        if (c.naturalFraction() > 0.8 && c.playerBuiltFraction() < 0.1 && c.totalFurnitureCount() == 0) {
            cavePenalty = 3.0;
        }

        String primaryReason = derivePrimaryReason(c, shelterScore, humanBuilt, openPenalty, cavePenalty);

        return new EvidenceScore(floorScore, shelterScore, boundary, access,
                humanBuilt, furniture, openPenalty, cavePenalty, primaryReason);
    }

    private static String derivePrimaryReason(ZoneCandidate c, double shelter, double humanBuilt,
                                               double openPenalty, double cavePenalty) {
        if (openPenalty > 0) return "open terrain — too large and unenclosed";
        if (cavePenalty > 0) return "natural cave — no player modification";
        if (!c.hasPortalAccess()) return "no portal access (no door/gate/trapdoor)";
        if (shelter > 2.0 && humanBuilt > 1.5) return "enclosed player-built space";
        if (shelter > 1.0) return "partially sheltered space";
        return "low shelter and boundary evidence";
    }
}
