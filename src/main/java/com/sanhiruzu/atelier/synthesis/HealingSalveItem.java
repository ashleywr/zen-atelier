package com.sanhiruzu.atelier.synthesis;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HealingSalveItem extends SynthesizedItem {
    public HealingSalveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int quality = Math.max(20, SynthesisResultComponents.quality(stack));
            int duration = 120 + quality * 4;
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, quality >= 70 ? 1 : 0));
            player.displayClientMessage(Component.translatable("message.zen_atelier.healing_salve"), true);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
