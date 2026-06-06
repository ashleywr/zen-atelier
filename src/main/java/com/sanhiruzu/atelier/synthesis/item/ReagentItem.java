package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class ReagentItem extends Item {
    public ReagentItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createStack(ReagentStack reagent) {
        ItemStack stack = new ItemStack(ZenAtelier.REAGENT.get());
        stack.set(ZenAtelier.REAGENT_STACK.get(), reagent);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(reagentNameKey(reagent.reagentId())));
        stack.set(DataComponents.RARITY, rarityForTier(reagent.tier()));
        return stack;
    }

    public static ReagentStack getReagent(ItemStack stack) {
        return stack.get(ZenAtelier.REAGENT_STACK.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
        ReagentStack reagent = getReagent(stack);
        if (reagent == null) {
            tooltip.add(Component.translatable("tooltip.zen_atelier.reagent.invalid").withStyle(ChatFormatting.DARK_RED));
            return;
        }

        tooltip.add(Component.translatable("tooltip.zen_atelier.reagent.amount", reagent.amount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.zen_atelier.reagent.stats",
                reagent.tier(),
                reagent.quality(),
                reagent.purity(),
                reagent.instability()
        ).withStyle(ChatFormatting.GRAY));
        if (!reagent.elements().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.zen_atelier.reagent.elements", formatElements(reagent)).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (!reagent.categories().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.zen_atelier.reagent.categories", formatIds(reagent.categories().stream().sorted().toList()))
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable(
                "tooltip.zen_atelier.reagent.shape",
                titleCase(reagent.shape().id()),
                reagent.shape().size()
        ).withStyle(ChatFormatting.BLUE));
        if (!reagent.traits().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.zen_atelier.reagent.traits", formatIds(reagent.traits())).withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (!reagent.sourceHints().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.zen_atelier.reagent.source", formatIds(reagent.sourceHints().stream().sorted().toList()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String reagentNameKey(String id) {
        return "zen_atelier.reagent." + path(id);
    }

    private static Rarity rarityForTier(int tier) {
        if (tier >= 5) {
            return Rarity.EPIC;
        }
        if (tier >= 3) {
            return Rarity.RARE;
        }
        if (tier >= 2) {
            return Rarity.UNCOMMON;
        }
        return Rarity.COMMON;
    }

    private static String formatElements(ReagentStack reagent) {
        return reagent.elements().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String formatIds(List<String> ids) {
        return ids.stream()
                .map(ReagentItem::path)
                .map(ReagentItem::titleCase)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String path(String id) {
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }

    private static String titleCase(String idPath) {
        String[] parts = idPath.replace('_', ' ').split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isBlank()) {
                parts[i] = parts[i].substring(0, 1).toUpperCase(Locale.ROOT) + parts[i].substring(1);
            }
        }
        return String.join(" ", parts);
    }
}
