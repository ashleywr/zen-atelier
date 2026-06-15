package com.sanhiruzu.atelier.synthesis.vfx.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** One layer of an impact effect: a particle spawned in a shape, scaled per tier. */
public record EmitterDefinition(
        ResourceLocation particle,
        EmitterShape shape,
        TieredValue count,
        TieredValue radius,
        TieredValue size,
        TieredValue lifetime,
        int growTicks,
        int fadeTicks,
        Anchor anchor,
        Blend blend,
        TieredValue yOffset
) {
    public static final Codec<EmitterDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("particle").forGetter(EmitterDefinition::particle),
            EmitterShape.CODEC.optionalFieldOf("shape", EmitterShape.POINT).forGetter(EmitterDefinition::shape),
            TieredValue.CODEC.optionalFieldOf("count", TieredValue.scalar(1)).forGetter(EmitterDefinition::count),
            TieredValue.CODEC.optionalFieldOf("radius", TieredValue.scalar(0)).forGetter(EmitterDefinition::radius),
            TieredValue.CODEC.optionalFieldOf("size", TieredValue.scalar(1.0f)).forGetter(EmitterDefinition::size),
            TieredValue.CODEC.optionalFieldOf("lifetime", TieredValue.scalar(20)).forGetter(EmitterDefinition::lifetime),
            Codec.INT.optionalFieldOf("grow_ticks", 4).forGetter(EmitterDefinition::growTicks),
            Codec.INT.optionalFieldOf("fade_ticks", 4).forGetter(EmitterDefinition::fadeTicks),
            Anchor.CODEC.optionalFieldOf("anchor", Anchor.CENTER).forGetter(EmitterDefinition::anchor),
            Blend.CODEC.optionalFieldOf("blend", Blend.TRANSLUCENT).forGetter(EmitterDefinition::blend),
            TieredValue.CODEC.optionalFieldOf("y_offset", TieredValue.scalar(0)).forGetter(EmitterDefinition::yOffset)
    ).apply(instance, EmitterDefinition::new));
}
