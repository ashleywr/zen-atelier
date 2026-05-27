package com.sanhiruzu.atelier.synthesis;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class SynthesisResultComponents {
    private SynthesisResultComponents() {
    }

    public static void apply(ItemStack stack, String modifier, int quality) {
        stack.set(ZenAtelier.SYNTHESIS_MODIFIER.get(), modifier);
        stack.set(ZenAtelier.SYNTHESIS_QUALITY.get(), quality);
    }

    public static String modifier(ItemStack stack) {
        return stack.getOrDefault(ZenAtelier.SYNTHESIS_MODIFIER.get(), "");
    }

    public static int quality(ItemStack stack) {
        return stack.getOrDefault(ZenAtelier.SYNTHESIS_QUALITY.get(), 0);
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        String modifier = modifier(stack);
        int quality = quality(stack);
        if (!modifier.isBlank()) {
            tooltip.add(Component.translatable("tooltip.zen_atelier.synthesis_modifier", Component.translatable("modifier.zen_atelier." + modifier)));
        }
        if (quality > 0) {
            tooltip.add(Component.translatable("tooltip.zen_atelier.synthesis_quality", quality));
        }
    }
}
