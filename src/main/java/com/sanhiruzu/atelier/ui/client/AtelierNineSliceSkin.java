package com.sanhiruzu.atelier.ui.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class AtelierNineSliceSkin implements UiSkin {
    static final AtelierNineSliceSkin INSTANCE = new AtelierNineSliceSkin();

    private AtelierNineSliceSkin() {
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

    @Override
    public boolean drawTabFace(GuiGraphics graphics, ScreenRect rect, int accent, boolean selected, boolean active) {
        drawFace(graphics, rect, selected ? AtelierUiSprites.RECIPE_CELL_SELECTED : AtelierUiSprites.RECIPE_CELL);
        if (selected) {
            UiChrome.frame(graphics, rect, accent);
        } else if (!active) {
            graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0x66000000);
        }
        return true;
    }

    @Override
    public boolean drawButtonFace(GuiGraphics graphics, ScreenRect rect, int accent, boolean enabled, boolean hovered) {
        ResourceLocation sprite = enabled && hovered ? AtelierUiSprites.BUTTON_HOVERED : AtelierUiSprites.BUTTON;
        drawFace(graphics, rect, sprite);
        if (!enabled) {
            graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0x99000000);
        } else if (hovered) {
            UiChrome.frame(graphics, rect, accent);
        }
        return true;
    }

    @Override
    public boolean drawChipFace(GuiGraphics graphics, ScreenRect rect, int activeFrame, boolean active) {
        drawFace(graphics, rect, active ? AtelierUiSprites.RECIPE_CELL_SELECTED : AtelierUiSprites.RECIPE_CELL);
        if (active) {
            UiChrome.frame(graphics, rect, activeFrame);
        } else {
            graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0x44000000);
        }
        return true;
    }

    private static void drawFace(GuiGraphics graphics, ScreenRect rect, ResourceLocation sprite) {
        graphics.blitSprite(sprite, rect.x(), rect.y(), rect.width(), rect.height());
    }
}
