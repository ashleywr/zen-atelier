package com.sanhiruzu.atelier.ui;

import com.sanhiruzu.atelier.ZenAtelier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;

public final class UiBootstrap {
    private UiBootstrap() {
    }

    public static void registerClientIfPresent(IEventBus modEventBus) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        try {
            Class<?> bootstrap = Class.forName("com.sanhiruzu.atelier.ui.client.ClientBootstrap");
            bootstrap.getMethod("register", IEventBus.class).invoke(null, modEventBus);
        } catch (ReflectiveOperationException exception) {
            ZenAtelier.LOGGER.warn("Failed to register Atelier client UI bootstrap", exception);
        }
    }
}
