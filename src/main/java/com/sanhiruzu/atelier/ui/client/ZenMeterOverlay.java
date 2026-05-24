package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.client.ClientZoneCache;
import com.sanhiruzu.atelier.ui.adapter.ZoneHudAdapter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

public class ZenMeterOverlay implements LayeredDraw.Layer {
    private float fadeAmount = 0.0f;

    // Bar constants — shared between normal and flash renders
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        boolean inZone = ClientZoneData.isInZone();
        boolean showRoomHud = ClientZoneData.shouldShowRoomHud();

        // Normal zone HUD
        if (showRoomHud) {
            fadeAmount = Math.min(1.0f, fadeAmount + 0.05f);
        } else {
            fadeAmount = Math.max(0.0f, fadeAmount - 0.05f);
        }

        if (fadeAmount > 0.0f && (inZone || showRoomHud)) {
            renderZoneHud(graphics, Minecraft.getInstance(), (int) (fadeAmount * 255));
        }

        // Grace period countdown — shown when the zone lost its entry but hasn't expired yet.
        // Takes priority over the "room lost" flash (they are mutually exclusive).
        if (!inZone && ClientZoneData.isShowingGracePeriod()) {
            renderGracePeriodHud(graphics, Minecraft.getInstance(), 220);
        } else if (ClientZoneData.isShowingZoneLost()) {
            // "Room lost" flash — shown briefly when the zone disappears while the player
            // was inside it. Fades independently of the normal HUD so both can coexist
            // during the crossover (e.g. walking out and back quickly).
            float lostFade = ClientZoneData.zoneLostFade();
            // Ease-in: opaque for first 60 %, then fade out
            float displayFade = lostFade > 0.4f ? 1.0f : lostFade / 0.4f;
            renderZoneLostFlash(graphics, Minecraft.getInstance(), (int) (displayFade * 200));
        }
    }

    private static void renderZoneHud(GuiGraphics graphics, Minecraft mc, int alpha) {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int x = width / 2 - 91;
        int y = height - 45;

        String activeProfiles = ClientZoneData.getCurrentActiveProfiles();
        boolean degraded = activeProfiles != null
                && (activeProfiles.startsWith("Open-air")
                || activeProfiles.startsWith("Exposed"));

        // Background track
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT,
                withAlpha(0x000000, (int) (alpha * 0.66f)));

        // Fill — amber for degraded rooms, theme colour otherwise
        int fillColor = degraded ? 0xFF8C00 : colorForCurrentZone();
        int fillWidth = (int) (BAR_WIDTH * (ClientZoneData.getCurrentZenScore() / 100.0f));
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, withAlpha(fillColor, alpha));
        }

        // Name line — add ⚠ prefix for degraded rooms so it's immediately obvious
        String nameText = ClientZoneData.getCurrentZoneName();
        if (degraded && nameText != null) {
            nameText = "⚠ " + nameText;
        }
        if (nameText != null) {
            graphics.drawString(mc.font, Component.literal(nameText), x, y - 10,
                    withAlpha(degraded ? 0xFFAA00 : 0xFFFFFF, alpha));
        }

        // Subtitle
        String subtitle = ClientZoneData.hasDisplaySubtitle()
                ? ClientZoneData.getCurrentGeneratedName()
                : ClientZoneData.getCurrentActiveProfiles();
        if (subtitle != null && !subtitle.isBlank()) {
            graphics.drawString(mc.font, Component.literal(subtitle), x, y + 8,
                    withAlpha(0xD8D8D8, alpha));
        }
    }

    private static void renderGracePeriodHud(GuiGraphics graphics, Minecraft mc, int alpha) {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int x = width / 2 - 91;
        int y = height - 45;

        // Empty bar with amber tint
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT,
                withAlpha(0x000000, (int) (alpha * 0.66f)));
        graphics.fill(x, y, x + BAR_WIDTH, y + 1, withAlpha(0xFF8800, alpha));

        String zoneName = graceZoneName();
        graphics.drawString(mc.font, Component.literal("⚠ " + zoneName), x, y - 10,
                withAlpha(0xFFAA00, alpha));

        int secondsLeft = Math.max(1, (ClientZoneData.getGracePeriodTicksRemaining() + 19) / 20);
        graphics.drawString(mc.font,
                Component.literal("Closing in " + secondsLeft + "s"),
                x, y + 8, withAlpha(0xFFCC66, alpha));
    }

    private static String graceZoneName() {
        var data = ClientZoneCache.getZone(ClientZoneData.getGracePeriodZoneId());
        if (data == null) return "Room";
        ZoneHudAdapter.ZoneHudSnapshot snap = ZoneHudAdapter.snapshotFromZoneData(data);
        return snap.name().isBlank() ? "Room" : snap.name();
    }

    private static void renderZoneLostFlash(GuiGraphics graphics, Minecraft mc, int alpha) {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int x = width / 2 - 91;
        int y = height - 45;

        // Empty / greyed-out bar
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT,
                withAlpha(0x000000, (int) (alpha * 0.66f)));
        // Thin red edge to signal removal
        graphics.fill(x, y, x + BAR_WIDTH, y + 1, withAlpha(0xFF3333, alpha));

        graphics.drawString(mc.font,
                Component.literal("✗ Room lost"),
                x, y - 10,
                withAlpha(0xFF5555, alpha));
    }

    private static int colorForCurrentZone() {
        // Type info now lives in the subtitle (activeProfiles), not the name
        String typeInfo = ClientZoneData.getCurrentActiveProfiles();
        if (typeInfo == null) return 0xFFC0CB;
        if (typeInfo.contains("Enchanting")) return 0x9370DB;
        if (typeInfo.contains("Greenhouse")) return 0x90EE90;
        if (typeInfo.contains("Partial")) return 0xFFD36E;
        return 0xFFC0CB;
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }
}
