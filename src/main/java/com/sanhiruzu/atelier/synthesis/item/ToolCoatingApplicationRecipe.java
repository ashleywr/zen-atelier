package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ToolCoatingApplicationRecipe extends CustomRecipe {
    public ToolCoatingApplicationRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findToolAndCoating(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ToolAndCoating match = findToolAndCoating(input);
        if (match == null) {
            return ItemStack.EMPTY;
        }
        return ToolCoatingApplicator.applyToCopy(match.toolStack(), match.coatingItem());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(Items.IRON_PICKAXE);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ZenAtelier.TOOL_COATING_APPLICATION_RECIPE.get();
    }

    private static ToolAndCoating findToolAndCoating(CraftingInput input) {
        ItemStack toolStack = ItemStack.EMPTY;
        ToolCoatingItem coatingItem = null;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof ToolCoatingItem item) {
                if (coatingItem != null) {
                    return null;
                }
                coatingItem = item;
            } else {
                if (!toolStack.isEmpty()) {
                    return null;
                }
                toolStack = stack;
            }
        }

        if (toolStack.isEmpty() || coatingItem == null || !ToolCoatingApplicator.canApplyTo(toolStack, coatingItem)) {
            return null;
        }
        return new ToolAndCoating(toolStack, coatingItem);
    }

    private record ToolAndCoating(ItemStack toolStack, ToolCoatingItem coatingItem) {
    }
}
