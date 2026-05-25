package com.sanhiruzu.atelier.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ZoneQualityChangePayload(
        int triggerX,
        int triggerY,
        int triggerZ,
        float oldQuality,
        float newQuality
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ZoneQualityChangePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("zen_atelier", "zone_quality_change"));

    public static final StreamCodec<FriendlyByteBuf, ZoneQualityChangePayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeInt(p.triggerX);
                buf.writeInt(p.triggerY);
                buf.writeInt(p.triggerZ);
                buf.writeFloat(p.oldQuality);
                buf.writeFloat(p.newQuality);
            },
            buf -> new ZoneQualityChangePayload(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readFloat(),
                    buf.readFloat()
            )
    );

    @Override
    public CustomPacketPayload.Type<ZoneQualityChangePayload> type() {
        return TYPE;
    }
}
