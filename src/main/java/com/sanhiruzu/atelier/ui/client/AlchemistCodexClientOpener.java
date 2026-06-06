package com.sanhiruzu.atelier.ui.client;

import net.minecraft.client.Minecraft;

public final class AlchemistCodexClientOpener {
    private AlchemistCodexClientOpener() {
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new AlchemistCodexScreen());
    }
}
