package com.sanhiruzu.atelier.synthesis.vfx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Generic particle options carrying a target render scale and lifetime, so the same
 * options shape can drive any number of registered particle types at per-tier sizes.
 * Each instance stores its own type (like vanilla BlockParticleOption) so getType()
 * routes serialization to the correct registered type.
 */
public record ScaledParticleOptions(ParticleType<ScaledParticleOptions> type, float peakScale, int lifetime)
        implements ParticleOptions {

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    /** Registered particle type whose codecs reconstruct options bound to itself. */
    public static final class Type extends ParticleType<ScaledParticleOptions> {
        public Type() {
            super(false);
        }

        @Override
        public MapCodec<ScaledParticleOptions> codec() {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.FLOAT.fieldOf("peak_scale").forGetter(ScaledParticleOptions::peakScale),
                    Codec.INT.fieldOf("lifetime").forGetter(ScaledParticleOptions::lifetime)
            ).apply(instance, (scale, life) -> new ScaledParticleOptions(this, scale, life)));
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, ScaledParticleOptions> streamCodec() {
            return StreamCodec.composite(
                    ByteBufCodecs.FLOAT, ScaledParticleOptions::peakScale,
                    ByteBufCodecs.VAR_INT, ScaledParticleOptions::lifetime,
                    (scale, life) -> new ScaledParticleOptions(this, scale, life)
            );
        }
    }
}
