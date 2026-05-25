package com.sanhiruzu.atelier.space.zone;

import javax.annotation.Nullable;

/**
 * Chooses lifecycle transitions after a scan/evaluation pass.
 *
 * <p>This is the first reconciliation layer. It does not yet match newly
 * discovered components to old zones by overlap; it makes the existing
 * evaluation behavior explicit so invalid rooms become non-destructive grace
 * states instead of silently staying active or being deleted immediately.</p>
 */
final class ZoneReconciler {
    private ZoneReconciler() {
    }

    static ZoneReconciliationDecision decide(@Nullable ZoneData previous,
                                             @Nullable ZoneData fresh,
                                             boolean inGracePeriod) {
        if (fresh != null) {
            if (inGracePeriod) {
                return new ZoneReconciliationDecision(
                        ZoneReconciliationAction.RECOVER,
                        "fresh evaluation is valid while zone is in grace period");
            }
            if (previous == null) {
                return new ZoneReconciliationDecision(
                        ZoneReconciliationAction.ACTIVATE,
                        "fresh evaluation created first player-facing room data");
            }
            return new ZoneReconciliationDecision(
                    ZoneReconciliationAction.UPDATE,
                    "fresh evaluation remains valid");
        }

        if (previous != null) {
            if (inGracePeriod) {
                return new ZoneReconciliationDecision(
                        ZoneReconciliationAction.STAY_IN_GRACE,
                        "zone is still invalid but already has a grace timer");
            }
            return new ZoneReconciliationDecision(
                    ZoneReconciliationAction.ENTER_GRACE,
                    "previously valid zone failed latest evaluation");
        }

        return new ZoneReconciliationDecision(
                ZoneReconciliationAction.DISSOLVE,
                "zone has never produced valid player-facing data");
    }
}
