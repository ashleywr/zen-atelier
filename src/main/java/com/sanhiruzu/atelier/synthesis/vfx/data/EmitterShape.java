package com.sanhiruzu.atelier.synthesis.vfx.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Where a layer's particles appear relative to the impact point. */
public enum EmitterShape implements StringRepresentable {
    POINT("point"),
    RING("ring"),
    SPHERE("sphere"),
    DISC("disc");

    public static final Codec<EmitterShape> CODEC = StringRepresentable.fromEnum(EmitterShape::values);

    private final String name;

    EmitterShape(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
