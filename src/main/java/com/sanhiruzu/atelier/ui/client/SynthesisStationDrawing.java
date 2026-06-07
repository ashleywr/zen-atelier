package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.ZenAtelier;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class SynthesisStationDrawing {
    private static final UiSkin SKIN = UiSkins.active();
    private static final ResourceLocation DARK_OAK_PLANKS =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/dark_oak_planks.png");

    private SynthesisStationDrawing() {
    }

    static void window(GuiGraphics graphics, ScreenRect rect) {
        SKIN.drawWindow(graphics, rect);
    }

    static void panel(GuiGraphics graphics, ScreenRect rect) {
        SKIN.drawPanel(graphics, rect);
    }

    static void recessedPanel(GuiGraphics graphics, ScreenRect rect) {
        SKIN.drawRecessedPanel(graphics, rect);
    }

    static void slotFrame(GuiGraphics graphics, ScreenRect rect) {
        SKIN.drawSlotFrame(graphics, rect);
    }

    static void recipeCell(GuiGraphics graphics, ScreenRect rect, boolean selected) {
        SKIN.drawRecipeCell(graphics, rect, selected);
    }

    static void tiledWood(GuiGraphics graphics, ScreenRect rect) {
        for (int y = rect.y(); y < rect.bottom(); y += 16) {
            int height = Math.min(16, rect.bottom() - y);
            for (int x = rect.x(); x < rect.right(); x += 16) {
                int width = Math.min(16, rect.right() - x);
                graphics.blit(DARK_OAK_PLANKS, x, y, 0, 0, width, height, 16, 16);
            }
        }
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xB816120F);
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, 0x55392B21);
    }

    static void frame(GuiGraphics graphics, ScreenRect rect, int color) {
        UiChrome.frame(graphics, rect, color);
    }

    static void innerHighlight(GuiGraphics graphics, ScreenRect rect, int color) {
        UiChrome.innerHighlight(graphics, rect, color);
    }

    static void unknownRecipeOutline(GuiGraphics graphics, int x, int y, int accent) {
        frame(graphics, new ScreenRect(x + 2, y + 2, 12, 12), accent);
        graphics.fill(x + 6, y + 4, x + 10, y + 6, accent);
        graphics.fill(x + 6, y + 10, x + 10, y + 12, accent);
    }

    static void searchBox(GuiGraphics graphics, ScreenRect rect) {
        UiChrome.searchBox(graphics, rect, SKIN.theme());
    }

    static void tab(GuiGraphics graphics, ScreenRect rect, int accent, boolean selected, boolean active) {
        UiChrome.tab(graphics, rect, SKIN, accent, selected, active);
    }

    static void recipeRow(GuiGraphics graphics, ScreenRect rect, int accent, boolean selected, boolean crafted) {
        UiChrome.recipeRow(graphics, rect, SKIN, accent, selected, crafted);
    }

    static void synthBoard(GuiGraphics graphics, SynthesisStationLayout layout, int offsetX, int offsetY, int accent) {
        ScreenRect board = layout.detailPanel.inset(4).offset(offsetX, offsetY);
        tiledWood(graphics, board);
        graphics.fill(board.x(), board.y(), board.right(), board.bottom(), 0x70100D0A);
        innerHighlight(graphics, board, 0x223C332B);
        sparkle(graphics, board.x() + 11, board.y() + 10, 0xFFE9D48D);
        sparkle(graphics, board.right() - 17, board.y() + 12, 0xFFE9D48D);
        sparkle(graphics, board.x() + 14, board.bottom() - 20, 0xFFD6F3C7);
        sparkle(graphics, board.right() - 19, board.bottom() - 21, 0xFFD6F3C7);

        ScreenRect core = layout.core().offset(offsetX, offsetY);
        for (int i = 0; i < 7; i++) {
            ScreenRect node = layout.synthesisNode(i).offset(offsetX, offsetY);
            connector(graphics, core, node, connectorColor(i, accent));
        }
        node(graphics, core, accent, true);
        for (int i = 0; i < 7; i++) {
            node(graphics, layout.synthesisNode(i).offset(offsetX, offsetY), connectorColor(i, accent), false);
        }
    }

    static void node(GuiGraphics graphics, ScreenRect rect, int accent, boolean filled) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        graphics.fill(cx - 8, cy - 10, cx + 8, cy + 10, 0xFF181512);
        graphics.fill(cx - 10, cy - 8, cx + 10, cy + 8, 0xFF181512);
        graphics.fill(cx - 7, cy - 9, cx + 7, cy - 7, accent);
        graphics.fill(cx - 7, cy + 7, cx + 7, cy + 9, accent);
        graphics.fill(cx - 9, cy - 7, cx - 7, cy + 7, accent);
        graphics.fill(cx + 7, cy - 7, cx + 9, cy + 7, accent);
        if (filled) {
            graphics.fill(cx - 5, cy - 5, cx + 5, cy + 5, 0xFF252019);
            frame(graphics, new ScreenRect(cx - 5, cy - 5, 10, 10), accent);
        }
    }

    static void meter(GuiGraphics graphics, ScreenRect rect, int fillColor, double amount) {
        UiChrome.meter(graphics, rect, SKIN, fillColor, amount);
    }

    static void smallIcon(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 3, y, x + 6, y + 3, color);
        graphics.fill(x + 1, y + 3, x + 8, y + 7, color);
        graphics.fill(x + 3, y + 7, x + 6, y + 10, color);
    }

    private static void connector(GuiGraphics graphics, ScreenRect from, ScreenRect to, int color) {
        int x1 = from.x() + from.width() / 2;
        int y1 = from.y() + from.height() / 2;
        int x2 = to.x() + to.width() / 2;
        int y2 = to.y() + to.height() / 2;
        double dx = x2 - x1;
        double dy = y2 - y1;
        int length = (int) Math.round(Math.sqrt(dx * dx + dy * dy));
        if (length <= 0) {
            return;
        }
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        graphics.pose().pushPose();
        graphics.pose().translate(x1, y1, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.fill(8, -1, Math.max(9, length - 8), 1, 0x66000000 | (color & 0x00FFFFFF));
        graphics.fill(8, 0, Math.max(9, length - 8), 2, color);
        graphics.pose().popPose();
    }

    private static int connectorColor(int index, int accent) {
        return switch (index) {
            case 0 -> 0xFFE37A61;
            case 1 -> 0xFFD38BC8;
            case 2 -> 0xFF7ECBE9;
            case 3 -> 0xFFA6E3E0;
            case 4 -> 0xFFC987F4;
            case 5 -> 0xFF79D875;
            case 6 -> 0xFFE7D656;
            default -> accent;
        };
    }

    private static void sparkle(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 1, y, x + 2, y + 3, color);
        graphics.fill(x, y + 1, x + 3, y + 2, color);
    }
}
