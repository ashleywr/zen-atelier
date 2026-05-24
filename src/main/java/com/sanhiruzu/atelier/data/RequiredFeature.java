package com.sanhiruzu.atelier.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RequiredFeature(String signal, int minimum) {
    public static final Codec<RequiredFeature> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("signal").forGetter(RequiredFeature::signal),
            Codec.INT.fieldOf("minimum").forGetter(RequiredFeature::minimum)
    ).apply(instance, RequiredFeature::new));
}
