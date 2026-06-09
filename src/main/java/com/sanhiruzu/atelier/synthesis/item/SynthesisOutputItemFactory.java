package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SynthesisOutputItemFactory {
    private SynthesisOutputItemFactory() {
    }

    public static ItemStack createStack(SynthesisOutput output) {
        ResourceLocation outputId = ResourceLocation.parse(output.outputId());
        Item item = BuiltInRegistries.ITEM.getOptional(outputId).orElse(Items.PAPER);
        // Always produce one item; count becomes max uses tracked as durability.
        ItemStack stack = new ItemStack(item, 1);
        if (item == Items.PAPER) {
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable(
                    "item." + namespace(output.outputId()) + "." + path(output.outputId())
            ).copy().withStyle(ChatFormatting.AQUA));
        }
        stack.set(DataComponents.MAX_DAMAGE, output.count());
        stack.set(ZenAtelier.SYNTHESIS_OUTPUT_DATA.get(),
                new SynthesisOutputData(output.tier(), output.quality(), output.affixes()));
        return stack;
    }

    public static ItemStack previewStack(SynthesisProfile profile) {
        return profile.primaryOutput()
                .map(SynthesisOutputItemFactory::createStack)
                .orElseGet(() -> new ItemStack(Items.PAPER));
    }

    private static String namespace(String id) {
        return id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
    }

    private static String path(String id) {
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }
}
