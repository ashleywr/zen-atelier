package com.sanhiruzu.atelier.ui.client;

import net.minecraft.client.gui.GuiGraphics;

enum SynthesisUiLayer {
    BASE(0),
    SPATIAL_BOARD(20),
    SPATIAL_BOARD_TEXT(30),
    ABOVE_VANILLA_SLOTS(300),
    CARRIED_REAGENT(360),
    TOOLTIP(500);

    private final int z;

    SynthesisUiLayer(int z) {
        this.z = z;
    }

    void run(GuiGraphics graphics, Runnable draw) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, z);
        draw.run();
        graphics.pose().popPose();
    }
}
