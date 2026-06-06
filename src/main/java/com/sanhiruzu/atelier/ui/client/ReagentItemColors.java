package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.Map;

final class ReagentItemColors {
    private static final int DEFAULT_CONTENTS_COLOR = 0x6FD0D8;
    private static final int DEFAULT_BORDER_COLOR = 0x9AA0A6;
    private static final Map<String, Integer> ELEMENT_COLORS = Map.ofEntries(
            Map.entry("air", 0xCFE8FF),
            Map.entry("binding", 0xB06CD7),
            Map.entry("conductive", 0xF0C84B),
            Map.entry("decay", 0x7A5A3A),
            Map.entry("earth", 0x8E6B3F),
            Map.entry("fire", 0xE4572E),
            Map.entry("life", 0x55C96B),
            Map.entry("light", 0xF4D35E),
            Map.entry("metal", 0xB7B7C8),
            Map.entry("preservation", 0x70D6A3),
            Map.entry("preserving", 0x70D6A3),
            Map.entry("sharp", 0xD9D9D9),
            Map.entry("water", 0x3CA6E8)
    );
    private static final Map<String, Integer> REAGENT_COLORS = Map.ofEntries(
            Map.entry("abrasive_reagent", 0xD9D9D9),
            Map.entry("binding_reagent", 0xB06CD7),
            Map.entry("conductive_reagent", 0xF0C84B),
            Map.entry("decay_reagent", 0x7A5A3A),
            Map.entry("fibrous_reagent", 0xC4B06B),
            Map.entry("luminous_reagent", 0xF4D35E),
            Map.entry("organic_reagent", 0x55C96B),
            Map.entry("preserving_reagent", 0x70D6A3),
            Map.entry("quickening_reagent", 0x76D6FF),
            Map.entry("spark_reagent", 0xE4572E),
            Map.entry("sticky_residue", 0xC68A43),
            Map.entry("stone_dust", 0x8E8A7A)
    );
    private static final Map<String, Integer> ITEM_CONTENTS_COLORS = Map.ofEntries(
            Map.entry("crude_mining_coating", 0xC8C0A8),
            Map.entry("sparking_mining_coating", 0xF0C84B),
            Map.entry("keen_weapon_coating", 0xD9D9D9),
            Map.entry("sparking_weapon_coating", 0xF05A5A)
    );
    private static final int[] TIER_BORDER_COLORS = {
            0x9AA0A6,
            0x45B36B,
            0x3F7EE8,
            0xA35CFF,
            0xF0B84A,
            0xF05A5A
    };

    private ReagentItemColors() {
    }

    static int color(ItemStack stack, int tintIndex) {
        ReagentStack reagent = ReagentItem.getReagent(stack);
        if (reagent == null) {
            return opaque(itemColor(stack, tintIndex));
        }
        if (tintIndex == 1) {
            return opaque(contentsColor(reagent));
        }
        if (tintIndex == 2) {
            return opaque(borderColor(reagent));
        }
        return -1;
    }

    private static int contentsColor(ReagentStack reagent) {
        return reagent.elements().entrySet().stream()
                .max(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> SynthesisNoun.color(entry.getKey(), ELEMENT_COLORS.getOrDefault(entry.getKey(), DEFAULT_CONTENTS_COLOR)))
                .orElseGet(() -> reagentIdColor(reagent));
    }

    private static int reagentIdColor(ReagentStack reagent) {
        ResourceLocation id = ResourceLocation.tryParse(reagent.reagentId());
        if (id == null) {
            return DEFAULT_CONTENTS_COLOR;
        }
        return SynthesisNoun.color(id.getPath(), REAGENT_COLORS.getOrDefault(id.getPath(), DEFAULT_CONTENTS_COLOR));
    }

    private static int borderColor(ReagentStack reagent) {
        int tier = Math.max(1, Math.min(TIER_BORDER_COLORS.length, reagent.tier()));
        int baseColor = TIER_BORDER_COLORS[tier - 1];
        return scaleBrightness(baseColor, 0.75F + reagent.quality() * 0.003F);
    }

    private static int scaleBrightness(int color, float multiplier) {
        int red = clampColor(((color >> 16) & 0xFF) * multiplier);
        int green = clampColor(((color >> 8) & 0xFF) * multiplier);
        int blue = clampColor((color & 0xFF) * multiplier);
        return red << 16 | green << 8 | blue;
    }

    private static int clampColor(float channel) {
        return Math.max(0, Math.min(255, Math.round(channel)));
    }

    private static int itemColor(ItemStack stack, int tintIndex) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (tintIndex == 1) {
            return SynthesisNoun.color(itemId.getPath(), ITEM_CONTENTS_COLORS.getOrDefault(itemId.getPath(), DEFAULT_CONTENTS_COLOR));
        }
        if (tintIndex == 2) {
            return DEFAULT_BORDER_COLOR;
        }
        return -1;
    }

    private static int opaque(int color) {
        return color < 0 ? color : 0xFF000000 | color;
    }
}
