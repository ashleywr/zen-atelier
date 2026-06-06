package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public final class SynthesisOutputItemFactory {
    private SynthesisOutputItemFactory() {
    }

    public static ItemStack createStack(SynthesisOutput output) {
        ResourceLocation outputId = ResourceLocation.parse(output.outputId());
        Item item = BuiltInRegistries.ITEM.getOptional(outputId).orElse(Items.PAPER);
        ItemStack stack = new ItemStack(item, output.count());
        if (item == Items.PAPER) {
            stack.set(DataComponents.CUSTOM_NAME, outputName(output.outputId()).copy().withStyle(ChatFormatting.AQUA));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable("tooltip.zen_atelier.synthesis.output", outputName(output.outputId())));
        lore.add(Component.translatable("tooltip.zen_atelier.synthesis.stats", output.tier(), output.quality()));
        if (!output.affixes().isEmpty()) {
            lore.add(Component.translatable("tooltip.zen_atelier.synthesis.traits", formatAffixes(output.affixes())));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    public static ItemStack previewStack(SynthesisProfile profile) {
        return profile.primaryOutput()
                .map(output -> createStack(new SynthesisOutput(output.outputId(), 1, output.tier(), output.quality(), output.affixes())))
                .orElseGet(() -> new ItemStack(Items.PAPER));
    }

    private static Component outputName(String id) {
        return Component.translatable("item." + namespace(id) + "." + path(id));
    }

    private static Component formatAffixes(List<String> affixes) {
        MutableComponent result = Component.empty();
        for (int i = 0; i < affixes.size(); i++) {
            if (i > 0) {
                result.append(Component.literal(", "));
            }
            result.append(Component.translatable("zen_atelier.affix." + path(affixes.get(i))));
        }
        return result;
    }

    private static String namespace(String id) {
        return id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
    }

    private static String path(String id) {
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }
}
