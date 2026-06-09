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

public class GelItem extends Item {
    private final boolean ember;

    public GelItem(Properties properties, boolean ember) {
        super(properties);
        this.ember = ember;
    }

    // Jump I 80t / Jump I 140t / Jump II 80t / Jump II 140t
    private static final int[] JUMP_DURATION  = { 80, 140,  80, 140};
    private static final int[] JUMP_AMPLIFIER = {  0,   0,   1,   1};
    // Fire resistance for ember gel
    private static final int[] FIRE_DURATION  = { 40,  80, 120, 200};

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            SynthesisOutputData data = stack.get(ZenAtelier.SYNTHESIS_OUTPUT_DATA.get());
            int qt = data != null ? data.qualityTier() : 0;
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, JUMP_DURATION[qt], JUMP_AMPLIFIER[qt]));
            if (ember) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FIRE_DURATION[qt], 0));
            }
            SynthesisItemEvents.consumeUse(stack, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(ember ? "tooltip.zen_atelier.ember_gel" : "tooltip.zen_atelier.aqua_gel")
                .withStyle(ChatFormatting.GRAY));
    }
}
