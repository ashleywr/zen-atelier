package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class ToolCoatingEvents {
    private static final ResourceLocation SMELTING_MINING_COATING_ID =
            ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "smelting_mining_coating");

    private ToolCoatingEvents() {
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack toolStack = event.getEntity().getMainHandItem();
        ActiveToolCoating coating = toolStack.get(ZenAtelier.ACTIVE_TOOL_COATING.get());
        if (!ToolCoatingApplicator.canBoost(toolStack, event.getState()) || coating == null) {
            return;
        }

        event.setNewSpeed(event.getNewSpeed() * coating.speedMultiplier());
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player) || player.getAbilities().instabuild) {
            return;
        }

        ItemStack toolStack = player.getMainHandItem();
        if (!ToolCoatingApplicator.canBoost(toolStack, event.getState())) {
            return;
        }

        if (ToolCoatingApplicator.hasCoating(toolStack, SMELTING_MINING_COATING_ID)) {
            smeltDrops(event.getLevel(), event);
        }
        ToolCoatingApplicator.consumeCharge(toolStack);
    }

    private static void smeltDrops(ServerLevel level, BlockDropsEvent event) {
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            ItemStack smelted = smelt(level, stack);
            if (!smelted.isEmpty()) {
                drop.setItem(smelted);
            }
        }
    }

    private static ItemStack smelt(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, input, level)
                .map(holder -> holder.value().assemble(input, level.registryAccess()))
                .filter(result -> !result.isEmpty())
                .map(result -> {
                    ItemStack copy = result.copy();
                    copy.setCount(result.getCount() * stack.getCount());
                    return copy;
                })
                .orElse(ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (event.getSource().getDirectEntity() != player) {
            return;
        }

        ItemStack weaponStack = player.getMainHandItem();
        ActiveToolCoating coating = weaponStack.get(ZenAtelier.ACTIVE_TOOL_COATING.get());
        if (!ToolCoatingApplicator.canBoostWeapon(weaponStack) || coating == null) {
            return;
        }

        event.setNewDamage(event.getNewDamage() * coating.speedMultiplier());
        if (!player.getAbilities().instabuild) {
            ToolCoatingApplicator.consumeCharge(weaponStack);
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ActiveToolCoating coating = event.getItemStack().get(ZenAtelier.ACTIVE_TOOL_COATING.get());
        if (coating == null) {
            return;
        }

        event.getToolTip().add(Component.translatable(
                "tooltip.zen_atelier.active_tool_coating",
                Component.translatable("item." + coating.coatingId().getNamespace() + "." + coating.coatingId().getPath()),
                coating.charges()
        ).withStyle(ChatFormatting.AQUA));
    }
}
