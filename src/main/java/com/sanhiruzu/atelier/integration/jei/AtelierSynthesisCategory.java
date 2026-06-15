package com.sanhiruzu.atelier.integration.jei;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.menu.SynthesisStationMenu;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class AtelierSynthesisCategory implements IRecipeCategory<JeiSynthesisRecipe> {
    private static final int WIDTH = 170;
    private static final int HEIGHT = 96;
    private static final int MAX_VISIBLE_REQUIREMENTS = 4;
    private static final int MAX_VISIBLE_OUTPUTS = 4;

    private final IDrawable icon;
    private final IDrawable arrow;

    AtelierSynthesisCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ZenAtelier.SYNTHESIS_STATION_ITEM.get()));
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<JeiSynthesisRecipe> getRecipeType() {
        return AtelierJeiRecipeTypes.SYNTHESIS;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.zen_atelier.category.synthesis");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, JeiSynthesisRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 72, 72)
                .setStandardSlotBackground()
                .addItemStack(new ItemStack(ZenAtelier.SYNTHESIS_STATION_ITEM.get()));

        int visibleRequirements = Math.min(recipe.requirements().size(), MAX_VISIBLE_REQUIREMENTS);
        for (int i = 0; i < visibleRequirements; i++) {
            JeiSynthesisRecipe.RequirementDisplay requirement = recipe.requirements().get(i);
            builder.addInputSlot(8 + (i % 2) * 28, 18 + (i / 2) * 28)
                    .setStandardSlotBackground()
                    .addItemStacks(requirement.stacks())
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(requirementTooltip(requirement).copy().withStyle(ChatFormatting.GRAY)));
        }
        for (int i = visibleRequirements; i < recipe.requirements().size(); i++) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                    .addIngredients(VanillaTypes.ITEM_STACK, recipe.requirements().get(i).stacks());
        }

        int visibleOutputs = Math.min(recipe.outputs().size(), MAX_VISIBLE_OUTPUTS);
        for (int i = 0; i < visibleOutputs; i++) {
            builder.addOutputSlot(118 + (i % 2) * 24, 18 + (i / 2) * 24)
                    .setStandardSlotBackground()
                    .addItemStack(recipe.outputs().get(i));
        }
        if (recipe.outputs().size() > visibleOutputs) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                    .addIngredients(VanillaTypes.ITEM_STACK, recipe.outputs().subList(visibleOutputs, recipe.outputs().size()));
        }
    }

    @Override
    public void draw(JeiSynthesisRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        arrow.draw(graphics, 78, 34);
        graphics.drawString(font, Component.translatable(SynthesisRecipeCategory.translationKey(recipe.profile().category())),
                7, 4, SynthesisRecipeCategory.color(recipe.profile().category()), false);
        graphics.drawString(font, Component.translatable("jei.zen_atelier.synthesis.outputs"), 116, 4, 0xFF6B5A42, false);

        for (int i = 0; i < Math.min(recipe.requirements().size(), MAX_VISIBLE_REQUIREMENTS); i++) {
            int x = 26 + (i % 2) * 28;
            int y = 27 + (i / 2) * 28;
            graphics.drawString(font, "x" + recipe.requirements().get(i).requirement().amount(), x, y, 0xFF806F56, false);
        }

        int pct = successPct(recipe.profile());
        graphics.drawString(font, Component.literal(pct + "%"), 7, 74, 0xFF806F56, false);
        graphics.drawString(font, Component.literal("success"), 7, 83, 0xFF4A3C2C, false);
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(JeiSynthesisRecipe recipe) {
        return recipe.id();
    }

    private static int successPct(SynthesisProfile profile) {
        SynthesisProfile effective = SynthesisStationMenu.effectiveProfile(profile, 0);
        int total = effective.outcomes().stream().mapToInt(o -> o.weight()).sum();
        int success = effective.outcomes().stream().filter(o -> o.outcomeClass().successful()).mapToInt(o -> o.weight()).sum();
        return total > 0 ? (int) Math.round(100.0 * success / total) : 0;
    }

    private static Component requirementTooltip(JeiSynthesisRecipe.RequirementDisplay display) {
        ReagentQuery query = display.requirement().query();
        StringBuilder text = new StringBuilder();
        text.append("Requires ").append(display.requirement().amount()).append(" reagent");
        if (!query.reagentIds().isEmpty()) {
            text.append(": ").append(readable(query.reagentIds().stream().sorted().toList()));
        }
        if (!query.minElements().isEmpty()) {
            text.append(" | Elements ").append(formatElements(query.minElements()));
        }
        if (!query.requiredTraits().isEmpty()) {
            text.append(" | Traits ").append(readable(query.requiredTraits().stream().sorted().toList()));
        }
        if (query.minTier() > 1 || query.minQuality() > 0 || query.minPurity() > 0 || query.maxInstability() < 100) {
            text.append(" | Tier ").append(query.minTier()).append("-").append(query.maxTier())
                    .append(", Quality ").append(query.minQuality()).append("+")
                    .append(", Purity ").append(query.minPurity()).append("+")
                    .append(", Instability <= ").append(query.maxInstability());
        }
        return Component.literal(text.toString());
    }

    private static String formatElements(Map<String, Integer> elements) {
        return elements.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private static String readable(List<String> ids) {
        return ids.stream()
                .map(id -> id.contains(":") ? id.substring(id.indexOf(':') + 1) : id)
                .map(id -> id.replace('_', ' '))
                .collect(Collectors.joining(", "));
    }
}
