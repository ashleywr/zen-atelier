package com.sanhiruzu.atelier.space.zone;

/**
 * Lifecycle action chosen after comparing a zone's previous player-facing data
 * with the latest scan/evaluation result.
 */
enum ZoneReconciliationAction {
    ACTIVATE,
    UPDATE,
    RECOVER,
    ENTER_GRACE,
    STAY_IN_GRACE,
    DISSOLVE
}
