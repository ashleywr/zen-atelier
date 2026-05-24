package com.sanhiruzu.atelier.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

public class ClassifiedRegion {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(
            UUID::fromString,
            UUID::toString
    );

    public static final Codec<ClassifiedRegion> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUID_CODEC.fieldOf("id").forGetter(ClassifiedRegion::getId),
                    Codec.INT.fieldOf("volume").forGetter(ClassifiedRegion::getVolume),
                    Codec.INT.fieldOf("openingArea").forGetter(ClassifiedRegion::getOpeningArea)
            ).apply(instance, ClassifiedRegion::new)
    );
    private final UUID id;
    private final int volume;
    private final int openingArea;

    public ClassifiedRegion(int volume, int openingArea) {
        this.id = UUID.randomUUID();
        this.volume = volume;
        this.openingArea = openingArea;
    }

    public ClassifiedRegion(UUID id, int volume, int openingArea) {
        this.id = id;
        this.volume = volume;
        this.openingArea = openingArea;
    }

    public UUID getId() {
        return id;
    }

    public int getVolume() {
        return volume;
    }

    public int getOpeningArea() {
        return openingArea;
    }

    public float getEnclosureScore() {
        if (volume == 0) return 0.0f;
        return 1.0f - ((float) openingArea / (float) (volume * 6));
    }
}
