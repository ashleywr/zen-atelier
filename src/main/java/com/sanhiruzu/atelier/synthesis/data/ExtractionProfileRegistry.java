package com.sanhiruzu.atelier.synthesis.data;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ExtractionProfileRegistry {
    private static final Map<ResourceLocation, ExtractionProfileDefinition> ENTRIES = new LinkedHashMap<>();

    private ExtractionProfileRegistry() {
    }

    public static void replaceAll(Map<ResourceLocation, ExtractionProfileDefinition> next) {
        ENTRIES.clear();
        ENTRIES.putAll(next);
    }

    @Nullable
    public static ExtractionProfileDefinition get(ResourceLocation id) {
        return ENTRIES.get(id);
    }

    public static Collection<ExtractionProfileDefinition> all() {
        return Collections.unmodifiableCollection(ENTRIES.values());
    }
}
