package com.sanhiruzu.atelier.ui.client;

import net.minecraft.client.gui.GuiGraphics;

interface UiSkin {
    UiTheme theme();

    void drawWindow(GuiGraphics graphics, ScreenRect rect);

    void drawPanel(GuiGraphics graphics, ScreenRect rect);

    void drawRecessedPanel(GuiGraphics graphics, ScreenRect rect);

    void drawSlotFrame(GuiGraphics graphics, ScreenRect rect);

    void drawRecipeCell(GuiGraphics graphics, ScreenRect rect, boolean selected);

    default boolean drawTabFace(GuiGraphics graphics, ScreenRect rect, int accent, boolean selected, boolean active) {
        return false;
    }

    default boolean drawRecipeRow(GuiGraphics graphics, ScreenRect rect, int accent, boolean selected, boolean emphasized) {
        return false;
    }

    default boolean drawButtonFace(GuiGraphics graphics, ScreenRect rect, int accent, boolean enabled, boolean hovered) {
        return false;
    }

    default boolean drawChipFace(GuiGraphics graphics, ScreenRect rect, int activeFrame, boolean active) {
        return false;
    }

    default boolean drawMeter(GuiGraphics graphics, ScreenRect rect, int fillColor, double amount) {
        return false;
    }
}
