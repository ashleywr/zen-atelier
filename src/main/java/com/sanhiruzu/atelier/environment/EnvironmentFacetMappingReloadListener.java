package com.sanhiruzu.atelier.environment;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class EnvironmentFacetMappingReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public EnvironmentFacetMappingReloadListener() {
        super(GSON, "atelier/environment_facet_mappings");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        EnvironmentFacetMappings mappings = EnvironmentFacetMappings.empty();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                mappings = mappings.mergedWith(parse(entry.getValue()));
            } catch (Exception e) {
                LOGGER.error("Failed to parse environment facet mapping {}: {}", entry.getKey(), e.getMessage());
            }
        }
        EnvironmentFacetSignals.replaceMappings(mappings);
        LOGGER.info("Loaded {} environment facet mapping entries", mappings.facetsToSignals().size());
    }

    static EnvironmentFacetMappings parse(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("mapping root must be an object");
        }
        JsonObject root = element.getAsJsonObject();
        JsonObject facets = root.getAsJsonObject("facets");
        if (facets == null) {
            return EnvironmentFacetMappings.empty();
        }
        LinkedHashMap<String, Set<String>> mappings = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : facets.entrySet()) {
            mappings.put(entry.getKey(), parseSignals(entry.getValue()));
        }
        return EnvironmentFacetMappings.of(mappings);
    }

    private static Set<String> parseSignals(JsonElement element) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        if (element == null) {
            return signals;
        }
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                signals.add(value.getAsString());
            }
            return signals;
        }
        signals.add(element.getAsString());
        return signals;
    }
}
