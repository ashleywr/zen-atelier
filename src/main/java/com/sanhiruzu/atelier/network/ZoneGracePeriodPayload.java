package com.sanhiruzu.atelier.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Sent server→client when a zone loses its last entry block (ticksRemaining > 0)
 * or when a zone recovers before expiry (ticksRemaining == 0).
 */
public record ZoneGracePeriodPayload(UUID zoneId, int ticksRemaining) implements CustomPacketPayload {
    public static final Type<ZoneGracePeriodPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("zen_atelier", "zone_grace_period"));

    public static final StreamCodec<FriendlyByteBuf, ZoneGracePeriodPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.zoneId.getMostSignificantBits());
                buf.writeLong(p.zoneId.getLeastSignificantBits());
                buf.writeVarInt(p.ticksRemaining);
            },
            buf -> {
                UUID id = new UUID(buf.readLong(), buf.readLong());
                return new ZoneGracePeriodPayload(id, buf.readVarInt());
            }
    );

    @Override
    public Type<ZoneGracePeriodPayload> type() {
        return TYPE;
    }
}
