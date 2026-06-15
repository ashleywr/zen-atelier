package com.sanhiruzu.atelier.synthesis.vfx;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure geometry for an eruption: a center point plus a jittered ring, counted and
 * sized per quality tier from an {@link EruptionTuning}. Returns XZ offsets relative
 * to the impact point (Y left to the caller). No world or client access.
 */
public final class EruptionGeometry {
    private EruptionGeometry() {}

    /** Offsets relative to impact: index 0 is always the center (Vec3.ZERO). */
    public static List<Vec3> crystalOffsets(EruptionTuning tuning, int qt, long seed) {
        int count = tuning.crystalCount(qt);
        double radius = tuning.ringRadius(qt);
        Random rng = new Random(seed);

        List<Vec3> offsets = new ArrayList<>(count);
        offsets.add(Vec3.ZERO); // center

        int ringCount = count - 1;
        for (int i = 0; i < ringCount; i++) {
            double angle = (Math.PI * 2.0 * i / ringCount) + (rng.nextDouble() - 0.5) * 0.4;
            double r = radius * (0.7 + rng.nextDouble() * 0.3);
            double jitterX = (rng.nextDouble() - 0.5) * 0.4;
            double jitterZ = (rng.nextDouble() - 0.5) * 0.4;
            offsets.add(new Vec3(Math.cos(angle) * r + jitterX, 0.0, Math.sin(angle) * r + jitterZ));
        }
        return offsets;
    }
}
