package com.sanhiruzu.atelier.synthesis.vfx.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** How a billboard blends with the scene. */
public enum Blend implements StringRepresentable {
    TRANSLUCENT("translucent"),
    ADDITIVE("additive");

    public static final Codec<Blend> CODEC = StringRepresentable.fromEnum(Blend::values);
    public static final StreamCodec<ByteBuf, Blend> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> Blend.values()[i], Blend::ordinal);

    private final String name;

    Blend(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
