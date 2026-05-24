package com.sanhiruzu.atelier.space.zone;

import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

import java.util.*;

public class BlockRarityCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Block, Float> rarityCache = new HashMap<>();
    private static final Set<Block> computing = new HashSet<>();

    private static boolean initialized = false;

    public static void initialize(RegistryAccess registries, RecipeManager recipes) {
        if (initialized) return;

        LOGGER.info("Initializing BlockRarityCache...");
        long startTime = System.currentTimeMillis();

        BuiltInRegistries.BLOCK.forEach(block -> {
            computeRarity(block, recipes);
        });

        initialized = true;
        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("BlockRarityCache initialized with {} blocks in {}ms", rarityCache.size(), elapsed);
    }

    private static float computeRarity(Block block, RecipeManager recipes) {
        if (rarityCache.containsKey(block)) {
            return rarityCache.get(block);
        }

        // Cycle detection
        if (computing.contains(block)) {
            return 0.5f; // Default to COMMON if we hit a cycle
        }
        computing.add(block);

        float rarity;

        // Look for crafting recipe
        float craftRarity = inferFromRecipe(block, recipes);
        if (craftRarity >= 0) {
            rarity = craftRarity;
        } else {
            // Fallback: infer from block properties (End, Nether, Deep Dark, hardness)
            rarity = inferNaturalRarity(block);
        }

        rarityCache.put(block, rarity);
        computing.remove(block);
        return rarity;
    }

    private static float inferFromRecipe(Block block, RecipeManager recipes) {
        var craftingRecipes = recipes.getAllRecipesFor(RecipeType.CRAFTING);

        for (RecipeHolder<?> recipeHolder : craftingRecipes) {
            ItemStack result = recipeHolder.value().getResultItem(null);
            if (result.isEmpty() || !(result.getItem() instanceof BlockItem blockItem) || blockItem.getBlock() != block) {
                continue;
            }

            // Weighted average of ingredient rarities
            List<Float> ingredientRarities = new ArrayList<>();
            for (Ingredient ingredient : recipeHolder.value().getIngredients()) {
                if (ingredient.isEmpty()) continue;

                float ingredientRarity = getIngredientRarity(ingredient, recipes);
                ingredientRarities.add(ingredientRarity);
            }

            if (ingredientRarities.isEmpty()) {
                return -1; // Can't infer
            }

            // Average the ingredient rarities
            float avg = (float) ingredientRarities.stream()
                    .mapToDouble(Float::doubleValue)
                    .average()
                    .orElse(0.5f);

            return Math.min(2.0f, avg); // Cap at EPIC
        }

        return -1; // No recipe found
    }

    private static float getIngredientRarity(Ingredient ingredient, RecipeManager recipes) {
        for (ItemStack stack : ingredient.getItems()) {
            if (stack.isEmpty()) continue;

            // If it's a block item, recurse
            if (stack.getItem() instanceof BlockItem blockItem) {
                return computeRarity(blockItem.getBlock(), recipes);
            }

            // For regular items, estimate based on crafting cost
            // We could expand this to check more heuristics
        }

        return 0.5f; // Default to COMMON
    }

    private static float inferNaturalRarity(Block block) {
        // Only called if no recipe found (natural blocks)

        // End blocks are EPIC
        if (isEndBlock(block)) {
            return 2.0f;
        }

        // Nether blocks are RARE
        if (isNetherBlock(block)) {
            return 1.5f;
        }

        // Deep Dark blocks are between UNCOMMON and RARE
        if (isDeepDarkBlock(block)) {
            return 1.3f;
        }

        // Very hard to mine (obsidian-like) is RARE
        if (block.defaultBlockState().getDestroySpeed(null, null) > 50) {
            return 1.2f;
        }

        return 0.5f; // Default COMMON
    }

    private static boolean isEndBlock(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).toString();
        return name.contains("end_") || name.contains("purpur") || name.contains("chorus");
    }

    private static boolean isNetherBlock(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).toString();
        return name.contains("nether_") || name.contains("warped_") || name.contains("crimson_")
                || name.contains("soul_") || block == Blocks.NETHERRACK || block == Blocks.MAGMA_BLOCK;
    }

    private static boolean isDeepDarkBlock(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).toString();
        return name.contains("sculk") || name.contains("deep_dark");
    }

    public static float getRarity(Block block) {
        return rarityCache.getOrDefault(block, 0.5f);
    }

    public static void reset() {
        rarityCache.clear();
        computing.clear();
        initialized = false;
    }
}
