package com.sanhiruzu.atelier.ui.client;

import net.minecraft.client.gui.GuiGraphics;

// Z-ordering for all Atelier UI rendering.
//
// Vanilla reference points (screen-relative z):
//   Slot backgrounds  ≈   0  (graphics.fill in renderBg)
//   Item icons        ≈  50  (renderFakeItem pushes z by 50)
//   Vanilla tooltips  ≈   0  (rendered at base pose, end of render())
//
// Popup note: z-depth is draw order only. Screens must gate mouseClicked()
// on popup visibility to prevent clicks from reaching widgets behind them.
public enum UiLayer {
    BACKGROUND(-10),
    BASE(0),
    PANEL(10),
    BOARD(20),
    BOARD_TEXT(30),
    ABOVE_ITEMS(300),
    POPUP(340),
    POPUP_CONTENT(360),
    CARRIED(390),
    TOOLTIP(500),
    TOOLTIP_DETAIL(520);

    final int z;

    UiLayer(int z) {
        this.z = z;
    }

    public void run(GuiGraphics graphics, Runnable draw) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, z);
        draw.run();
        graphics.pose().popPose();
    }
}
