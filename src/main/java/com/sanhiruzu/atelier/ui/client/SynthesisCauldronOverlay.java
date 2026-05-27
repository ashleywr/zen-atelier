package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.AlchemyWandItem;
import com.sanhiruzu.atelier.synthesis.SynthesisCauldronBlockEntity;
import com.sanhiruzu.atelier.synthesis.SynthesisRecipe;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Optional;

public class SynthesisCauldronOverlay implements LayeredDraw.Layer {
    private static final int PANEL_WIDTH = 178;
    private static final int PANEL_HEIGHT = 78;
    private static final int PAD = 7;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) {
            return;
        }

        AlchemyWandItem wand = heldWand(mc);
        if (wand == null || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult hit = (BlockHitResult) mc.hitResult;
        if (!mc.level.getBlockState(hit.getBlockPos()).is(ZenAtelier.SYNTHESIS_CAULDRON.get())) {
            return;
        }

        if (!(mc.level.getBlockEntity(hit.getBlockPos()) instanceof SynthesisCauldronBlockEntity cauldron)) {
            return;
        }

        renderPanel(graphics, mc, cauldron, wand);
    }

    private static AlchemyWandItem heldWand(Minecraft mc) {
        if (mc.player.getMainHandItem().getItem() instanceof AlchemyWandItem wand) {
            return wand;
        }
        if (mc.player.getOffhandItem().getItem() instanceof AlchemyWandItem wand) {
            return wand;
        }
        return null;
    }

    private static void renderPanel(GuiGraphics graphics, Minecraft mc, SynthesisCauldronBlockEntity cauldron, AlchemyWandItem wand) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int x = screenWidth / 2 + 16;
        if (x + PANEL_WIDTH + 6 > screenWidth) {
            x = screenWidth / 2 - PANEL_WIDTH - 16;
        }
        int y = Math.max(8, screenHeight / 2 - PANEL_HEIGHT / 2);

        int alpha = 220;
        fillPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, alpha);
        graphics.drawString(mc.font, Component.translatable("overlay.zen_atelier.synthesis_cauldron"), x + PAD, y + 6, 0xFFE6EDF7);

        List<ItemStack> ingredients = cauldron.ingredients();
        for (int i = 0; i < 3; i++) {
            String text = i < ingredients.size()
                    ? ingredients.get(i).getHoverName().getString()
                    : "-";
            graphics.drawString(mc.font, fit(mc, text, PANEL_WIDTH - PAD * 2), x + PAD, y + 20 + i * 10, 0xFFB8C0CC);
        }

        String roomLine = roomLine();
        graphics.drawString(mc.font, fit(mc, roomLine, PANEL_WIDTH - PAD * 2), x + PAD, y + 52, 0xFFD7C48A);

        String resultLine = resultLine(cauldron, wand);
        graphics.drawString(mc.font, fit(mc, resultLine, PANEL_WIDTH - PAD * 2), x + PAD, y + 64, 0xFF9ED9C5);
    }

    private static String roomLine() {
        if (!ClientZoneData.isInZone()) {
            return Component.translatable("overlay.zen_atelier.not_in_room").getString();
        }

        String profiles = ClientZoneData.getCurrentActiveProfiles();
        int quality = ClientZoneData.getCurrentZenScore();
        boolean atelier = profiles != null && profiles.toLowerCase().contains("atelier");
        return atelier
                ? Component.translatable("overlay.zen_atelier.atelier_quality", quality).getString()
                : Component.translatable("overlay.zen_atelier.requires_atelier").getString();
    }

    private static String resultLine(SynthesisCauldronBlockEntity cauldron, AlchemyWandItem wand) {
        if (cauldron.ingredientCount() == 0) {
            return Component.translatable("overlay.zen_atelier.empty").getString();
        }
        if (cauldron.ingredientCount() < 3) {
            return Component.translatable("overlay.zen_atelier.add_ingredients", cauldron.ingredientCount(), 3).getString();
        }

        int quality = ClientZoneData.isInZone() ? ClientZoneData.getCurrentZenScore() : 0;
        boolean atelier = ClientZoneData.isInZone()
                && ClientZoneData.getCurrentActiveProfiles() != null
                && ClientZoneData.getCurrentActiveProfiles().toLowerCase().contains("atelier");
        if (!atelier) {
            return Component.translatable("overlay.zen_atelier.blocked_atelier").getString();
        }

        Optional<SynthesisRecipe> recipe = SynthesisRecipe.find(cauldron.ingredients(), wand.tier(), quality);
        if (recipe.isPresent()) {
            return Component.translatable("overlay.zen_atelier.result", Component.translatable("item." + recipe.get().displayOutputId().replace(':', '.'))).getString();
        }

        Optional<SynthesisRecipe> blocked = SynthesisRecipe.firstBlockedByTierOrQuality(cauldron.ingredients());
        if (blocked.isPresent()) {
            if (!wand.tier().atLeast(blocked.get().minimumTier())) {
                return Component.translatable("overlay.zen_atelier.blocked_wand", blocked.get().minimumTier().name().toLowerCase()).getString();
            }
            return Component.translatable("overlay.zen_atelier.blocked_quality", blocked.get().minimumAtelierQuality()).getString();
        }
        return Component.translatable("overlay.zen_atelier.no_recipe").getString();
    }

    private static void fillPanel(GuiGraphics graphics, int x, int y, int width, int height, int alpha) {
        graphics.fill(x, y, x + width, y + height, withAlpha(0x050608, alpha));
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, withAlpha(0x11161C, alpha));
        graphics.fill(x, y, x + width, y + 1, withAlpha(0xFFFFFF, 32));
        graphics.fill(x, y + height - 1, x + width, y + height, withAlpha(0x000000, 120));
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
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
}
