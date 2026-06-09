package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.ZenAtelier;
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

public class InstantSalveItem extends Item {
    public InstantSalveItem(Properties properties) {
        super(properties);
    }

    // Regen II 80t / Regen II 120t / Regen III 80t / Regen III 140t
    private static final int[] REGEN_DURATION  = {80, 120,  80, 140};
    private static final int[] REGEN_AMPLIFIER = {1,    1,   2,   2};

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            SynthesisOutputData data = stack.get(ZenAtelier.SYNTHESIS_OUTPUT_DATA.get());
            int qt = data != null ? data.qualityTier() : 0;
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION[qt], REGEN_AMPLIFIER[qt]));
            SynthesisItemEvents.consumeUse(stack, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.zen_atelier.instant_salve").withStyle(ChatFormatting.GRAY));
    }
}
