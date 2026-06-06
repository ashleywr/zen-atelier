package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.client.AtelierKeys;
import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.world.ExtractionCauldronBlock;
import com.sanhiruzu.atelier.synthesis.world.ExtractionCauldronPhase;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientEvents.ModBusEvents::onRegisterGuiLayers);
        modEventBus.addListener(ClientBootstrap::onRegisterKeyMappings);
        modEventBus.addListener(ClientBootstrap::onRegisterMenuScreens);
        modEventBus.addListener(ClientBootstrap::onRegisterBlockColors);
        modEventBus.addListener(ClientBootstrap::onRegisterItemColors);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLevelUnload);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(AtelierKeys.SHOW_ZONE_BOUNDS);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ZenAtelier.SYNTHESIS_STATION_MENU.get(), SynthesisStationScreen::new);
    }

    private static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            ExtractionCauldronPhase phase = state.getValue(ExtractionCauldronBlock.PHASE);
            return phase.waterTint();
        }, ZenAtelier.EXTRACTION_CAULDRON.get());
    }

    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                ReagentItemColors::color,
                ZenAtelier.REAGENT.get(),
                ZenAtelier.CRUDE_MINING_COATING.get(),
                ZenAtelier.SPARKING_MINING_COATING.get(),
                ZenAtelier.KEEN_WEAPON_COATING.get(),
                ZenAtelier.SPARKING_WEAPON_COATING.get()
        );
    }
}
