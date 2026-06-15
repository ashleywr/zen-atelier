package com.sanhiruzu.atelier.synthesis.gathering;

import java.util.Locale;

public enum GatheringMarkerType {
    FORAGE("forage"),
    ORE("ore"),
    STRIKE("strike");

    private final String serializedName;

    GatheringMarkerType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static GatheringMarkerType fromSerializedName(String serializedName) {
        if (serializedName == null || serializedName.isBlank()) {
            return FORAGE;
        }
        String normalized = serializedName.toLowerCase(Locale.ROOT);
        for (GatheringMarkerType type : values()) {
            if (type.serializedName.equals(normalized)) {
                return type;
            }
        }
        return FORAGE;
    }
}
