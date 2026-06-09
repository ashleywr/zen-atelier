package com.sanhiruzu.atelier.synthesis.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class GelItem extends Item {
    private final boolean ember;

    public GelItem(Properties properties, boolean ember) {
        super(properties);
        this.ember = ember;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(ember ? "tooltip.zen_atelier.ember_gel" : "tooltip.zen_atelier.aqua_gel")
                .withStyle(ChatFormatting.GRAY));
    }
}
