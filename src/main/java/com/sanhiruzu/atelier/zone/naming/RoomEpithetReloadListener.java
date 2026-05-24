package com.sanhiruzu.atelier.zone.naming;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class RoomEpithetReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomEpithetReloadListener.class);
    private static final Gson GSON = new Gson();

    public RoomEpithetReloadListener() {
        super(GSON, "room_epithets");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.push("zen_atelier:room_epithets");
        RoomEpithetRegistry.clear();

        objects.forEach((location, json) -> {
            try {
                boolean catalogFile = json.isJsonObject() && json.getAsJsonObject().has("room_epithets");
                List<RoomEpithet> epithets = catalogFile
                        ? RoomEpithetCatalog.CODEC.parse(JsonOps.INSTANCE, json)
                          .resultOrPartial(error -> LOGGER.error("Failed to parse room epithet catalog {}: {}", location, error))
                          .map(RoomEpithetCatalog::roomEpithets)
                          .orElse(List.of())
                        : RoomEpithet.CODEC.parse(JsonOps.INSTANCE, json)
                          .resultOrPartial(error -> LOGGER.error("Failed to parse room epithet {}: {}", location, error))
                          .map(List::of)
                          .orElse(List.of());

                for (RoomEpithet epithet : epithets) {
                    RoomEpithetRegistry.register(epithet);
                    LOGGER.info("Loaded room epithet: {}", epithet.id());
                }
            } catch (Exception e) {
                LOGGER.error("Error loading room epithet from {}", location, e);
            }
        });

        profiler.pop();
    }
}
