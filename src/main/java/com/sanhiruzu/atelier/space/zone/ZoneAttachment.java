package com.sanhiruzu.atelier.space.zone;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ZoneAttachment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "zen_atelier");

    public static final Supplier<AttachmentType<ZoneAttachment>> ZONE =
            ATTACHMENT_TYPES.register("entity_zone", () ->
                    AttachmentType.builder(ZoneAttachment::new).build()
            );

    @Nullable
    private Zone currentZone;

    @Nullable
    public Zone getCurrentZone() {
        return currentZone;
    }

    public void setCurrentZone(@Nullable Zone zone) {
        this.currentZone = zone;
    }
}
