package com.sanhiruzu.atelier.synthesis.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class GelItem extends Item {
    private final boolean ember;

    public GelItem(Properties properties, boolean ember) {
        super(properties);
        this.ember = ember;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 80, 0));
            if (ember) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0));
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(ember ? "tooltip.zen_atelier.ember_gel" : "tooltip.zen_atelier.aqua_gel")
                .withStyle(ChatFormatting.GRAY));
    }
}
