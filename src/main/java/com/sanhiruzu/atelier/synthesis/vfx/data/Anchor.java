package com.sanhiruzu.atelier.synthesis.vfx.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** How a billboard sits relative to its spawn point. */
public enum Anchor implements StringRepresentable {
    CENTER("center"),
    GROUND("ground");

    public static final Codec<Anchor> CODEC = StringRepresentable.fromEnum(Anchor::values);
    public static final StreamCodec<ByteBuf, Anchor> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> Anchor.values()[i], Anchor::ordinal);

    private final String name;

    Anchor(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
