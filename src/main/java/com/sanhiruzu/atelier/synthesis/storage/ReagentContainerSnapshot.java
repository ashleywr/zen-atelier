package com.sanhiruzu.atelier.synthesis.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;

import java.util.List;

public record ReagentContainerSnapshot(List<ReagentStack> entries) {
    public static final Codec<ReagentContainerSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ReagentStack.CODEC.listOf().fieldOf("entries").forGetter(ReagentContainerSnapshot::entries)
    ).apply(instance, ReagentContainerSnapshot::new));

    public ReagentContainerSnapshot {
        entries = List.copyOf(entries);
    }

    public static ReagentContainerSnapshot fromContainer(ReagentContainer container) {
        return new ReagentContainerSnapshot(container.entries());
    }

    public ReagentContainer toContainer() {
        ReagentContainer container = new ReagentContainer();
        for (ReagentStack entry : entries) {
            container.insert(entry);
        }
        return container;
    }
}
