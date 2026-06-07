package com.sanhiruzu.atelier.ui.client;

import net.minecraft.client.gui.GuiGraphics;

final class UiChrome {
    private UiChrome() {
    }

    static void frame(GuiGraphics graphics, ScreenRect rect, int color) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + UiMetrics.FRAME, color);
        graphics.fill(rect.x(), rect.bottom() - UiMetrics.FRAME, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x(), rect.y(), rect.x() + UiMetrics.FRAME, rect.bottom(), color);
        graphics.fill(rect.right() - UiMetrics.FRAME, rect.y(), rect.right(), rect.bottom(), color);
    }

    static void innerHighlight(GuiGraphics graphics, ScreenRect rect, int color) {
        graphics.fill(rect.x() + UiMetrics.FRAME, rect.y() + UiMetrics.FRAME, rect.right() - UiMetrics.FRAME, rect.y() + UiMetrics.INSET_SMALL, color);
        graphics.fill(rect.x() + UiMetrics.FRAME, rect.y() + UiMetrics.INSET_SMALL, rect.x() + UiMetrics.INSET_SMALL, rect.bottom() - UiMetrics.FRAME, color);
    }

    static void searchBox(GuiGraphics graphics, ScreenRect rect, UiTheme theme) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), theme.panelDarkest());
        frame(graphics, rect, 0xFF4E443A);
        innerHighlight(graphics, rect, 0x33342D27);
    }

    static void tab(GuiGraphics graphics, ScreenRect rect, UiSkin skin, int accent, boolean selected, boolean active) {
        if (skin.drawTabFace(graphics, rect, accent, selected, active)) {
            return;
        }
        UiTheme theme = skin.theme();
        int fill = selected ? 0xFF5B4D39 : active ? 0xFF302A25 : 0xFF1C1815;
        int frame = selected ? accent : active ? 0xFF5E5145 : 0xFF322B25;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
        frame(graphics, rect, frame);
        innerHighlight(graphics, rect, selected ? 0x55FFFFFF : 0x224A4037);
    }

    static void recipeRow(GuiGraphics graphics, ScreenRect rect, UiSkin skin, int accent, boolean selected, boolean emphasized) {
        if (skin.drawRecipeRow(graphics, rect, accent, selected, emphasized)) {
            return;
        }
        int fill = selected ? 0xFF5B4D38 : emphasized ? 0xFF312A24 : 0xFF251F1B;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
        frame(graphics, rect, selected ? accent : 0xFF4C4138);
        if (selected) {
            graphics.fill(rect.x() + UiMetrics.FRAME, rect.y() + UiMetrics.FRAME, rect.x() + UiMetrics.INSET_MEDIUM, rect.bottom() - UiMetrics.FRAME, accent);
        }
    }

    static void meter(GuiGraphics graphics, ScreenRect rect, UiSkin skin, int fillColor, double amount) {
        if (skin.drawMeter(graphics, rect, fillColor, amount)) {
            return;
        }
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xFF171411);
        frame(graphics, rect, 0xFF6A5B4B);
        int innerWidth = rect.width() - UiMetrics.INSET_SMALL;
        int width = Math.max(0, Math.min(innerWidth, (int) Math.round(innerWidth * amount)));
        graphics.fill(rect.x() + UiMetrics.FRAME, rect.y() + UiMetrics.FRAME, rect.x() + UiMetrics.FRAME + width, rect.bottom() - UiMetrics.FRAME, fillColor);
    }

    static void buttonFace(GuiGraphics graphics, ScreenRect rect, UiSkin skin, int accent, boolean enabled, boolean hovered) {
        if (skin.drawButtonFace(graphics, rect, accent, enabled, hovered)) {
            return;
        }
        UiTheme theme = skin.theme();
        int frame = enabled ? (hovered ? accent : theme.panelLight()) : theme.panel();
        int fill = enabled ? (hovered ? theme.panelLight() : theme.panelDark()) : 0xFF1A1715;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
        frame(graphics, rect, frame);
        innerHighlight(graphics, rect, enabled ? 0x554A4036 : 0x33332D28);
    }

    static void chipFace(GuiGraphics graphics, ScreenRect rect, UiSkin skin, int activeFrame, boolean active) {
        if (skin.drawChipFace(graphics, rect, activeFrame, active)) {
            return;
        }
        UiTheme theme = skin.theme();
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), active ? 0xFF3B332D : theme.panelDark());
        frame(graphics, rect, active ? activeFrame : 0xFF4A4037);
    }
}
