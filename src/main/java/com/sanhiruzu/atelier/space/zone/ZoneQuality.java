package com.sanhiruzu.atelier.space.zone;

public enum ZoneQuality {
    F(0.00f),
    D(0.10f),
    C(0.25f),
    B(0.40f),
    A(0.55f),
    S(0.70f),
    SS(0.85f);

    public final float minQuality;

    ZoneQuality(float min) {
        this.minQuality = min;
    }

    public static ZoneQuality fromQuality(float quality) {
        ZoneQuality result = F;
        for (ZoneQuality grade : values()) {
            if (quality >= grade.minQuality) result = grade;
        }
        return result;
    }
}
