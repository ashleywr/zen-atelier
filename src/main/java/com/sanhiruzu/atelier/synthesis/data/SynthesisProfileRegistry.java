package com.sanhiruzu.atelier.synthesis.data;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SynthesisProfileRegistry {
    private static final Map<ResourceLocation, SynthesisProfileDefinition> ENTRIES = new LinkedHashMap<>();

    private SynthesisProfileRegistry() {
    }

    public static void replaceAll(Map<ResourceLocation, SynthesisProfileDefinition> next) {
        ENTRIES.clear();
        ENTRIES.putAll(next);
    }

    @Nullable
    public static SynthesisProfileDefinition get(ResourceLocation id) {
        return ENTRIES.get(id);
    }

    public static Collection<SynthesisProfileDefinition> all() {
        return Collections.unmodifiableCollection(ENTRIES.values());
    }
}
