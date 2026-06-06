package com.sanhiruzu.atelier.ui.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

final class SynthesisStationLayoutDebug {
    private static final int PANEL_COLOR = 0xFF66D9EF;
    private static final int ACTION_COLOR = 0xFFA6E22E;
    private static final int DETAIL_COLOR = 0xFFF92672;
    private static final int RECIPE_COLOR = 0xFFF4BF75;
    private static final int INVENTORY_COLOR = 0xFFAE81FF;

    private SynthesisStationLayoutDebug() {
    }

    static void render(GuiGraphics graphics, Font font, SynthesisStationLayout layout, ScreenRect origin) {
        outline(graphics, font, absolute(layout.mainPanel, origin), "main", PANEL_COLOR);
        outline(graphics, font, absolute(layout.recipePanel, origin), "recipes", PANEL_COLOR);
        outline(graphics, font, absolute(layout.detailPanel, origin), "detail", PANEL_COLOR);
        outline(graphics, font, absolute(layout.previousButton, origin), "prev", ACTION_COLOR);
        outline(graphics, font, absolute(layout.nextButton, origin), "next", ACTION_COLOR);
        outline(graphics, font, absolute(layout.synthesizeButton, origin), "make", ACTION_COLOR);
        outline(graphics, font, absolute(layout.detailIcon, origin), "icon", DETAIL_COLOR);
        outline(graphics, font, absolute(layout.detailName, origin), "name", DETAIL_COLOR);
        outline(graphics, font, absolute(layout.requirementsList, origin), "req", DETAIL_COLOR);
        outline(graphics, font, absolute(layout.successBar, origin), "odds", ACTION_COLOR);
        outline(graphics, font, absolute(layout.outcomeList, origin), "outs", DETAIL_COLOR);
        for (int i = 0; i < 10; i++) {
            outline(graphics, font, absolute(layout.recipeCell(i), origin), "r" + i, RECIPE_COLOR);
        }
        for (int column = 0; column < 9; column++) {
            outline(graphics, font, absolute(layout.hotbarSlot(column), origin), "h" + column, INVENTORY_COLOR);
        }
    }

    private static void outline(GuiGraphics graphics, Font font, ScreenRect rect, String label, int color) {
        SynthesisStationDrawing.frame(graphics, rect, color);
        graphics.drawString(font, label, rect.x() + 2, rect.y() + 2, color, false);
    }

    private static ScreenRect absolute(ScreenRect rect, ScreenRect origin) {
        return rect.offset(origin.x(), origin.y());
    }
}
