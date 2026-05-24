package com.sanhiruzu.atelier.network;

import com.sanhiruzu.atelier.space.ChunkClassificationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncChunkClassificationPayload(int chunkX, int chunkZ,
                                             ChunkClassificationData data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncChunkClassificationPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("zen_atelier", "sync_chunk_classification"));

    public static final StreamCodec<FriendlyByteBuf, SyncChunkClassificationPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SyncChunkClassificationPayload::chunkX,
            ByteBufCodecs.INT,
            SyncChunkClassificationPayload::chunkZ,
            ByteBufCodecs.COMPOUND_TAG,
            payload -> {
                CompoundTag tag = new CompoundTag();
                payload.data.serializeNBT(tag);
                return tag;
            },
            (chunkX, chunkZ, tag) -> {
                ChunkClassificationData data = new ChunkClassificationData();
                data.deserializeNBT(tag);
                return new SyncChunkClassificationPayload(chunkX, chunkZ, data);
            }
    );

    @Override
    public CustomPacketPayload.Type<SyncChunkClassificationPayload> type() {
        return TYPE;
    }
}
