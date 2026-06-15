package com.sanhiruzu.atelier.synthesis.vfx;

import com.sanhiruzu.atelier.synthesis.vfx.data.EmitterShape;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class EmitterShapesTest {
    @Test
    void pointPutsEverythingAtCenter() {
        List<Vec3> p = EmitterShapes.positions(EmitterShape.POINT, 5, 3.0, 1L);
        assertThat(p).hasSize(5).allMatch(v -> v.equals(Vec3.ZERO));
    }

    @Test
    void ringPlacesCountOnApproxRadius() {
        List<Vec3> p = EmitterShapes.positions(EmitterShape.RING, 8, 2.0, 1L);
        assertThat(p).hasSize(8);
        for (Vec3 v : p) {
            double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
            assertThat(horiz).isBetween(1.4, 2.2); // ~0.85-1.0 of radius + jitter
            assertThat(v.y).isEqualTo(0.0);
        }
    }

    @Test
    void discStaysWithinRadiusOnGroundPlane() {
        List<Vec3> p = EmitterShapes.positions(EmitterShape.DISC, 20, 2.5, 7L);
        assertThat(p).hasSize(20);
        for (Vec3 v : p) {
            assertThat(Math.sqrt(v.x * v.x + v.z * v.z)).isLessThanOrEqualTo(2.5);
            assertThat(v.y).isEqualTo(0.0);
        }
    }

    @Test
    void sphereStaysWithinRadius() {
        List<Vec3> p = EmitterShapes.positions(EmitterShape.SPHERE, 20, 1.5, 7L);
        assertThat(p).hasSize(20).allMatch(v -> v.length() <= 1.5 + 1e-6);
    }

    @Test
    void deterministicForSameSeed() {
        assertThat(EmitterShapes.positions(EmitterShape.SPHERE, 10, 2.0, 42L))
                .isEqualTo(EmitterShapes.positions(EmitterShape.SPHERE, 10, 2.0, 42L));
    }
}
