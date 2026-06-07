package com.sanhiruzu.atelier.ui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

final class SynthesisStationButton extends Button {
    private static final UiSkin SKIN = UiSkins.active();
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
        UiChrome.buttonFace(graphics, rect, SKIN, accent, active, isHoveredOrFocused());

        Font font = Minecraft.getInstance().font;
        String text = SynthesisStationText.fitWidth(font, getMessage().getString(), rect.width() - UiMetrics.BUTTON_TEXT_PAD_X);
        int color = active ? SynthesisScreenTheme.TEXT : SynthesisScreenTheme.MUTED;
        int textX = rect.x() + (rect.width() - font.width(text)) / 2;
        int textY = rect.y() + (rect.height() - UiMetrics.TEXT_HEIGHT) / 2;
        graphics.drawString(font, text, textX, textY, color, false);
    }
}
