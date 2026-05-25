package com.sanhiruzu.atelier.data;

import com.sanhiruzu.atelier.space.zone.ZoneTypeRegistry;
import com.sanhiruzu.atelier.zone.naming.RoomEpithetReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public class DataReloadEventHandler {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ZoneDefinitionReloadListener());
        // Room profiles are what ZoneTypeRegistry rebuilds from — chain the rebuild so a
        // /reload (or world load) reflects the latest profile JSON immediately.
        event.addListener(new RoomProfileReloadListener() {
            @Override
            protected void apply(java.util.Map<net.minecraft.resources.ResourceLocation, com.google.gson.JsonElement> entries,
                                 net.minecraft.server.packs.resources.ResourceManager manager,
                                 net.minecraft.util.profiling.ProfilerFiller profiler) {
                super.apply(entries, manager, profiler);
                ZoneTypeRegistry.rebuildFromProfiles();
            }
        });
        event.addListener(new RoomEpithetReloadListener());
    }
}
