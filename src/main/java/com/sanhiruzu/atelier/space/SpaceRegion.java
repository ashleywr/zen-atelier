package com.sanhiruzu.atelier.space;

import java.util.UUID;

public class SpaceRegion {
    private final UUID id;
    private final ClassificationState type;
    private int volume;
    private int openingArea;

    public SpaceRegion(UUID id, ClassificationState type, int volume, int openingArea) {
        this.id = id;
        this.type = type;
        this.volume = volume;
        this.openingArea = openingArea;
    }

    public UUID getId() {
        return id;
    }

    public ClassificationState getType() {
        return type;
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

    public void mergeWith(SpaceRegion other) {
        this.volume += other.volume;
        this.openingArea += other.openingArea;
    }

    public void adjustVolume(int delta) {
        this.volume += delta;
    }

    public void adjustOpeningArea(int delta) {
        this.openingArea = Math.max(0, this.openingArea + delta);
    }
}
