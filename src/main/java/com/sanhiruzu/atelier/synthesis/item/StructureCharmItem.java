package com.sanhiruzu.atelier.synthesis.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class StructureCharmItem extends Item {
    public StructureCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.zen_atelier.structure_charm").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.zen_atelier.structure_charm_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
