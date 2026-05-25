package com.sanhiruzu.atelier.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

public record ClassifiedRegion(UUID id, int volume, int openingArea) {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(
            UUID::fromString,
            UUID::toString
    );

    public static final Codec<ClassifiedRegion> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUID_CODEC.fieldOf("id").forGetter(ClassifiedRegion::id),
                    Codec.INT.fieldOf("volume").forGetter(ClassifiedRegion::volume),
                    Codec.INT.fieldOf("openingArea").forGetter(ClassifiedRegion::openingArea)
            ).apply(instance, ClassifiedRegion::new)
    );

    public ClassifiedRegion(int volume, int openingArea) {
        this(UUID.randomUUID(), volume, openingArea);
    }

    public float getEnclosureScore() {
        if (volume == 0) return 0.0f;
        return 1.0f - ((float) openingArea / (float) (volume * 6));
    }
}
