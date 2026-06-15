package com.sanhiruzu.atelier.synthesis.vfx;

import com.sanhiruzu.atelier.synthesis.vfx.data.EmitterShape;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Pure: turns an emitter shape + count + radius into spawn offsets relative to impact. */
public final class EmitterShapes {
    private EmitterShapes() {}

    public static List<Vec3> positions(EmitterShape shape, int count, double radius, long seed) {
        Random rng = new Random(seed);
        List<Vec3> out = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            out.add(switch (shape) {
                case POINT -> Vec3.ZERO;
                case RING -> {
                    double a = (Math.PI * 2.0 * i / count) + (rng.nextDouble() - 0.5) * 0.4;
                    double r = radius * (0.85 + rng.nextDouble() * 0.15);
                    yield new Vec3(Math.cos(a) * r, 0.0, Math.sin(a) * r);
                }
                case DISC -> {
                    double a = rng.nextDouble() * Math.PI * 2.0;
                    double r = radius * Math.sqrt(rng.nextDouble());
                    yield new Vec3(Math.cos(a) * r, 0.0, Math.sin(a) * r);
                }
                case SPHERE -> {
                    double r = radius * Math.cbrt(rng.nextDouble());
                    double theta = rng.nextDouble() * Math.PI * 2.0;
                    double phi = Math.acos(2.0 * rng.nextDouble() - 1.0);
                    yield new Vec3(r * Math.sin(phi) * Math.cos(theta), r * Math.cos(phi), r * Math.sin(phi) * Math.sin(theta));
                }
            });
        }
        return out;
    }
}
