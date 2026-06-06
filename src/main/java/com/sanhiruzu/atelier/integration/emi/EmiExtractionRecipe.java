package com.sanhiruzu.atelier.integration.emi;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionOutcome;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionProfile;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EmiExtractionRecipe extends BasicEmiRecipe {
    private final boolean priming;

    private EmiExtractionRecipe(ResourceLocation id, boolean priming) {
        super(AtelierEmiPlugin.EXTRACTION, id, 154, 72);
        this.priming = priming;
    }

    static EmiExtractionRecipe priming() {
        EmiExtractionRecipe recipe = new EmiExtractionRecipe(
                EmiRecipeIds.synthetic(ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "prime_extraction_cauldron")),
                true
        );
        recipe.inputs.add(EmiIngredient.of(Ingredient.of(ZenAtelier.ALCHEMIST_PRIMER.get(), ZenAtelier.CRUCIBLE_SPOON.get())));
        recipe.catalysts.add(EmiStack.of(Items.CAULDRON));
        recipe.catalysts.add(EmiStack.of(Items.CAMPFIRE));
        recipe.outputs.add(EmiStack.of(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get()));
        return recipe;
    }

    static EmiExtractionRecipe fromProfile(ExtractionProfile profile) {
        EmiExtractionRecipe recipe = new EmiExtractionRecipe(EmiRecipeIds.synthetic(ResourceLocation.parse(profile.id())), false);
        recipe.inputs.add(ingredientForSource(profile.sourceKey()));
        recipe.catalysts.add(EmiStack.of(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get()));
        recipe.outputs.addAll(distinctReagents(profile.outcomes().stream()
                .flatMap(outcome -> reagents(outcome).stream())
                .toList()));
        return recipe;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addText(priming
                        ? Component.translatable("jei.zen_atelier.extraction.priming")
                        : Component.translatable("jei.zen_atelier.extraction.heated"),
                6, 4, 0xFF6B5A42, false);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 66, 28);

        if (priming) {
            widgets.addSlot(EmiStack.of(Items.CAULDRON), 8, 28).catalyst(true);
            widgets.addSlot(inputs.getFirst(), 32, 28);
            widgets.addSlot(EmiStack.of(Items.CAMPFIRE), 56, 28).catalyst(true);
            widgets.addSlot(outputs.getFirst(), 120, 28).recipeContext(this);
            widgets.addText(Component.translatable("jei.zen_atelier.extraction.water_heat"), 54, 52, 0xFF806F56, false);
            return;
        }

        widgets.addSlot(catalysts.getFirst(), 8, 28).catalyst(true);
        widgets.addSlot(inputs.getFirst(), 32, 28);
        int visible = Math.min(outputs.size(), 4);
        for (int i = 0; i < visible; i++) {
            widgets.addSlot(outputs.get(i), 96 + (i % 2) * 24, 16 + (i / 2) * 24).recipeContext(this);
        }
    }

    private static List<ReagentStack> reagents(ExtractionOutcome outcome) {
        List<ReagentStack> reagents = new ArrayList<>();
        reagents.addAll(outcome.reagents());
        reagents.addAll(outcome.byproducts());
        return reagents;
    }

    private static List<EmiStack> distinctReagents(List<ReagentStack> reagents) {
        Map<String, ReagentStack> distinct = new LinkedHashMap<>();
        for (ReagentStack reagent : reagents) {
            distinct.putIfAbsent(reagent.reagentId(), reagent);
        }
        return distinct.values().stream()
                .map(ReagentItem::createStack)
                .map(EmiStack::of)
                .toList();
    }

    private static EmiIngredient ingredientForSource(String sourceKey) {
        if (sourceKey.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.parse(sourceKey.substring(1));
            return EmiIngredient.of(TagKey.create(Registries.ITEM, tagId));
        }
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(sourceKey))
                .map(Item::getDefaultInstance)
                .map(Ingredient::of)
                .map(EmiIngredient::of)
                .orElse(EmiStack.EMPTY);
    }
}
