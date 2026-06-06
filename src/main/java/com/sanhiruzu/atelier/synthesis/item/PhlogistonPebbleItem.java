package com.sanhiruzu.atelier.synthesis.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public class PhlogistonPebbleItem extends Item {
    private static final int BURN_TIME_TICKS = 160;

    public PhlogistonPebbleItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getBurnTime(ItemStack itemStack, RecipeType<?> recipeType) {
        return BURN_TIME_TICKS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.zen_atelier.phlogiston_pebble").withStyle(ChatFormatting.GRAY));
    }
}
