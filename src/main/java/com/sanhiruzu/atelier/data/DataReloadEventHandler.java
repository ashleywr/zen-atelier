package com.sanhiruzu.atelier.data;

import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileReloadListener;
import com.sanhiruzu.atelier.synthesis.data.SynthesisProfileReloadListener;
import com.sanhiruzu.atelier.synthesis.data.TraitFusionReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public class DataReloadEventHandler {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ZoneDefinitionReloadListener());
        event.addListener(new ExtractionProfileReloadListener());
        event.addListener(new SynthesisProfileReloadListener());
        event.addListener(new RoomProfileReloadListener());
        event.addListener(new TraitFusionReloadListener());
        event.addListener(new com.sanhiruzu.atelier.synthesis.vfx.ImpactVfxReloadListener());
    }
}
