package com.sanhiruzu.atelier.synthesis.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TraitFusionReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public TraitFusionReloadListener() {
        super(GSON, "atelier/trait_fusions");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        List<TraitFusionRule> parsed = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                parseFile(entry.getKey(), entry.getValue().getAsJsonObject(), parsed);
            } catch (Exception e) {
                LOGGER.error("Failed to parse trait fusion file {}: {}", entry.getKey(), e.getMessage());
            }
        }
        TraitFusionRegistry.replaceAll(parsed);
        LOGGER.info("Loaded {} trait fusion rules", parsed.size());
    }

    private static void parseFile(ResourceLocation fileId, JsonObject obj, List<TraitFusionRule> out) {
        JsonArray rules = obj.getAsJsonArray("rules");
        if (rules == null) {
            LOGGER.error("Trait fusion file {} has no 'rules' array", fileId);
            return;
        }
        for (JsonElement elem : rules) {
            try {
                out.add(parseRule(fileId, elem.getAsJsonObject()));
            } catch (Exception e) {
                LOGGER.error("Skipping malformed trait fusion rule in {}: {}", fileId, e.getMessage());
            }
        }
    }

    private static TraitFusionRule parseRule(ResourceLocation fileId, JsonObject obj) {
        JsonArray inputs = obj.getAsJsonArray("inputs");
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException("'inputs' must be a 2-element array");
        }
        String traitA = inputs.get(0).getAsString();
        String traitB = inputs.get(1).getAsString();

        Optional<String> outputAffix = obj.has("output_affix")
                ? Optional.of(obj.get("output_affix").getAsString())
                : Optional.empty();
        int qualityBonus = obj.has("quality_bonus") ? obj.get("quality_bonus").getAsInt() : 0;
        int successWeightBonus = obj.has("success_weight_bonus") ? obj.get("success_weight_bonus").getAsInt() : 0;
        int color = obj.has("glow_color") ? parseColor(obj.get("glow_color").getAsString()) : 0xFFFFFFFF;

        String id = fileId.getNamespace() + ":" + TraitFusionRegistry.pairKey(traitA, traitB);
        return new TraitFusionRule(id, traitA, traitB, outputAffix, qualityBonus, successWeightBonus, color);
    }

    private static int parseColor(String hex) {
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        // Treat 6-char hex as fully opaque ARGB
        if (s.length() == 6) {
            return (int) (0xFF000000L | Long.parseLong(s, 16));
        }
        return (int) Long.parseLong(s, 16);
    }
}
