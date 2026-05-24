package com.sanhiruzu.atelier.space;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ChunkClassificationAttachment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "zen_atelier");

    public static final Supplier<AttachmentType<ChunkClassificationData>> CHUNK_CLASSIFICATION =
            ATTACHMENT_TYPES.register("chunk_classification", () ->
                    AttachmentType.builder(() -> new ChunkClassificationData())
                            .serialize(ChunkClassificationData.CODEC)
                            .build()
            );

    public static ChunkClassificationData get(ChunkAccess chunk) {
        return chunk.getData(CHUNK_CLASSIFICATION.get());
    }

    public static void set(ChunkAccess chunk, ChunkClassificationData data) {
        chunk.setData(CHUNK_CLASSIFICATION.get(), data);
    }
}
