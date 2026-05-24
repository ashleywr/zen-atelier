package com.sanhiruzu.atelier.data;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RoomProfileRegistry {
    private static final Map<ResourceLocation, RoomProfile> ENTRIES = new LinkedHashMap<>();

    public static void replaceAll(Map<ResourceLocation, RoomProfile> next) {
        ENTRIES.clear();
        ENTRIES.putAll(next);
    }

    @Nullable
    public static RoomProfile get(ResourceLocation id) {
        return ENTRIES.get(id);
    }

    public static Collection<RoomProfile> all() {
        return Collections.unmodifiableCollection(ENTRIES.values());
    }
}
