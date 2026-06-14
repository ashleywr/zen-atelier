package com.sanhiruzu.atelier.space.analyze;

public enum CandidateDecision {
    ACCEPT_INDOOR,
    ACCEPT_SHELTERED,
    ACCEPT_OUTDOOR_FUNCTIONAL,
    PENDING_NEIGHBOR,
    PENDING_STABILITY,
    REJECT_LOW_CONFIDENCE,
    REJECT_TOO_LARGE_OPEN_AIR
}
