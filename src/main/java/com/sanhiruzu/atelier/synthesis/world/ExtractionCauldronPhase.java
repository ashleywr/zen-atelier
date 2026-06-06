package com.sanhiruzu.atelier.synthesis.world;

import net.minecraft.util.StringRepresentable;

public enum ExtractionCauldronPhase implements StringRepresentable {
    READY("ready", 0x57C978),
    EXTRACTING("extracting", 0xE8902F);

    private final String serializedName;
    private final int waterTint;

    ExtractionCauldronPhase(String serializedName, int waterTint) {
        this.serializedName = serializedName;
        this.waterTint = waterTint;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public int waterTint() {
        return waterTint;
    }
}
