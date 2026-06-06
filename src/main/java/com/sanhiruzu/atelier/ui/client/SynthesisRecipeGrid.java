package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.item.SynthesisOutputItemFactory;
import com.sanhiruzu.atelier.synthesis.menu.SynthesisStationMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class SynthesisRecipeGrid {
    private SynthesisRecipeGrid() {
    }

    static void render(GuiGraphics graphics, Font font, SynthesisStationMenu menu, SynthesisStationLayout layout, ScreenRect origin, String selectedCategory) {
        List<SynthesisProfile> profiles = menu.profiles();
        List<Integer> visible = visibleProfileIndexes(menu, selectedCategory);
        if (visible.isEmpty()) {
            ScreenRect label = absolute(layout.emptyRecipeCategoryLabel, origin);
            graphics.drawCenteredString(font, Component.translatable("screen.zen_atelier.synthesis.empty_category"), label.x() + label.width() / 2, label.y(), SynthesisScreenTheme.MUTED);
            return;
        }

        for (int i = 0; i < visible.size() && i < SynthesisStationLayout.RECIPE_ROWS; i++) {
            int profileIndex = visible.get(i);
            SynthesisProfile profile = profiles.get(profileIndex);
            ScreenRect cell = absolute(layout.recipeCell(i), origin);
            boolean selected = menu.selectedProfileIndex() == profileIndex;
            boolean crafted = menu.isProfileCrafted(profile);
            int accent = SynthesisRecipeCategory.color(profile.category());
            SynthesisStationDrawing.recipeRow(graphics, cell, accent, selected, crafted);
            ItemStack stack = SynthesisOutputItemFactory.previewStack(profile);
            graphics.renderFakeItem(stack, cell.x() + 4, cell.y() + 2);
            int textColor = selected ? SynthesisScreenTheme.TEXT : crafted ? SynthesisScreenTheme.MUTED : 0xFF8C8277;
            String name = SynthesisStationText.profileName(profile);
            graphics.drawString(font, SynthesisStationText.fitWidth(font, name, cell.width() - 27), cell.x() + 25, cell.y() + 6, textColor, false);
        }
    }

    static void renderTooltip(GuiGraphics graphics, Font font, SynthesisStationMenu menu, SynthesisStationLayout layout, ScreenRect origin, String selectedCategory, int mouseX, int mouseY) {
        Optional<Integer> hovered = hoveredProfileIndex(menu, layout, origin, selectedCategory, mouseX, mouseY);
        if (hovered.isEmpty()) {
            return;
        }
        SynthesisProfile profile = menu.profiles().get(hovered.get());
        graphics.renderTooltip(font, SynthesisOutputItemFactory.previewStack(profile), mouseX, mouseY);
    }

    static Optional<Integer> hoveredProfileIndex(SynthesisStationMenu menu, SynthesisStationLayout layout, ScreenRect origin, String selectedCategory, int mouseX, int mouseY) {
        List<Integer> visible = visibleProfileIndexes(menu, selectedCategory);
        for (int i = 0; i < visible.size() && i < SynthesisStationLayout.RECIPE_ROWS; i++) {
            if (absolute(layout.recipeCell(i), origin).contains(mouseX, mouseY)) {
                return Optional.of(visible.get(i));
            }
        }
        return Optional.empty();
    }

    private static List<Integer> visibleProfileIndexes(SynthesisStationMenu menu, String selectedCategory) {
        List<SynthesisProfile> profiles = menu.profiles();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).category().equals(selectedCategory)) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private static ScreenRect absolute(ScreenRect rect, ScreenRect origin) {
        return rect.offset(origin.x(), origin.y());
    }
}
