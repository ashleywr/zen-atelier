package com.sanhiruzu.atelier.integration.jei;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionOutcome;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionProfile;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record JeiExtractionRecipe(
        ResourceLocation id,
        Ingredient source,
        ItemStack station,
        ItemStack output,
        List<ItemStack> reagentOutputs,
        boolean priming
) {
    static JeiExtractionRecipe createPriming() {
        return new JeiExtractionRecipe(
                ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "prime_extraction_cauldron"),
                Ingredient.of(ZenAtelier.ALCHEMIST_PRIMER.get(), ZenAtelier.CRUCIBLE_SPOON.get()),
                new ItemStack(Items.WATER_BUCKET),
                new ItemStack(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get()),
                List.of(),
                true
        );
    }

    static JeiExtractionRecipe fromProfile(ExtractionProfile profile) {
        List<ItemStack> outputs = distinctReagents(profile.outcomes().stream()
                .flatMap(outcome -> reagents(outcome).stream())
                .toList());
        return new JeiExtractionRecipe(
                ResourceLocation.parse(profile.id()),
                ingredientForSource(profile.sourceKey()),
                new ItemStack(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get()),
                outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst(),
                outputs,
                false
        );
    }

    private static List<ReagentStack> reagents(ExtractionOutcome outcome) {
        List<ReagentStack> reagents = new ArrayList<>();
        reagents.addAll(outcome.reagents());
        reagents.addAll(outcome.byproducts());
        return reagents;
    }

    private static List<ItemStack> distinctReagents(List<ReagentStack> reagents) {
        Map<String, ReagentStack> distinct = new LinkedHashMap<>();
        for (ReagentStack reagent : reagents) {
            distinct.putIfAbsent(reagent.reagentId(), reagent);
        }
        return distinct.values().stream()
                .map(ReagentItem::createStack)
                .toList();
    }

    private static Ingredient ingredientForSource(String sourceKey) {
        if (sourceKey.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.parse(sourceKey.substring(1));
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
            return Ingredient.of(tag);
        }
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(sourceKey))
                .map(Ingredient::of)
                .orElse(Ingredient.EMPTY);
    }
}
