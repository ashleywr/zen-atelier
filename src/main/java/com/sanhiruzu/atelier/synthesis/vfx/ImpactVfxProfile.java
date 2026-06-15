package com.sanhiruzu.atelier.synthesis.vfx;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Supplier;

/**
 * A complete impact effect, defined in code. Adding a new element = drop textures,
 * register its particle types, and add one of these constants plus a dispatch entry.
 *
 * @param burstType    center flash particle type (scalable)
 * @param crystalType  ring "crystal" particle type (scalable, shatters on death)
 * @param ringAccent   accent particle scattered around the ring (e.g. spark)
 * @param trailAccent  secondary vanilla accent (e.g. snowflake)
 * @param sound        impact sound
 * @param tuning       per-tier scaling
 */
public record ImpactVfxProfile(
        Supplier<ParticleType<ScaledParticleOptions>> burstType,
        Supplier<ParticleType<ScaledParticleOptions>> crystalType,
        Supplier<? extends ParticleOptions> ringAccent,
        ParticleOptions trailAccent,
        Supplier<SoundEvent> sound,
        EruptionTuning tuning) {

    public static final ImpactVfxProfile ICE = new ImpactVfxProfile(
            () -> ZenAtelier.ICE_BURST.get(),
            () -> ZenAtelier.ICE_CRYSTAL.get(),
            ZenAtelier.ICE_SPARK::get,
            ParticleTypes.SNOWFLAKE,
            () -> SoundEvents.GLASS_BREAK,
            EruptionTuning.ICE
    );
}
