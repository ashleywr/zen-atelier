package com.sanhiruzu.atelier.synthesis.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ToolCoatingItem extends Item {
    private final ResourceLocation coatingId;
    private final Target target;
    private final int charges;
    private final float multiplier;

    public ToolCoatingItem(Properties properties, ResourceLocation coatingId, int charges, float multiplier) {
        this(properties, coatingId, Target.MINING, charges, multiplier);
    }

    public ToolCoatingItem(Properties properties, ResourceLocation coatingId, Target target, int charges, float multiplier) {
        super(properties);
        this.coatingId = coatingId;
        this.target = target;
        this.charges = charges;
        this.multiplier = multiplier;
    }

    public ActiveToolCoating createActiveCoating() {
        return new ActiveToolCoating(coatingId, charges, multiplier);
    }

    boolean canApplyTo(ItemStack stack) {
        return target.canApplyTo(stack);
    }

    Component requiresTargetMessage() {
        return Component.translatable(target.requiresMessageKey);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack coatingStack = player.getItemInHand(hand);
        ItemStack toolStack = player.getOffhandItem();
        if (hand == InteractionHand.OFF_HAND) {
            toolStack = player.getMainHandItem();
        }

        ToolCoatingApplicator.ApplyResult result = ToolCoatingApplicator.applyInPlace(toolStack, coatingStack, player);
        if (!level.isClientSide && result.message() != null) {
            player.displayClientMessage(result.message(), true);
        }

        if (result.applied()) {
            return InteractionResultHolder.sidedSuccess(coatingStack, level.isClientSide);
        }
        return InteractionResultHolder.fail(coatingStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(target.appliesToKey).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                target.multiplierKey,
                Math.round((multiplier - 1.0f) * 100.0f),
                charges
        ).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.zen_atelier.tool_coating.use").withStyle(ChatFormatting.DARK_GRAY));
    }

    public enum Target {
        MINING(
                "tooltip.zen_atelier.tool_coating.applies_to_pickaxes",
                "tooltip.zen_atelier.tool_coating.mining_speed",
                "message.zen_atelier.tool_coating.requires_pickaxe"
        ) {
            @Override
            boolean canApplyTo(ItemStack stack) {
                return stack.is(ItemTags.PICKAXES);
            }
        },
        WEAPON(
                "tooltip.zen_atelier.tool_coating.applies_to_weapons",
                "tooltip.zen_atelier.tool_coating.melee_damage",
                "message.zen_atelier.tool_coating.requires_weapon"
        ) {
            @Override
            boolean canApplyTo(ItemStack stack) {
                return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
            }
        };

        private final String appliesToKey;
        private final String multiplierKey;
        private final String requiresMessageKey;

        Target(String appliesToKey, String multiplierKey, String requiresMessageKey) {
            this.appliesToKey = appliesToKey;
            this.multiplierKey = multiplierKey;
            this.requiresMessageKey = requiresMessageKey;
        }

        abstract boolean canApplyTo(ItemStack stack);
    }
}
