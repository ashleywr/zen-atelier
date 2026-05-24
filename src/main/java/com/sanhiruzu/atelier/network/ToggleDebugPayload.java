package com.sanhiruzu.atelier.network;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToggleDebugPayload(boolean enabled) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleDebugPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "toggle_debug")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleDebugPayload> CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.BOOL,
            ToggleDebugPayload::enabled,
            ToggleDebugPayload::new
    );

    @Override
    public CustomPacketPayload.Type<ToggleDebugPayload> type() {
        return TYPE;
    }
}
