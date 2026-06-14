package com.sanhiruzu.atelier.space.analyze;

public final class CandidateResolver {
    private static final double ACCEPT_INDOOR_THRESHOLD = 6.0;
    private static final double ACCEPT_SHELTERED_THRESHOLD = 3.5;
    private static final int MAX_OPEN_TERRAIN_CELLS = 2000;

    private CandidateResolver() {}

    public static CandidateDecision resolve(EvidenceScore score, ZoneCandidate candidate) {
        double total = score.total();

        if (candidate.totalWalkableCells() > MAX_OPEN_TERRAIN_CELLS && candidate.shelterFraction() < 0.1) {
            return CandidateDecision.REJECT_TOO_LARGE_OPEN_AIR;
        }
        if (score.naturalCavePenalty() >= 3.0 && !candidate.hasPortalAccess()) {
            return CandidateDecision.REJECT_LOW_CONFIDENCE;
        }
        if (total >= ACCEPT_INDOOR_THRESHOLD && candidate.hasPortalAccess()) {
            return CandidateDecision.ACCEPT_INDOOR;
        }
        if (total >= ACCEPT_SHELTERED_THRESHOLD && candidate.hasPortalAccess()) {
            return CandidateDecision.ACCEPT_SHELTERED;
        }
        if (!candidate.hasPortalAccess() && total >= ACCEPT_INDOOR_THRESHOLD) {
            return CandidateDecision.PENDING_STABILITY;
        }
        if (total < 1.0) {
            return CandidateDecision.REJECT_LOW_CONFIDENCE;
        }
        return CandidateDecision.PENDING_STABILITY;
    }
}
