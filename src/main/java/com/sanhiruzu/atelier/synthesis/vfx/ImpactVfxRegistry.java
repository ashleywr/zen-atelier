package com.sanhiruzu.atelier.synthesis.vfx;

import com.sanhiruzu.atelier.synthesis.vfx.data.ImpactVfxDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/** Holds the loaded impact-VFX profiles, keyed by resource id. */
public final class ImpactVfxRegistry {
    private static volatile Map<ResourceLocation, ImpactVfxDefinition> profiles = Map.of();

    private ImpactVfxRegistry() {}

    public static void replaceAll(Map<ResourceLocation, ImpactVfxDefinition> incoming) {
        profiles = Map.copyOf(incoming);
    }

    public static Optional<ImpactVfxDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(profiles.get(id));
    }
}
