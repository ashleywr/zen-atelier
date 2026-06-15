package com.sanhiruzu.atelier.synthesis.vfx;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EruptionTuningTest {
    @Test
    void iceValuesIncreaseMonotonicallyWithTier() {
        EruptionTuning t = EruptionTuning.ICE;
        for (int qt = 1; qt <= 3; qt++) {
            assertThat(t.crystalCount(qt)).isGreaterThan(t.crystalCount(qt - 1));
            assertThat(t.ringRadius(qt)).isGreaterThan(t.ringRadius(qt - 1));
            assertThat(t.crystalPeakScale(qt)).isGreaterThan(t.crystalPeakScale(qt - 1));
            assertThat(t.crystalLifetime(qt)).isGreaterThan(t.crystalLifetime(qt - 1));
            assertThat(t.burstPeakScale(qt)).isGreaterThan(t.burstPeakScale(qt - 1));
            assertThat(t.accentCount(qt)).isGreaterThan(t.accentCount(qt - 1));
        }
    }

    @Test
    void tierIsClampedToValidRange() {
        EruptionTuning t = EruptionTuning.ICE;
        assertThat(t.crystalCount(-5)).isEqualTo(t.crystalCount(0));
        assertThat(t.crystalCount(99)).isEqualTo(t.crystalCount(3));
    }
}
