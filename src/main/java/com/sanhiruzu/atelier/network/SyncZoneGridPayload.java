package com.sanhiruzu.atelier.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

public record SyncZoneGridPayload(
        UUID zoneId,
        boolean isOutdoor,
        int volume,
        float enclosureScore,
        float quality,
        @Nullable ResourceLocation zoneTypeId,
        boolean degraded,
        @Nullable String epithetName,
        @Nullable String generatedName,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncZoneGridPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("zen_atelier", "sync_zone_grid"));

    public static final StreamCodec<FriendlyByteBuf, SyncZoneGridPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.zoneId.getMostSignificantBits());
                buf.writeLong(p.zoneId.getLeastSignificantBits());
                buf.writeBoolean(p.isOutdoor);
                buf.writeVarInt(p.volume);
                buf.writeFloat(p.enclosureScore);
                buf.writeFloat(p.quality);
                if (p.zoneTypeId != null) {
                    buf.writeBoolean(true);
                    buf.writeResourceLocation(p.zoneTypeId);
                } else {
                    buf.writeBoolean(false);
                }
                buf.writeBoolean(p.degraded);
                if (p.epithetName != null) {
                    buf.writeBoolean(true);
                    buf.writeUtf(p.epithetName);
                } else {
                    buf.writeBoolean(false);
                }
                if (p.generatedName != null) {
                    buf.writeBoolean(true);
                    buf.writeUtf(p.generatedName);
                } else {
                    buf.writeBoolean(false);
                }
                buf.writeInt(p.minX);
                buf.writeInt(p.minY);
                buf.writeInt(p.minZ);
                buf.writeInt(p.maxX);
                buf.writeInt(p.maxY);
                buf.writeInt(p.maxZ);
            },
            buf -> {
                UUID zoneId = new UUID(buf.readLong(), buf.readLong());
                boolean isOutdoor = buf.readBoolean();
                int volume = buf.readVarInt();
                float enclosureScore = buf.readFloat();
                float quality = buf.readFloat();
                ResourceLocation zoneTypeId = buf.readBoolean() ? buf.readResourceLocation() : null;
                boolean degraded = buf.readBoolean();
                String epithetName = buf.readBoolean() ? buf.readUtf() : null;
                String generatedName = buf.readBoolean() ? buf.readUtf() : null;
                int minX = buf.readInt();
                int minY = buf.readInt();
                int minZ = buf.readInt();
                int maxX = buf.readInt();
                int maxY = buf.readInt();
                int maxZ = buf.readInt();
                return new SyncZoneGridPayload(
                        zoneId, isOutdoor, volume, enclosureScore, quality,
                        zoneTypeId, degraded, epithetName, generatedName,
                        minX, minY, minZ, maxX, maxY, maxZ);
            }
    );

    @Override
    public CustomPacketPayload.Type<SyncZoneGridPayload> type() {
        return TYPE;
    }
}
