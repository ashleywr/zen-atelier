package com.sanhiruzu.atelier.synthesis.vfx;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.sanhiruzu.atelier.synthesis.vfx.data.ImpactVfxDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public class ImpactVfxReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public ImpactVfxReloadListener() {
        super(GSON, "atelier/impact_vfx");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ImpactVfxDefinition> parsed = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ImpactVfxDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> LOGGER.error("Failed to parse impact_vfx {}: {}", entry.getKey(), err))
                    .filter(def -> matchesId(entry.getKey(), def))
                    .ifPresent(def -> parsed.put(def.id(), def));
        }
        ImpactVfxRegistry.replaceAll(parsed);
        LOGGER.info("Loaded {} impact VFX profiles", parsed.size());
    }

    private static boolean matchesId(ResourceLocation file, ImpactVfxDefinition def) {
        if (file.equals(def.id())) {
            return true;
        }
        LOGGER.error("Skipping impact_vfx {} because embedded id is {}", file, def.id());
        return false;
    }
}
