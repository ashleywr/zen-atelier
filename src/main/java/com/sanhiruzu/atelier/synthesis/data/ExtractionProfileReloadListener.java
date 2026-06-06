package com.sanhiruzu.atelier.synthesis.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExtractionProfileReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public ExtractionProfileReloadListener() {
        super(GSON, "atelier/extraction_profiles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ExtractionProfileDefinition> parsed = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ExtractionProfileDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> LOGGER.error("Failed to parse extraction profile {}: {}", entry.getKey(), err))
                    .filter(profile -> hasMatchingId(entry.getKey(), profile))
                    .ifPresent(profile -> parsed.put(entry.getKey(), profile));
        }
        ExtractionProfileRegistry.replaceAll(parsed);
        LOGGER.info("Loaded {} extraction profiles", parsed.size());
    }

    static boolean hasMatchingId(ResourceLocation resourceId, ExtractionProfileDefinition profile) {
        if (resourceId.equals(profile.id())) {
            return true;
        }
        LOGGER.error("Skipping extraction profile {} because embedded id is {}", resourceId, profile.id());
        return false;
    }
}
