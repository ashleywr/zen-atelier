package com.sanhiruzu.atelier.space.zone;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class QualityEvaluator {
    // Default weights if no room-type profile is provided
    private static final float SIZE_MAX = 0.30f;
    private static final float ENCLOSURE_MAX = 0.35f;
    private static final float FURNITURE_MAX = 0.20f;
    private static final float THEME_MAX = 0.15f;

    public static class QualityBreakdown {
        public float sizeScore;
        public float enclosureScore;
        public float furnitureScore;
        public float themeScore;
        public float totalQuality;

        // For HUD display
        public Map<String, Float> components = new HashMap<>();

        public QualityBreakdown() {
            this.sizeScore = 0;
            this.enclosureScore = 0;
            this.furnitureScore = 0;
            this.themeScore = 0;
            this.totalQuality = 0;
        }
    }

    public static QualityBreakdown evaluate(int volume, float enclosureScore, Map<String, Integer> furnitureCounts, Map<String, Integer> signalCounts) {
        return evaluate(volume, enclosureScore, furnitureCounts, signalCounts, null);
    }

    public static QualityBreakdown evaluate(int volume, float enclosureScore, Map<String, Integer> furnitureCounts, Map<String, Integer> signalCounts, @Nullable RoomTypeQualityProfile roomProfile) {
        QualityBreakdown breakdown = new QualityBreakdown();

        // Get weights from room profile or use defaults
        float sizeWeight = roomProfile != null ? roomProfile.getSizeWeight() : SIZE_MAX;
        float enclosureWeight = roomProfile != null ? roomProfile.getEnclosureWeight() : ENCLOSURE_MAX;
        float furnitureWeight = roomProfile != null ? roomProfile.getFurnitureWeight() : FURNITURE_MAX;
        float themeWeight = roomProfile != null ? roomProfile.getThemeWeight() : THEME_MAX;

        // Size component: volume matters, but with diminishing returns
        breakdown.sizeScore = evaluateSize(volume) * sizeWeight;

        // Enclosure component: how sealed is it?
        breakdown.enclosureScore = Math.max(0, enclosureScore) * enclosureWeight;

        // Furniture component: diversity + rarity of furniture types
        float furnitureQuality = evaluateFurniture(furnitureCounts);
        breakdown.furnitureScore = Math.min(1.0f, furnitureQuality) * furnitureWeight;

        // Theme component: room-type-specific aesthetics and setup
        if (roomProfile != null) {
            breakdown.themeScore = evaluateTheme(furnitureCounts, signalCounts, roomProfile) * themeWeight;
        } else {
            // Fall back to generic crafting setup evaluation
            breakdown.themeScore = evaluateCraftingSetup(signalCounts) * themeWeight;
        }

        // Total is sum of all components (max 1.0)
        breakdown.totalQuality = Math.min(1.0f, breakdown.sizeScore + breakdown.enclosureScore + breakdown.furnitureScore + breakdown.themeScore);

        // Populate components map for HUD
        breakdown.components.put("Size", breakdown.sizeScore);
        breakdown.components.put("Enclosure", breakdown.enclosureScore);
        breakdown.components.put("Furniture", breakdown.furnitureScore);
        breakdown.components.put("Theme", breakdown.themeScore);

        return breakdown;
    }

    private static float evaluateTheme(Map<String, Integer> furnitureCounts, Map<String, Integer> signalCounts, RoomTypeQualityProfile profile) {
        float themeScore = 0;
        int matchCount = 0;

        // Check furniture against theme factors
        for (Map.Entry<String, Integer> furniture : furnitureCounts.entrySet()) {
            String key = furniture.getKey().toLowerCase();
            for (Map.Entry<String, Float> themeFactor : profile.getThemeFactors().entrySet()) {
                if (key.contains(themeFactor.getKey())) {
                    themeScore += Math.min(1.0f, themeFactor.getValue() * 0.2f);
                    matchCount++;
                    break;
                }
            }
        }

        // Also check signals
        for (String signal : signalCounts.keySet()) {
            String key = signal.toLowerCase();
            for (Map.Entry<String, Float> themeFactor : profile.getThemeFactors().entrySet()) {
                if (key.contains(themeFactor.getKey())) {
                    themeScore += Math.min(1.0f, themeFactor.getValue() * 0.15f);
                    matchCount++;
                    break;
                }
            }
        }

        return Math.min(1.0f, themeScore);
    }

    private static float evaluateSize(int volume) {
        if (volume < 30) return 0.1f;
        if (volume < 50) return 0.2f;
        if (volume < 100) return 0.5f;
        if (volume < 200) return 0.8f;
        return 1.0f; // 200+ blocks is max
    }

    private static float evaluateCraftingSetup(Map<String, Integer> signals) {
        if (signals.isEmpty()) return 0.0f;

        int craftingSignalCount = 0;
        for (String signal : signals.keySet()) {
            String lower = signal.toLowerCase();
            if (isCraftingSignal(lower)) {
                craftingSignalCount += signals.get(signal);
            }
        }

        // 1-2 signals = 0.3, 3+ = 0.6, 5+ = 1.0
        if (craftingSignalCount >= 5) return 1.0f;
        if (craftingSignalCount >= 3) return 0.6f;
        if (craftingSignalCount >= 1) return 0.3f;
        return 0.0f;
    }

    private static boolean isCraftingSignal(String signal) {
        return signal.contains("brewing") || signal.contains("enchanting") || signal.contains("furnace")
                || signal.contains("cauldron") || signal.contains("loom") || signal.contains("stonecutter")
                || signal.contains("cartography") || signal.contains("smithing") || signal.contains("crafting");
    }

    private static float evaluateFurniture(Map<String, Integer> furnitureCounts) {
        if (furnitureCounts.isEmpty()) return 0.0f;

        // Average rarity of all furniture types, weighted by count
        float totalRarity = 0;
        int totalBlocks = 0;

        for (Map.Entry<String, Integer> entry : furnitureCounts.entrySet()) {
            String blockKey = entry.getKey();
            int count = entry.getValue();

            // Parse block registry name and look up rarity
            float blockRarity = 0.5f; // Default COMMON
            try {
                ResourceLocation rl = ResourceLocation.parse(blockKey);
                Block block = BuiltInRegistries.BLOCK.get(rl);
                if (block != null) {
                    blockRarity = BlockRarityCache.getRarity(block);
                }
            } catch (Exception e) {
                // Silently use default if parsing fails
            }

            totalRarity += blockRarity * count;
            totalBlocks += count;
        }

        // Normalize to [0, 1] range
        float avgRarity = totalBlocks > 0 ? totalRarity / totalBlocks : 0.5f;

        // Diversity bonus: more distinct types = higher score
        int distinctTypes = furnitureCounts.size();
        float diversityBonus = Math.min(1.0f, distinctTypes / 5.0f);

        // Combine rarity (normalized from 0.5-2.0 to 0-1) with diversity
        float rarityNormalized = (avgRarity - 0.5f) / 1.5f; // Maps [0.5, 2.0] to [0, 1]
        return (rarityNormalized * 0.6f + diversityBonus * 0.4f);
    }
}
