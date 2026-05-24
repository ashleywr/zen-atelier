package com.sanhiruzu.atelier.ui.network;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record DiscoveryDataSyncPayload(Map<String, Integer> discoveredRooms) implements CustomPacketPayload {
    public static final Type<DiscoveryDataSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "discovery_sync"));

    public static final StreamCodec<FriendlyByteBuf, DiscoveryDataSyncPayload> CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeVarInt(packet.discoveredRooms.size());
                        for (Map.Entry<String, Integer> entry : packet.discoveredRooms.entrySet()) {
                            buf.writeUtf(entry.getKey());
                            buf.writeVarInt(entry.getValue());
                        }
                    },
                    buf -> {
                        Map<String, Integer> data = new HashMap<>();
                        int size = buf.readVarInt();
                        for (int i = 0; i < size; i++) {
                            data.put(buf.readUtf(), buf.readVarInt());
                        }
                        return new DiscoveryDataSyncPayload(data);
                    }
            );

    @Override
    public Type<DiscoveryDataSyncPayload> type() {
        return TYPE;
    }
}
