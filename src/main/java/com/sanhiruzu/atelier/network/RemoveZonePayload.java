package com.sanhiruzu.atelier.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record RemoveZonePayload(UUID zoneId, int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RemoveZonePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("zen_atelier", "remove_zone"));

    public static final StreamCodec<FriendlyByteBuf, RemoveZonePayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.zoneId().getMostSignificantBits());
                buf.writeLong(p.zoneId().getLeastSignificantBits());
                buf.writeInt(p.minX());
                buf.writeInt(p.minY());
                buf.writeInt(p.minZ());
                buf.writeInt(p.maxX());
                buf.writeInt(p.maxY());
                buf.writeInt(p.maxZ());
            },
            buf -> new RemoveZonePayload(
                    new UUID(buf.readLong(), buf.readLong()),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
