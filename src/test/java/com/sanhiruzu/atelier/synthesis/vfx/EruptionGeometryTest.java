package com.sanhiruzu.atelier.synthesis.vfx;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class EruptionGeometryTest {
    @Test
    void producesOneCrystalPerTierCount() {
        for (int qt = 0; qt <= 3; qt++) {
            List<Vec3> offsets = EruptionGeometry.crystalOffsets(EruptionTuning.ICE, qt, 1234L);
            assertThat(offsets).hasSize(EruptionTuning.ICE.crystalCount(qt));
        }
    }

    @Test
    void firstOffsetIsTheCenter() {
        List<Vec3> offsets = EruptionGeometry.crystalOffsets(EruptionTuning.ICE, 3, 1234L);
        assertThat(offsets.get(0)).isEqualTo(Vec3.ZERO);
    }

    @Test
    void ringOffsetsStayWithinRadiusPlusJitter() {
        int qt = 3;
        double maxHoriz = EruptionTuning.ICE.ringRadius(qt) + 0.6;
        List<Vec3> offsets = EruptionGeometry.crystalOffsets(EruptionTuning.ICE, qt, 1234L);
        for (Vec3 o : offsets) {
            double horiz = Math.sqrt(o.x * o.x + o.z * o.z);
            assertThat(horiz).isLessThanOrEqualTo(maxHoriz);
        }
    }

    @Test
    void isDeterministicForSameSeed() {
        assertThat(EruptionGeometry.crystalOffsets(EruptionTuning.ICE, 2, 42L))
                .isEqualTo(EruptionGeometry.crystalOffsets(EruptionTuning.ICE, 2, 42L));
    }
}
