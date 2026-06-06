package com.sanhiruzu.atelier.synthesis.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class UniItem extends SnowballItem {
    public UniItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.zen_atelier.uni").withStyle(ChatFormatting.GRAY));
    }
}
