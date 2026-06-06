package com.sanhiruzu.atelier.integration.jei;

import com.sanhiruzu.atelier.ZenAtelier;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

final class AtelierExtractionCategory implements IRecipeCategory<JeiExtractionRecipe> {
    private static final int WIDTH = 154;
    private static final int HEIGHT = 72;

    private final IDrawable icon;
    private final IDrawable arrow;

    AtelierExtractionCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get()));
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<JeiExtractionRecipe> getRecipeType() {
        return AtelierJeiRecipeTypes.EXTRACTION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.zen_atelier.category.extraction");
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
    public void setRecipe(IRecipeLayoutBuilder builder, JeiExtractionRecipe recipe, IFocusGroup focuses) {
        if (recipe.priming()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 8, 28)
                    .setStandardSlotBackground()
                    .addItemStack(new ItemStack(Items.CAULDRON));
            builder.addInputSlot(32, 28)
                    .setStandardSlotBackground()
                    .addIngredients(recipe.source());
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 56, 28)
                    .setStandardSlotBackground()
                    .addItemStack(new ItemStack(Items.CAMPFIRE));
            builder.addOutputSlot(120, 28)
                    .setOutputSlotBackground()
                    .addItemStack(recipe.output());
            return;
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 8, 28)
                .setStandardSlotBackground()
                .addItemStack(recipe.station());
        builder.addInputSlot(32, 28)
                .setStandardSlotBackground()
                .addIngredients(recipe.source());

        int visible = Math.min(recipe.reagentOutputs().size(), 4);
        for (int i = 0; i < visible; i++) {
            builder.addOutputSlot(96 + (i % 2) * 24, 16 + (i / 2) * 24)
                    .setStandardSlotBackground()
                    .addItemStack(recipe.reagentOutputs().get(i));
        }
        if (recipe.reagentOutputs().size() > visible) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                    .addIngredients(VanillaTypes.ITEM_STACK, recipe.reagentOutputs().subList(visible, recipe.reagentOutputs().size()));
        }
    }

    @Override
    public void draw(JeiExtractionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 66, 28);
        var font = Minecraft.getInstance().font;
        Component label = recipe.priming()
                ? Component.translatable("jei.zen_atelier.extraction.priming")
                : Component.translatable("jei.zen_atelier.extraction.heated");
        graphics.drawString(font, label, 6, 4, 0xFF6B5A42, false);
        if (recipe.priming()) {
            graphics.drawString(font, Component.translatable("jei.zen_atelier.extraction.water_heat"), 54, 52, 0xFF806F56, false);
        }
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(JeiExtractionRecipe recipe) {
        return recipe.id();
    }
}
