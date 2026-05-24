package com.sanhiruzu.atelier.data;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ZoneDefinitionRegistry {
    private static final Map<ResourceLocation, ZoneDefinition> ENTRIES = new LinkedHashMap<>();

    public static void replaceAll(Map<ResourceLocation, ZoneDefinition> next) {
        ENTRIES.clear();
        ENTRIES.putAll(next);
    }

    @Nullable
    public static ZoneDefinition get(ResourceLocation id) {
        return ENTRIES.get(id);
    }

    public static Collection<ZoneDefinition> all() {
        return Collections.unmodifiableCollection(ENTRIES.values());
    }
}
