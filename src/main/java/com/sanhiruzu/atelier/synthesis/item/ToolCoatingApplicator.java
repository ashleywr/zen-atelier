package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ToolCoatingApplicator {
    private ToolCoatingApplicator() {
    }

    public static boolean isCoating(ItemStack stack) {
        return stack.getItem() instanceof ToolCoatingItem;
    }

    public static boolean canApplyTo(ItemStack toolStack) {
        return !toolStack.isEmpty()
                && (toolStack.is(ItemTags.PICKAXES) || toolStack.is(ItemTags.SWORDS) || toolStack.is(ItemTags.AXES))
                && !toolStack.has(ZenAtelier.ACTIVE_TOOL_COATING.get());
    }

    public static boolean canApplyTo(ItemStack toolStack, ToolCoatingItem coatingItem) {
        return canApplyTo(toolStack) && coatingItem.canApplyTo(toolStack);
    }

    public static ItemStack applyToCopy(ItemStack toolStack, ToolCoatingItem coatingItem) {
        ItemStack result = toolStack.copyWithCount(1);
        result.set(ZenAtelier.ACTIVE_TOOL_COATING.get(), coatingItem.createActiveCoating());
        return result;
    }

    public static ApplyResult applyInPlace(ItemStack toolStack, ItemStack coatingStack, Player player) {
        if (!(coatingStack.getItem() instanceof ToolCoatingItem coatingItem)) {
            return ApplyResult.rejected(null);
        }

        if (toolStack.isEmpty() || !coatingItem.canApplyTo(toolStack)) {
            return ApplyResult.rejected(coatingItem.requiresTargetMessage());
        }

        if (toolStack.has(ZenAtelier.ACTIVE_TOOL_COATING.get())) {
            return ApplyResult.rejected(Component.translatable("message.zen_atelier.tool_coating.already_coated"));
        }

        if (!player.level().isClientSide) {
            toolStack.set(ZenAtelier.ACTIVE_TOOL_COATING.get(), coatingItem.createActiveCoating());
            if (!player.getAbilities().instabuild) {
                coatingStack.shrink(1);
            }
        }
        return ApplyResult.applied(Component.translatable("message.zen_atelier.tool_coating.applied").withStyle(ChatFormatting.AQUA));
    }

    public static boolean isEligibleBlock(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    public static boolean canBoost(ItemStack toolStack, BlockState state) {
        return toolStack.is(ItemTags.PICKAXES)
                && toolStack.has(ZenAtelier.ACTIVE_TOOL_COATING.get())
                && isEligibleBlock(state);
    }

    public static boolean canBoostWeapon(ItemStack toolStack) {
        return (toolStack.is(ItemTags.SWORDS) || toolStack.is(ItemTags.AXES))
                && toolStack.has(ZenAtelier.ACTIVE_TOOL_COATING.get());
    }

    public static boolean hasCoating(ItemStack toolStack, ResourceLocation coatingId) {
        ActiveToolCoating active = toolStack.get(ZenAtelier.ACTIVE_TOOL_COATING.get());
        return active != null && active.coatingId().equals(coatingId);
    }

    public static void consumeCharge(ItemStack toolStack) {
        ActiveToolCoating active = toolStack.get(ZenAtelier.ACTIVE_TOOL_COATING.get());
        if (active == null) {
            return;
        }

        if (active.charges() <= 1) {
            toolStack.remove(ZenAtelier.ACTIVE_TOOL_COATING.get());
            return;
        }
        toolStack.set(ZenAtelier.ACTIVE_TOOL_COATING.get(), active.consumeCharge());
    }

    public record ApplyResult(boolean applied, Component message) {
        static ApplyResult applied(Component message) {
            return new ApplyResult(true, message);
        }

        static ApplyResult rejected(Component message) {
            return new ApplyResult(false, message);
        }
    }
}
