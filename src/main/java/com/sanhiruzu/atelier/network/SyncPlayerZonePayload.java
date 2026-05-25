package com.sanhiruzu.atelier.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

public record SyncPlayerZonePayload(@Nullable UUID zoneId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPlayerZonePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("zen_atelier", "sync_player_zone"));

    public static final StreamCodec<FriendlyByteBuf, SyncPlayerZonePayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                if (p.zoneId != null) {
                    buf.writeBoolean(true);
                    buf.writeLong(p.zoneId.getMostSignificantBits());
                    buf.writeLong(p.zoneId.getLeastSignificantBits());
                } else {
                    buf.writeBoolean(false);
                }
            },
            buf -> {
                UUID zoneId = buf.readBoolean() ? new UUID(buf.readLong(), buf.readLong()) : null;
                return new SyncPlayerZonePayload(zoneId);
            }
    );

    @Override
    public CustomPacketPayload.Type<SyncPlayerZonePayload> type() {
        return TYPE;
    }
}
