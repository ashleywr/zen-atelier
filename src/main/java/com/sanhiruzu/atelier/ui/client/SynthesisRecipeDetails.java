package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory;
import com.sanhiruzu.atelier.synthesis.engine.OutcomeWeight;
import com.sanhiruzu.atelier.synthesis.engine.RequirementStatus;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.item.SynthesisOutputItemFactory;
import com.sanhiruzu.atelier.synthesis.menu.SynthesisStationMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class SynthesisRecipeDetails {
    private SynthesisRecipeDetails() {
    }

    static void render(GuiGraphics graphics, Font font, SynthesisStationMenu menu, SynthesisStationLayout layout, SynthesisProfile profile) {
        ScreenRect page = layout.recipeBookRightPage().inset(10);
        renderHeader(graphics, font, menu, page, profile);
        menu.currentPlan().ifPresent(plan -> {
            renderBookRequirements(graphics, font, page, plan);
            renderBookPreview(graphics, font, page, profile, plan);
        });
    }

    private static void renderHeader(GuiGraphics graphics, Font font, SynthesisStationMenu menu, ScreenRect page, SynthesisProfile profile) {
        ItemStack stack = SynthesisOutputItemFactory.previewStack(profile);
        boolean crafted = menu.isProfileCrafted(profile);
        int accent = SynthesisRecipeCategory.color(profile.category());
        String name = SynthesisStationText.profileName(profile);
        graphics.drawString(font, Component.literal("[RECIPE DETAILS]"), page.x(), page.y(), SynthesisScreenTheme.ACCENT, false);
        ScreenRect icon = new ScreenRect(page.x(), page.y() + 18, 36, 36);
        SynthesisStationDrawing.frame(graphics, icon, accent);
        graphics.renderFakeItem(stack, icon.x() + 10, icon.y() + 10);
        graphics.drawString(font, SynthesisStationText.fitWidth(font, name, page.width() - 48), page.x() + 46, page.y() + 23, crafted ? SynthesisScreenTheme.TEXT : SynthesisScreenTheme.MUTED, false);
        graphics.drawString(font, Component.literal(categoryName(profile.category())), page.x() + 46, page.y() + 36, SynthesisScreenTheme.MUTED, false);
    }

    private static void renderBookRequirements(GuiGraphics graphics, Font font, ScreenRect page, SynthesisPlan plan) {
        int x = page.x();
        int y = page.y() + 68;
        int width = 112;
        graphics.drawString(font, Component.literal("Needs"), x, y, SynthesisScreenTheme.MUTED, false);
        y += 11;
        for (RequirementStatus status : plan.requirements()) {
            if (y + 12 > page.bottom()) {
                break;
            }
            int color = status.satisfied() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.BAD;
            ScreenRect row = new ScreenRect(x, y, width, 11);
            graphics.fill(row.x(), row.y() + 8, row.right(), row.y() + 10, SynthesisScreenTheme.PANEL_LIGHT);
            int filled = Mth.floor(row.width() * Mth.clamp(status.availableAmount() / (float) status.requirement().amount(), 0.0F, 1.0F));
            graphics.fill(row.x(), row.y() + 8, row.x() + filled, row.y() + 10, color);
            graphics.drawString(font, SynthesisStationText.fitWidth(font, SynthesisStationText.requirementLine(status), width), row.x(), row.y(), color, false);
            y += 15;
        }
    }

    private static void renderBookPreview(GuiGraphics graphics, Font font, ScreenRect page, SynthesisProfile profile, SynthesisPlan plan) {
        int x = page.x() + 136;
        int y = page.y() + 68;
        int width = page.right() - x;
        graphics.drawString(font, Component.literal("Result"), x, y, SynthesisScreenTheme.MUTED, false);
        y += 11;
        graphics.drawString(font, SynthesisStationText.fitWidth(font, "Yield " + expectedYield(profile), width), x, y, SynthesisScreenTheme.TEXT, false);
        y += 13;

        String success = "Success " + SynthesisStationText.percent(plan.preview().successProbability());
        graphics.drawString(font, SynthesisStationText.fitWidth(font, success, width), x, y, plan.canSynthesize() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.MUTED, false);
        y += 13;
        int shown = 0;
        for (OutcomeWeight weight : plan.preview().weights()) {
            if (shown >= 3 || y + 8 > page.bottom()) {
                break;
            }
            int color = SynthesisStationText.outcomeColor(weight);
            String line = SynthesisStationText.outcomeName(weight) + " " + SynthesisStationText.percent(weight.probability());
            graphics.drawString(font, SynthesisStationText.fitWidth(font, line, width), x, y, color, false);
            y += 10;
            shown++;
        }
    }

    private static String expectedYield(SynthesisProfile profile) {
        List<Integer> counts = profile.outcomes().stream()
                .flatMap(outcome -> outcome.outputs().stream())
                .map(SynthesisOutput::count)
                .toList();
        if (counts.isEmpty()) {
            return "0";
        }
        int min = counts.stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = counts.stream().mapToInt(Integer::intValue).max().orElse(min);
        return min == max ? Integer.toString(min) : min + "-" + max;
    }

    private static String categoryName(String category) {
        String normalized = SynthesisRecipeCategory.normalize(category).replace('_', ' ');
        return normalized.isEmpty() ? normalized : Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

}
