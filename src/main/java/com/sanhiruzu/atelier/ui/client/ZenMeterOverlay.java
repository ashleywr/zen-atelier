package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.client.ClientZoneCache;
import com.sanhiruzu.atelier.ui.adapter.ZoneHudAdapter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

public class ZenMeterOverlay implements LayeredDraw.Layer {
    private static final int PANEL_WIDTH = 186;
    private static final int PANEL_HEIGHT = 36;
    private static final int BAR_HEIGHT = 4;
    private static final int PAD = 8;
    private static final int BADGE_SIZE = 22;
    private static final int GAP = 6;
    private static final int VANILLA_HUD_GAP = 6;
    private float fadeAmount = 0.0f;

    private static void renderZoneHud(GuiGraphics graphics, Minecraft mc, int alpha) {
        int width = mc.getWindow().getGuiScaledWidth();
        int x = width / 2 - PANEL_WIDTH / 2;
        int y = hudY(mc);

        String activeProfiles = ClientZoneData.getCurrentActiveProfiles();
        boolean degraded = activeProfiles != null
                && (activeProfiles.startsWith("Open-air")
                || activeProfiles.startsWith("Exposed"));

        int score = ClientZoneData.getCurrentZenScore();
        String grade = gradeForScore(score);
        int fillColor = colorForCurrentZone(degraded);
        int contentX = x + PAD + BADGE_SIZE + GAP;
        int contentRight = x + PANEL_WIDTH - PAD;
        int contentWidth = Math.max(24, contentRight - contentX);

        renderPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, alpha, degraded ? 0x3A2612 : 0x101218);

        // Score badge
        int badgeX = x + PAD;
        int badgeY = y + 7;
        graphics.fill(badgeX, badgeY, badgeX + BADGE_SIZE, badgeY + BADGE_SIZE, withAlpha(fillColor, (int) (alpha * 0.28f)));
        graphics.fill(badgeX, badgeY + BADGE_SIZE - 1, badgeX + BADGE_SIZE, badgeY + BADGE_SIZE, withAlpha(fillColor, alpha));
        drawCentered(graphics, mc, grade, badgeX + BADGE_SIZE / 2, badgeY + (BADGE_SIZE - mc.font.lineHeight) / 2,
                withAlpha(fillColor, alpha));

        // Name line
        String nameText = fit(mc, ClientZoneData.getCurrentZoneName(), contentWidth);
        if (degraded && nameText != null && !nameText.isBlank()) {
            nameText = "! " + nameText;
        }
        if (nameText != null && !nameText.isBlank()) {
            graphics.drawString(mc.font, Component.literal(fit(mc, nameText, contentWidth)), contentX, y + 7,
                    withAlpha(degraded ? 0xFFCC75 : 0xF5F7FA, alpha));
        }

        // Track and fill
        int barX = contentX;
        int barY = y + 20;
        graphics.fill(barX, barY, barX + contentWidth, barY + BAR_HEIGHT,
                withAlpha(0x000000, (int) (alpha * 0.42f)));
        int fillWidth = Math.min(contentWidth, (int) (contentWidth * (score / 100.0f)));
        if (fillWidth > 0) {
            graphics.fill(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, withAlpha(fillColor, alpha));
        }
        graphics.fill(barX, barY + BAR_HEIGHT - 1, barX + contentWidth, barY + BAR_HEIGHT,
                withAlpha(0xFFFFFF, (int) (alpha * 0.12f)));

        String subtitle = ClientZoneData.hasDisplaySubtitle()
                ? ClientZoneData.getCurrentGeneratedName()
                : ClientZoneData.getCurrentActiveProfiles();
        if (subtitle != null && !subtitle.isBlank()) {
            graphics.drawString(mc.font, Component.literal(fit(mc, subtitle, contentWidth)), contentX, y + 27,
                    withAlpha(0xB8BDC8, alpha));
        }

        if (ClientZoneData.isDebugMode()) {
            String breakdown = fit(mc, ClientZoneData.getQualityBreakdown(), PANEL_WIDTH - 18);
            if (breakdown != null && !breakdown.isBlank()) {
                graphics.drawString(mc.font, Component.literal(breakdown), x + 9, y + PANEL_HEIGHT + 3,
                        withAlpha(0xAEB4C2, (int) (alpha * 0.86f)));
            }
        }
    }

    private static void renderGracePeriodHud(GuiGraphics graphics, Minecraft mc, int alpha) {
        int width = mc.getWindow().getGuiScaledWidth();
        int x = width / 2 - PANEL_WIDTH / 2;
        int y = hudY(mc);

        renderPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, alpha, 0x3A2612);
        graphics.fill(x, y, x + PANEL_WIDTH, y + 2, withAlpha(0xFFAA33, alpha));

        String zoneName = graceZoneName();
        graphics.drawString(mc.font, Component.literal("! " + fit(mc, zoneName, 122)), x + 9, y + 8,
                withAlpha(0xFFCC75, alpha));

        int secondsLeft = Math.max(1, (ClientZoneData.getGracePeriodTicksRemaining() + 19) / 20);
        graphics.drawString(mc.font,
                Component.literal("Closing in " + secondsLeft + "s"),
                x + 9, y + 22, withAlpha(0xFFDD99, alpha));
    }

    private static String graceZoneName() {
        var data = ClientZoneCache.getZone(ClientZoneData.getGracePeriodZoneId());
        if (data == null) return "Room";
        ZoneHudAdapter.ZoneHudSnapshot snap = ZoneHudAdapter.snapshotFromZoneData(data);
        return snap.name().isBlank() ? "Room" : snap.name();
    }

    private static void renderZoneLostFlash(GuiGraphics graphics, Minecraft mc, int alpha) {
        int width = mc.getWindow().getGuiScaledWidth();
        int x = width / 2 - PANEL_WIDTH / 2;
        int y = hudY(mc);

        renderPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, alpha, 0x2C1215);
        graphics.fill(x, y, x + PANEL_WIDTH, y + 2, withAlpha(0xFF4444, alpha));

        graphics.drawString(mc.font,
                Component.literal("Room lost"),
                x + 9, y + 8,
                withAlpha(0xFF7777, alpha));
        graphics.drawString(mc.font,
                Component.literal(fit(mc, "Re-enter or inspect", PANEL_WIDTH - PAD * 2)),
                x + 9, y + 22,
                withAlpha(0xD7A0A0, alpha));
    }

    public static int colorForCurrentZone() {
        return colorForCurrentZone(false);
    }

    private static int colorForCurrentZone(boolean degraded) {
        // Type info now lives in the subtitle (activeProfiles), not the name
        return RoomHudColors.forTypeInfo(ClientZoneData.getCurrentActiveProfiles(), degraded);
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private static void renderPanel(GuiGraphics graphics, int x, int y, int width, int height, int alpha, int tint) {
        graphics.fill(x, y, x + width, y + height, withAlpha(0x050608, (int) (alpha * 0.72f)));
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, withAlpha(tint, (int) (alpha * 0.76f)));
        graphics.fill(x, y, x + width, y + 1, withAlpha(0xFFFFFF, (int) (alpha * 0.16f)));
        graphics.fill(x, y + height - 1, x + width, y + height, withAlpha(0x000000, (int) (alpha * 0.5f)));
    }

    private static String gradeForScore(int score) {
        if (score >= 90) return "S";
        if (score >= 75) return "A";
        if (score >= 55) return "B";
        if (score >= 35) return "C";
        return "D";
    }

    private static void drawCentered(GuiGraphics graphics, Minecraft mc, String text, int centerX, int y, int color) {
        graphics.drawString(mc.font, Component.literal(text), centerX - mc.font.width(text) / 2, y, color);
    }

    private static String fit(Minecraft mc, String text, int maxWidth) {
        if (text == null || mc.font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && mc.font.width(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : text.substring(0, end) + ellipsis;
    }

    private static int hudY(Minecraft mc) {
        int height = mc.getWindow().getGuiScaledHeight();
        int vanillaHudHeight = Math.max(mc.gui.leftHeight, mc.gui.rightHeight);
        return Math.max(VANILLA_HUD_GAP, height - vanillaHudHeight - PANEL_HEIGHT - VANILLA_HUD_GAP);
    }

    private static void reserveHudSpace(Minecraft mc) {
        int reservedHeight = PANEL_HEIGHT + VANILLA_HUD_GAP;
        mc.gui.leftHeight += reservedHeight;
        mc.gui.rightHeight += reservedHeight;
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        boolean inZone = ClientZoneData.isInZone();
        boolean showRoomHud = ClientZoneData.shouldShowRoomHud();
        boolean renderedHud = false;

        // Normal zone HUD
        if (showRoomHud) {
            fadeAmount = Math.min(1.0f, fadeAmount + 0.05f);
        } else {
            fadeAmount = Math.max(0.0f, fadeAmount - 0.05f);
        }

        if (fadeAmount > 0.0f && (inZone || showRoomHud)) {
            renderZoneHud(graphics, mc, (int) (fadeAmount * 255));
            renderedHud = true;
        }

        // Grace period countdown — shown when the zone lost its entry but hasn't expired yet.
        // Takes priority over the "room lost" flash (they are mutually exclusive).
        if (!inZone && ClientZoneData.isShowingGracePeriod()) {
            renderGracePeriodHud(graphics, mc, 220);
            renderedHud = true;
        } else if (ClientZoneData.isShowingZoneLost()) {
            // "Room lost" flash — shown briefly when the zone disappears while the player
            // was inside it. Fades independently of the normal HUD so both can coexist
            // during the crossover (e.g. walking out and back quickly).
            float lostFade = ClientZoneData.zoneLostFade();
            // Ease-in: opaque for first 60 %, then fade out
            float displayFade = lostFade > 0.4f ? 1.0f : lostFade / 0.4f;
            renderZoneLostFlash(graphics, mc, (int) (displayFade * 200));
            renderedHud = true;
        }

        if (renderedHud) {
            reserveHudSpace(mc);
        }
    }
}
