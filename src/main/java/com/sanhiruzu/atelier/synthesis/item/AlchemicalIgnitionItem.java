package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.synthesis.world.CauldronExtractionService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class AlchemicalIgnitionItem extends Item {
    private final boolean requiresHeat;
    private final boolean consumedOnUse;
    private final String tooltipKey;

    public AlchemicalIgnitionItem(Properties properties, boolean requiresHeat, boolean consumedOnUse, String tooltipKey) {
        super(properties);
        this.requiresHeat = requiresHeat;
        this.consumedOnUse = consumedOnUse;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        boolean primed = CauldronExtractionService.tryPrimeFromTool(level, context.getClickedPos(), requiresHeat);
        if (!primed) {
            Player player = context.getPlayer();
            if (player != null) {
                player.displayClientMessage(Component.translatable(failureMessageKey()), true);
            }
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (consumedOnUse && player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.zen_atelier.extraction.primed"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
    }

    private String failureMessageKey() {
        return requiresHeat
                ? "message.zen_atelier.extraction.spoon_requires_heated_water"
                : "message.zen_atelier.extraction.primer_requires_cauldron";
    }
}
