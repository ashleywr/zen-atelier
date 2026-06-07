package com.sanhiruzu.atelier.ui.client;

import net.minecraft.client.gui.GuiGraphics;

final class AtelierSkin implements UiSkin {
    static final AtelierSkin INSTANCE = new AtelierSkin();

    private AtelierSkin() {
    }

    @Override
    public UiTheme theme() {
        return SynthesisScreenTheme.UI;
    }

    @Override
    public void drawWindow(GuiGraphics graphics, ScreenRect rect) {
        graphics.blitSprite(AtelierUiSprites.WINDOW, rect.x(), rect.y(), rect.width(), rect.height());
    }

    @Override
    public void drawPanel(GuiGraphics graphics, ScreenRect rect) {
        graphics.blitSprite(AtelierUiSprites.PANEL, rect.x(), rect.y(), rect.width(), rect.height());
    }

    @Override
    public void drawRecessedPanel(GuiGraphics graphics, ScreenRect rect) {
        graphics.blitSprite(AtelierUiSprites.RECESSED_PANEL, rect.x(), rect.y(), rect.width(), rect.height());
    }

    @Override
    public void drawSlotFrame(GuiGraphics graphics, ScreenRect rect) {
        graphics.blitSprite(AtelierUiSprites.SLOT, rect.x(), rect.y(), rect.width(), rect.height());
    }

    @Override
    public void drawRecipeCell(GuiGraphics graphics, ScreenRect rect, boolean selected) {
        graphics.blitSprite(selected ? AtelierUiSprites.RECIPE_CELL_SELECTED : AtelierUiSprites.RECIPE_CELL, rect.x(), rect.y(), rect.width(), rect.height());
    }
}
