package com.sanhiruzu.atelier.synthesis.vfx;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server-side trigger for an eruption-style impact effect. Spawns the center flash,
 * the radial ring of crystals (each shatters itself client-side), accents, and sound,
 * all scaled by quality tier via the profile's tuning. sendParticles auto-syncs.
 */
public final class EruptionVfx {
    private EruptionVfx() {}

    public static void play(ServerLevel level, Vec3 pos, int qt, ImpactVfxProfile profile) {
        EruptionTuning t = profile.tuning();

        ParticleType<ScaledParticleOptions> burstType = profile.burstType().get();
        level.sendParticles(new ScaledParticleOptions(burstType, t.burstPeakScale(qt), 10),
                pos.x, pos.y + 0.1, pos.z, 1, 0.0, 0.0, 0.0, 0.0);

        ParticleType<ScaledParticleOptions> crystalType = profile.crystalType().get();
        List<Vec3> offsets = EruptionGeometry.crystalOffsets(t, qt, level.random.nextLong());
        for (Vec3 o : offsets) {
            level.sendParticles(new ScaledParticleOptions(crystalType, t.crystalPeakScale(qt), t.crystalLifetime(qt)),
                    pos.x + o.x, pos.y + 0.05, pos.z + o.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        double spread = t.ringRadius(qt) * 0.6;
        int accents = t.accentCount(qt);
        ParticleOptions ringAccent = profile.ringAccent().get();
        level.sendParticles(ringAccent, pos.x, pos.y + 0.3, pos.z,
                accents, spread, 0.2, spread, 0.01);
        level.sendParticles(profile.trailAccent(), pos.x, pos.y + 0.3, pos.z,
                accents, spread, 0.2, spread, 0.02);

        float pitch = 0.9F + qt * 0.07F;
        level.playSound(null, pos.x, pos.y, pos.z, profile.sound().get(), SoundSource.PLAYERS, 0.9F, pitch);
    }
}
