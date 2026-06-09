package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.ZenAtelierTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class SynthesisItemEvents {

    // --- Instant Salve: Regen duration/amplifier by quality tier (0–3) ---
    private static final int[] REGEN_DURATION  = { 80, 120,  80, 140};
    private static final int[] REGEN_AMPLIFIER = {  1,   1,   2,   2};

    // --- Gel: Jump duration/amplifier by quality tier ---
    private static final int[] JUMP_DURATION   = { 80, 140,  80, 140};
    private static final int[] JUMP_AMPLIFIER  = {  0,   0,   1,   1};

    // --- Ember Gel: Fire resistance duration by quality tier ---
    private static final int[] FIRE_DURATION   = { 40,  80, 120, 200};

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

        } else if (stack.is(ZenAtelierTags.Items.CONSUMABLE)) {
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.6f,
                        level.getRandom().nextFloat() * 0.1f + 0.9f);
                SynthesisOutputData data = stack.get(ZenAtelier.SYNTHESIS_OUTPUT_DATA.get());
                int qt = data != null ? data.qualityTier() : 0;
                Item item = stack.getItem();
                if (item == ZenAtelier.INSTANT_SALVE.get()) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION[qt], REGEN_AMPLIFIER[qt]));
                } else if (item == ZenAtelier.AQUA_GEL.get()) {
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, JUMP_DURATION[qt], JUMP_AMPLIFIER[qt]));
                } else if (item == ZenAtelier.EMBER_GEL.get()) {
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, JUMP_DURATION[qt], JUMP_AMPLIFIER[qt]));
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FIRE_DURATION[qt], 0));
                }
                consumeUse(stack, player);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static final String[] QUALITY_TIER_KEYS = {
        "tooltip.zen_atelier.quality.crude",
        "tooltip.zen_atelier.quality.fine",
        "tooltip.zen_atelier.quality.superior",
        "tooltip.zen_atelier.quality.masterwork"
    };

    private static final ChatFormatting[] QUALITY_TIER_COLORS = {
        ChatFormatting.DARK_GRAY,
        ChatFormatting.GRAY,
        ChatFormatting.AQUA,
        ChatFormatting.GOLD
    };

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        SynthesisOutputData data = stack.get(ZenAtelier.SYNTHESIS_OUTPUT_DATA.get());
        if (data == null) return;

        int qt = data.qualityTier();
        event.getToolTip().add(
            Component.literal("Quality: ")
                .append(Component.translatable(QUALITY_TIER_KEYS[qt]))
                .append(Component.literal(" (" + data.quality() + ")"))
                .withStyle(QUALITY_TIER_COLORS[qt])
        );

        for (String affix : data.affixes()) {
            // "zen_atelier:freezing" → "zen_atelier.affix.freezing"
            String langKey = affix.replace(":", ".affix.");
            event.getToolTip().add(
                Component.literal("  ▪ ").append(Component.translatable(langKey))
                    .withStyle(ChatFormatting.DARK_AQUA)
            );
        }

        if (!isFresh(stack)) {
            event.getToolTip().add(
                Component.translatable("tooltip.zen_atelier.synthesis_output.spent")
                    .withStyle(ChatFormatting.DARK_RED)
            );
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
