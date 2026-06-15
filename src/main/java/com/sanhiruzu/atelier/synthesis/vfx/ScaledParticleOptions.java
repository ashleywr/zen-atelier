package com.sanhiruzu.atelier.synthesis.vfx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.vfx.data.Anchor;
import com.sanhiruzu.atelier.synthesis.vfx.data.Blend;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Generic particle options carrying full per-billboard behavior (scale, lifetime, grow/
 * fade timing, ground/centre anchor, blend mode), so one options shape drives any number
 * of registered particle types. Each instance stores its own type (like vanilla
 * BlockParticleOption) so getType() routes serialization to the correct registered type.
 */
public record ScaledParticleOptions(
        ParticleType<ScaledParticleOptions> type,
        float peakScale,
        int lifetime,
        int growTicks,
        int fadeTicks,
        Anchor anchor,
        Blend blend
) implements ParticleOptions {

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    public static final class Type extends ParticleType<ScaledParticleOptions> {
        public Type() {
            super(false);
        }

        @Override
        public MapCodec<ScaledParticleOptions> codec() {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.FLOAT.fieldOf("peak_scale").forGetter(ScaledParticleOptions::peakScale),
                    Codec.INT.fieldOf("lifetime").forGetter(ScaledParticleOptions::lifetime),
                    Codec.INT.fieldOf("grow_ticks").forGetter(ScaledParticleOptions::growTicks),
                    Codec.INT.fieldOf("fade_ticks").forGetter(ScaledParticleOptions::fadeTicks),
                    Anchor.CODEC.fieldOf("anchor").forGetter(ScaledParticleOptions::anchor),
                    Blend.CODEC.fieldOf("blend").forGetter(ScaledParticleOptions::blend)
            ).apply(instance, (scale, life, grow, fade, anchor, blend) ->
                    new ScaledParticleOptions(this, scale, life, grow, fade, anchor, blend)));
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, ScaledParticleOptions> streamCodec() {
            return StreamCodec.composite(
                    ByteBufCodecs.FLOAT, ScaledParticleOptions::peakScale,
                    ByteBufCodecs.VAR_INT, ScaledParticleOptions::lifetime,
                    ByteBufCodecs.VAR_INT, ScaledParticleOptions::growTicks,
                    ByteBufCodecs.VAR_INT, ScaledParticleOptions::fadeTicks,
                    Anchor.STREAM_CODEC, ScaledParticleOptions::anchor,
                    Blend.STREAM_CODEC, ScaledParticleOptions::blend,
                    (scale, life, grow, fade, anchor, blend) ->
                            new ScaledParticleOptions(this, scale, life, grow, fade, anchor, blend)
            );
        }
    }
}
