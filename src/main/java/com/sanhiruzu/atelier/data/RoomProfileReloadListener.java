package com.sanhiruzu.atelier.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public class RoomProfileReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public RoomProfileReloadListener() {
        super(GSON, "room_profiles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, RoomProfile> parsed = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            RoomProfile.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> LOGGER.error("Failed to parse room profile {}: {}", entry.getKey(), err))
                    .filter(this::hasRequiredMods)
                    .ifPresent(p -> parsed.put(entry.getKey(), p));
        }
        RoomProfileRegistry.replaceAll(parsed);
        LOGGER.info("Loaded {} room profiles", parsed.size());
    }

    private boolean hasRequiredMods(RoomProfile profile) {
        for (String modId : profile.requiredMods()) {
            if (!ModList.get().isLoaded(modId)) {
                return false;
            }
        }
        return true;
    }
}
