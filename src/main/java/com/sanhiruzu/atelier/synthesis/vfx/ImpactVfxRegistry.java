package com.sanhiruzu.atelier.synthesis.vfx;

import com.sanhiruzu.atelier.synthesis.vfx.data.ImpactVfxDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Holds the loaded impact-VFX profiles, keyed by resource id. */
public final class ImpactVfxRegistry {
    private static final Map<ResourceLocation, ImpactVfxDefinition> PROFILES = new ConcurrentHashMap<>();

    private ImpactVfxRegistry() {}

    public static void replaceAll(Map<ResourceLocation, ImpactVfxDefinition> profiles) {
        PROFILES.clear();
        PROFILES.putAll(profiles);
    }

    public static Optional<ImpactVfxDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(PROFILES.get(id));
    }
}
