package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.ZenAtelierTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class SynthesisItemEvents {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        Level level = event.getLevel();

        if (stack.is(ZenAtelierTags.Items.THROWABLE)) {
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5f,
                        0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
                AlchemicalThrowable entity = new AlchemicalThrowable(level, player);
                entity.setItem(stack);
                entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.5f, 1.0f);
                level.addFreshEntity(entity);
                consumeUse(stack, player);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!isFresh(stack) && stack.has(ZenAtelier.SYNTHESIS_OUTPUT_DATA.get())) {
            event.getToolTip().add(Component.translatable("tooltip.zen_atelier.synthesis_output.spent")
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    // True if the item has no synthesis durability at all, or has all uses remaining.
    // Partially-used items must not be accepted as synthesis inputs.
    public static boolean isFresh(ItemStack stack) {
        return !stack.isDamageableItem() || stack.getDamageValue() == 0;
    }

    // Decrement one use of durability, or shrink the stack if non-damageable.
    public static void consumeUse(ItemStack stack, LivingEntity user) {
        if (user instanceof Player player && player.getAbilities().instabuild) return;
        if (stack.isDamageableItem()) {
            int next = stack.getDamageValue() + 1;
            if (next >= stack.getMaxDamage()) {
                stack.shrink(1);
            } else {
                stack.setDamageValue(next);
            }
        } else {
            stack.shrink(1);
        }
    }
}
