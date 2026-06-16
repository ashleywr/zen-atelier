package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.OutcomeWeight;
import com.sanhiruzu.atelier.synthesis.engine.RequirementStatus;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.item.SynthesisOutputItemFactory;
import com.sanhiruzu.atelier.synthesis.menu.SynthesisStationMenu;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static void renderRequirements(GuiGraphics graphics, Font font, SynthesisStationLayout layout, SynthesisPlan plan) {
        int y = layout.requirementsList.y();
        graphics.drawString(font, Component.literal("Needs"), layout.requirementsTitle.x(), layout.requirementsTitle.y(), SynthesisScreenTheme.MUTED, false);
        for (RequirementStatus status : plan.requirements()) {
            if (y + 8 > layout.requirementsList.bottom()) {
                break;
            }
            int color = status.satisfied() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.BAD;
            graphics.fill(layout.requirementsList.x(), y + 2, layout.requirementsList.x() + 4, y + 6, color);
            String text = SynthesisStationText.requirementLine(status);
            graphics.drawString(font, SynthesisStationText.fitWidth(font, text, layout.requirementsList.width() - 7), layout.requirementsList.x() + 7, y, color, false);
            y += 9;
        }
    }

    private static void renderPreview(GuiGraphics graphics, Font font, SynthesisStationLayout layout, SynthesisPlan plan) {
        int successWidth = Mth.floor(plan.preview().successProbability() * layout.successBar.width());
        graphics.fill(layout.successBar.x(), layout.successBar.y(), layout.successBar.right(), layout.successBar.bottom(), SynthesisScreenTheme.PANEL_LIGHT);
        graphics.fill(layout.successBar.x(), layout.successBar.y(), layout.successBar.x() + successWidth, layout.successBar.bottom(), plan.canSynthesize() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.ACCENT_DIM);

        String success = "Success " + SynthesisStationText.percent(plan.preview().successProbability());
        graphics.drawString(font, SynthesisStationText.fitWidth(font, success, 62), layout.outcomeList.x(), layout.outcomeList.y() - 10, plan.canSynthesize() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.MUTED, false);
        int y = layout.outcomeList.y();
        int shown = 0;
        for (OutcomeWeight weight : plan.preview().weights()) {
            int color = SynthesisStationText.outcomeColor(weight);
            String name = SynthesisStationText.outcomeName(weight);
            String percent = SynthesisStationText.percent(weight.probability());
            graphics.fill(layout.outcomeList.x(), y + 2, layout.outcomeList.x() + 4, y + 6, color);
            graphics.drawString(font, SynthesisStationText.fitWidth(font, name, 55), layout.outcomeList.x() + 7, y, color, false);
            graphics.drawString(font, percent, layout.outcomeList.x() + 76 - font.width(percent), y, color, false);
            y += 9;
            shown++;
            if (shown >= 2) {
                break;
            }
        }
    }

    private static void renderSynthesisBoard(GuiGraphics graphics, Font font, SynthesisStationLayout layout, SynthesisProfile profile, SynthesisPlan plan) {
        ItemStack output = SynthesisOutputItemFactory.previewStack(profile);
        ScreenRect core = layout.core();
        graphics.renderFakeItem(output, core.x() + 5, core.y() + 5);

        for (int i = 0; i < plan.requirements().size() && i < 7; i++) {
            RequirementStatus status = plan.requirements().get(i);
            ScreenRect node = layout.synthesisNode(i);
            graphics.renderFakeItem(requirementStack(status), node.x() + 3, node.y() + 3);
            if (!status.satisfied()) {
                graphics.fill(node.x() + 2, node.y() + 2, node.right() - 2, node.bottom() - 2, 0x881F1111);
            }
        }

        int baseX = 145;
        int y = 145;
        SynthesisStationDrawing.smallIcon(graphics, baseX - 13, y - 2, 0xFFFF704D);
        SynthesisStationDrawing.meter(graphics, new ScreenRect(baseX, y, 55, 7), 0xFFFF8758, plan.preview().successProbability());
        SynthesisStationDrawing.smallIcon(graphics, baseX + 69, y - 2, 0xFFA5E9FF);
        SynthesisStationDrawing.meter(graphics, new ScreenRect(baseX + 82, y, 55, 7), 0xFF7FC4FF, plan.preview().probabilityOf(OutcomeClass.PERFECT_SUCCESS));
        SynthesisStationDrawing.smallIcon(graphics, baseX + 151, y - 2, 0xFF8BC7FF);
        SynthesisStationDrawing.meter(graphics, new ScreenRect(baseX + 164, y, 55, 7), 0xFF7FCF82, 1.0D - plan.preview().failureProbability());

        renderCraftingInfo(graphics, font, profile, plan);
    }

    private static void renderCraftingInfo(GuiGraphics graphics, Font font, SynthesisProfile profile, SynthesisPlan plan) {
        int x = 20;
        int width = 103;
        int y = 163;
        int shown = 0;
        for (RequirementStatus status : plan.requirements()) {
            if (shown >= 2) {
                break;
            }
            int color = status.satisfied() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.BAD;
            String text = "Req: " + SynthesisStationText.requirementLine(status);
            graphics.drawString(font, SynthesisStationText.fitWidth(font, text, width), x, y + shown * 9, color, false);
            shown++;
        }

        if (shown == 0) {
            graphics.drawString(font, SynthesisStationText.fitWidth(font, "Yield: " + expectedYield(profile), width), x, y, SynthesisScreenTheme.TEXT, false);
            shown = 1;
        }

        String elements = SynthesisStationText.compactElementBudget(elementNeeds(plan));
        String bottom = "Elem: " + elements;
        int bottomColor = "None".equals(elements) ? SynthesisScreenTheme.MUTED : SynthesisScreenTheme.ACCENT;
        graphics.drawString(font, SynthesisStationText.fitWidth(font, bottom, width), x, y + shown * 9, bottomColor, false);

        if (shown < 2) {
            String traits = SynthesisStationText.compactTraitList(requiredTraits(plan));
            graphics.drawString(font, SynthesisStationText.fitWidth(font, "Traits: " + traits, width), x, y + (shown + 1) * 9, SynthesisScreenTheme.MUTED, false);
        }
    }

    private static ItemStack requirementStack(RequirementStatus status) {
        ReagentQuery query = status.requirement().query();
        String id = query.reagentIds().stream().sorted().findFirst().orElse(ReagentQuery.DEBUG_UNIVERSAL_REAGENT_ID);
        ReagentStack stack = new ReagentStack(
                id,
                Math.max(1, status.requirement().amount()),
                Math.max(1, query.minTier()),
                query.minQuality(),
                query.minPurity(),
                Math.min(100, query.maxInstability()),
                query.minElements(),
                sortedList(query.requiredTraits()),
                query.requiredSourceHints()
        );
        return ReagentItem.createStack(stack);
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

    private static int outputQuality(SynthesisProfile profile) {
        return profile.primaryOutput().map(SynthesisOutput::quality).orElse(0);
    }

    private static Map<String, Integer> elementNeeds(SynthesisPlan plan) {
        java.util.LinkedHashMap<String, Integer> elements = new java.util.LinkedHashMap<>();
        plan.requirements().stream()
                .map(RequirementStatus::requirement)
                .flatMap(requirement -> requirement.query().minElements().entrySet().stream())
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> elements.merge(entry.getKey(), entry.getValue(), Integer::sum));
        return Map.copyOf(elements);
    }

    private static Set<String> requiredTraits(SynthesisPlan plan) {
        return plan.requirements().stream()
                .map(RequirementStatus::requirement)
                .flatMap(requirement -> requirement.query().requiredTraits().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static List<String> sortedList(Set<String> ids) {
        return ids.stream().sorted().toList();
    }
}
