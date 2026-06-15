package com.sanhiruzu.atelier.synthesis.gathering;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainerSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GatheringBasketItem extends Item {
    private static final int TOOLTIP_ENTRY_LIMIT = 5;

    public GatheringBasketItem(Properties properties) {
        super(properties);
    }

    public static boolean isBasket(ItemStack stack) {
        return stack.is(ZenAtelier.GATHERING_BASKET.get());
    }

    public static ReagentContainer getContents(ItemStack stack) {
        ReagentContainerSnapshot snapshot = stack.get(ZenAtelier.BASKET_REAGENTS.get());
        return snapshot == null ? new ReagentContainer() : snapshot.toContainer();
    }

    public static void setContents(ItemStack stack, ReagentContainer container) {
        stack.set(ZenAtelier.BASKET_REAGENTS.get(), ReagentContainerSnapshot.fromContainer(container));
    }

    public static void insert(ItemStack stack, ReagentStack reagent) {
        ReagentContainer container = getContents(stack);
        container.insert(reagent);
        setContents(stack, container);
    }

    public static List<ReagentStack> entries(ItemStack stack) {
        return getContents(stack).entries();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
        List<ReagentStack> entries = entries(stack);
        int units = entries.stream().mapToInt(ReagentStack::amount).sum();
        tooltip.add(Component.translatable("tooltip.zen_atelier.gathering_basket.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.zen_atelier.gathering_basket.summary", entries.size(), units)
                .withStyle(ChatFormatting.GOLD));

        if (entries.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.zen_atelier.gathering_basket.empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        List<ReagentStack> sorted = new ArrayList<>(entries);
        sorted.sort(java.util.Comparator
                .comparing(ReagentStack::reagentId)
                .thenComparing(java.util.Comparator.comparingInt(ReagentStack::tier).reversed())
                .thenComparing(java.util.Comparator.comparingInt(ReagentStack::quality).reversed()));
        for (ReagentStack reagent : sorted.stream().limit(TOOLTIP_ENTRY_LIMIT).toList()) {
            tooltip.add(Component.literal(" - ")
                    .append(Component.translatable(reagentNameKey(reagent.reagentId())))
                    .append(Component.literal(" x" + reagent.amount() + " Q" + reagent.quality()))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        if (entries.size() > TOOLTIP_ENTRY_LIMIT) {
            tooltip.add(Component.translatable("tooltip.zen_atelier.gathering_basket.more", entries.size() - TOOLTIP_ENTRY_LIMIT)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    static Component gatheredMessage(ReagentStack reagent) {
        return Component.translatable(
                "message.zen_atelier.gathering.got",
                Component.translatable(reagentNameKey(reagent.reagentId())),
                reagent.amount(),
                reagent.quality()
        );
    }

    private static String reagentNameKey(String id) {
        return "zen_atelier.reagent." + path(id);
    }

    private static String path(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return path.toLowerCase(Locale.ROOT);
    }
}
