package com.sanhiruzu.atelier.synthesis;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public record SynthesisRecipe(
        ResourceLocation id,
        AlchemyWandTier minimumTier,
        int minimumAtelierQuality,
        List<Item> ingredients,
        Item output,
        int outputCount,
        List<String> modifiers
) {
    private static final Random RANDOM = new Random();
    private static final List<SynthesisRecipe> RECIPES = List.of(
            recipe("healing_salve", AlchemyWandTier.COPPER, 10, ZenAtelier.HEALING_SALVE.get(), 1,
                    List.of("soothing", "vital"),
                    Items.HONEY_BOTTLE, Items.GLOW_BERRIES, Items.AMETHYST_SHARD),
            recipe("flash_bomb", AlchemyWandTier.COPPER, 20, ZenAtelier.FLASH_BOMB.get(), 1,
                    List.of("bright", "wide"),
                    Items.GUNPOWDER, Items.GLOWSTONE_DUST, Items.AMETHYST_SHARD),
            recipe("refined_copper_ingot", AlchemyWandTier.COPPER, 25, ZenAtelier.REFINED_COPPER_INGOT.get(), 1,
                    List.of("conductive", "tempered"),
                    Items.COPPER_INGOT, Items.REDSTONE, Items.AMETHYST_SHARD),
            recipe("refined_iron_ingot", AlchemyWandTier.SILVER, 40, ZenAtelier.REFINED_IRON_INGOT.get(), 1,
                    List.of("reinforced", "tempered"),
                    Items.IRON_INGOT, Items.QUARTZ, Items.AMETHYST_SHARD),
            recipe("refined_gold_ingot", AlchemyWandTier.GOLD, 55, ZenAtelier.REFINED_GOLD_INGOT.get(), 1,
                    List.of("lightweight", "resonant"),
                    Items.GOLD_INGOT, Items.GLOWSTONE_DUST, Items.AMETHYST_SHARD)
    );

    private static SynthesisRecipe recipe(String path, AlchemyWandTier tier, int minimumQuality, Item output, int outputCount,
                                          List<String> modifiers, Item... ingredients) {
        return new SynthesisRecipe(
                ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, path),
                tier,
                minimumQuality,
                List.of(ingredients),
                output,
                outputCount,
                modifiers
        );
    }

    public static Optional<SynthesisRecipe> find(List<ItemStack> ingredients, AlchemyWandTier tier, int atelierQuality) {
        return RECIPES.stream()
                .filter(recipe -> tier.atLeast(recipe.minimumTier()))
                .filter(recipe -> atelierQuality >= recipe.minimumAtelierQuality())
                .filter(recipe -> recipe.matches(ingredients))
                .findFirst();
    }

    public static Optional<SynthesisRecipe> firstBlockedByTierOrQuality(List<ItemStack> ingredients) {
        return RECIPES.stream()
                .filter(recipe -> recipe.matches(ingredients))
                .findFirst();
    }

    public ItemStack assemble(AlchemyWandTier tier, int atelierQuality) {
        ItemStack stack = new ItemStack(output, outputCount);
        int quality = Math.clamp(25 + tier.qualityBonus() + RANDOM.nextInt(18), 1, Math.max(1, atelierQuality));
        String modifier = modifiers.get(RANDOM.nextInt(modifiers.size()));
        SynthesisResultComponents.apply(stack, modifier, quality);
        return stack;
    }

    private boolean matches(List<ItemStack> candidateStacks) {
        if (candidateStacks.size() != ingredients.size()) {
            return false;
        }

        List<Item> remaining = new ArrayList<>(ingredients);
        for (ItemStack stack : candidateStacks) {
            if (stack.isEmpty()) {
                return false;
            }
            Item item = stack.getItem();
            if (!remaining.remove(item)) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    public String displayOutputId() {
        return BuiltInRegistries.ITEM.getKey(output).toString();
    }
}
