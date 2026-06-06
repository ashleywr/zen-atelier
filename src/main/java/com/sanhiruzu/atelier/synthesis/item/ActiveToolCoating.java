package com.sanhiruzu.atelier.synthesis.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record ActiveToolCoating(ResourceLocation coatingId, int charges, float speedMultiplier) {
    public static final Codec<ActiveToolCoating> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("coating_id").forGetter(ActiveToolCoating::coatingId),
            Codec.intRange(1, 4096).fieldOf("charges").forGetter(ActiveToolCoating::charges),
            Codec.floatRange(1.0f, 16.0f).fieldOf("speed_multiplier").forGetter(ActiveToolCoating::speedMultiplier)
    ).apply(instance, ActiveToolCoating::new));

    public ActiveToolCoating {
        charges = Math.clamp(charges, 1, 4096);
        speedMultiplier = Math.clamp(speedMultiplier, 1.0f, 16.0f);
    }

    public ActiveToolCoating consumeCharge() {
        return new ActiveToolCoating(coatingId, charges - 1, speedMultiplier);
    }
}
