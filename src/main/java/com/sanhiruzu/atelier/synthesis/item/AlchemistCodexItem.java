package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.synthesis.world.PlayerExtractionKnowledge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class AlchemistCodexItem extends Item {
    private static final int SUMMARY_LIMIT = 8;

    public AlchemistCodexItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            openClientScreen();
            return InteractionResultHolder.consume(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.zen_atelier.codex.header")
                    .withStyle(ChatFormatting.DARK_AQUA));
            for (Component line : PlayerExtractionKnowledge.codexSummary(serverPlayer, SUMMARY_LIMIT)) {
                serverPlayer.sendSystemMessage(line);
            }
            if (PlayerExtractionKnowledge.knownSourceCount(serverPlayer) > SUMMARY_LIMIT) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.zen_atelier.codex.more",
                        PlayerExtractionKnowledge.knownSourceCount(serverPlayer) - SUMMARY_LIMIT
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
        tooltip.add(Component.translatable("tooltip.zen_atelier.alchemist_codex").withStyle(ChatFormatting.GRAY));
    }

    private static void openClientScreen() {
        try {
            Class<?> opener = Class.forName("com.sanhiruzu.atelier.ui.client.AlchemistCodexClientOpener");
            opener.getMethod("open").invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
