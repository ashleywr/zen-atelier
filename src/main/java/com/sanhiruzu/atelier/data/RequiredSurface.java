package com.sanhiruzu.atelier.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RequiredSurface(
        String block,
        int minimum
) {
    public static final Codec<RequiredSurface> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("block").forGetter(RequiredSurface::block),
                    Codec.INT.fieldOf("minimum").forGetter(RequiredSurface::minimum)
            ).apply(instance, RequiredSurface::new)
    );
}
