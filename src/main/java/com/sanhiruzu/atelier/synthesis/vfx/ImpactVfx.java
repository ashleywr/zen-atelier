package com.sanhiruzu.atelier.synthesis.vfx;

import com.sanhiruzu.atelier.synthesis.vfx.data.EmitterDefinition;
import com.sanhiruzu.atelier.synthesis.vfx.data.ImpactVfxDefinition;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Server-side: spawns a loaded impact-VFX profile at a position, scaled by quality tier. */
public final class ImpactVfx {
    private ImpactVfx() {}

    public static void play(ServerLevel level, Vec3 pos, int qt, ImpactVfxDefinition def) {
        for (EmitterDefinition e : def.emitters()) {
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(e.particle());
            if (type == null) {
                continue;
            }
            int count = Math.max(0, e.count().intAt(qt));
            double radius = e.radius().floatAt(qt);
            double yOff = e.yOffset().floatAt(qt);
            List<Vec3> offsets = EmitterShapes.positions(e.shape(), count, radius, level.random.nextLong());

            for (Vec3 o : offsets) {
                double px = pos.x + o.x;
                double py = pos.y + o.y + yOff;
                double pz = pos.z + o.z;
                if (type instanceof ScaledParticleOptions.Type scalable) {
                    ScaledParticleOptions opts = new ScaledParticleOptions(
                            scalable, e.size().floatAt(qt), e.lifetime().intAt(qt),
                            e.growTicks(), e.fadeTicks(), e.anchor(), e.blend());
                    level.sendParticles(opts, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
                } else if (type instanceof ParticleOptions plain) {
                    level.sendParticles(plain, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }

        def.sound().ifPresent(soundId -> {
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId);
            if (sound != null) {
                float pitch = 0.9F + qt * 0.07F;
                level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, 0.9F, pitch);
            }
        });
    }
}
