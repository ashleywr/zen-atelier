package com.sanhiruzu.atelier.synthesis;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SynthesizedItem extends Item {
    public SynthesizedItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SynthesisResultComponents.appendTooltip(stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
