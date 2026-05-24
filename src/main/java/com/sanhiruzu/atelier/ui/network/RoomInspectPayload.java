package com.sanhiruzu.atelier.ui.network;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.ui.journal.RoomJournalActions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record RoomInspectPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RoomInspectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "room_inspect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoomInspectPayload> CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeBlockPos(packet.pos),
                    buf -> new RoomInspectPayload(buf.readBlockPos())
            );

    @Override
    public Type<RoomInspectPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        RoomJournalActions.inspectRoom(player, this.pos);
    }
}
