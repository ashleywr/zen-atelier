package com.sanhiruzu.atelier.ui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

final class SynthesisStationButton extends Button {
    private final int accent;

    private SynthesisStationButton(Button.Builder builder, int accent) {
        super(builder);
        this.accent = accent;
    }

    static Button build(Button.Builder builder, int accent) {
        return builder.build(buttonBuilder -> new SynthesisStationButton(buttonBuilder, accent));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRect rect = new ScreenRect(getX(), getY(), getWidth(), getHeight());
        int frame = active ? (isHoveredOrFocused() ? accent : SynthesisScreenTheme.PANEL_LIGHT) : SynthesisScreenTheme.PANEL;
        int fill = active ? (isHoveredOrFocused() ? SynthesisScreenTheme.PANEL_LIGHT : SynthesisScreenTheme.PANEL_DARK) : 0xFF1A1715;

        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
        SynthesisStationDrawing.frame(graphics, rect, frame);
        SynthesisStationDrawing.innerHighlight(graphics, rect, active ? 0x554A4036 : 0x33332D28);

        Font font = Minecraft.getInstance().font;
        String text = SynthesisStationText.fitWidth(font, getMessage().getString(), rect.width() - 8);
        int color = active ? SynthesisScreenTheme.TEXT : SynthesisScreenTheme.MUTED;
        int textX = rect.x() + (rect.width() - font.width(text)) / 2;
        int textY = rect.y() + (rect.height() - 8) / 2;
        graphics.drawString(font, text, textX, textY, color, false);
    }
}
