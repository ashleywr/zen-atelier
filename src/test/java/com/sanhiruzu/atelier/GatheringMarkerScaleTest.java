package com.sanhiruzu.atelier;

import com.sanhiruzu.atelier.synthesis.gathering.client.GatheringPointRenderer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatheringMarkerScaleTest {
    @Test
    void iconScaleGrowsWithDistanceSoCloseMarkersDoNotBalloon() {
        float close = GatheringPointRenderer.gatheringIconScale(2.0D);
        float mid = GatheringPointRenderer.gatheringIconScale(12.0D);
        float far = GatheringPointRenderer.gatheringIconScale(48.0D);

        assertThat(close).isLessThan(mid);
        assertThat(mid).isLessThan(far);
    }

    @Test
    void iconScaleIsSmallerThanOldMarkerAndRemainsBounded() {
        float close = GatheringPointRenderer.gatheringIconScale(2.0D);
        float far = GatheringPointRenderer.gatheringIconScale(48.0D);

        assertThat(close).isBetween(0.11F, 0.18F);
        assertThat(far).isBetween(close, 0.34F);
    }

    @Test
    void stemHeightIsStableForNearbyMarkers() {
        assertThat(GatheringPointRenderer.gatheringStemHeight(2.0D)).isBetween(0.72F, 0.95F);
        assertThat(GatheringPointRenderer.gatheringStemHeight(48.0D)).isBetween(0.72F, 1.2F);
    }
}
