package com.sanhiruzu.atelier.synthesis;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class FlashBombItem extends SynthesizedItem {
    public FlashBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int quality = Math.max(20, SynthesisResultComponents.quality(stack));
            double radius = quality >= 70 ? 5.0D : 3.5D;
            int duration = 80 + quality * 2;
            AABB area = player.getBoundingBox().inflate(radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && entity.isAlive())) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
            }
            player.displayClientMessage(Component.translatable("message.zen_atelier.flash_bomb"), true);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
