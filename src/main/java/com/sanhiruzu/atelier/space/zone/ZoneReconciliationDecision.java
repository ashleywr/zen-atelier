package com.sanhiruzu.atelier.space.zone;

/**
 * Result of reconciling a zone's old state with a new scan/evaluation pass.
 *
 * <p>The message is intentionally developer-facing. It gives debug commands and
 * logs a stable explanation for why a room was preserved, put into grace, or
 * dissolved.</p>
 */
record ZoneReconciliationDecision(ZoneReconciliationAction action, String reason) {
}
