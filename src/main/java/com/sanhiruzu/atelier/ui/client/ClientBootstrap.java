package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.client.AtelierKeys;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientEvents.ModBusEvents::onRegisterGuiLayers);
        modEventBus.addListener(ClientBootstrap::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLevelUnload);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(AtelierKeys.SHOW_ZONE_BOUNDS);
    }
}
