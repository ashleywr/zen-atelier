package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.data.RoomProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QualityEvaluator {
    private static final float SIZE_MAX      = 0.30f;
    private static final float ENCLOSURE_MAX = 0.35f;
    private static final float FURNITURE_MAX = 0.20f;
    private static final float THEME_MAX     = 0.15f;

    // Penalty signals reduce the theme score by up to this fraction of the max theme weight.
    private static final float PENALTY_WEIGHT = 0.35f;

    // Maps aspirational signal names used in quality_signals/penalty_signals JSON to the
    // real signal names that ZoneEvaluator actually populates in signalCounts.
    private static final Map<String, String> SIGNAL_ALIASES = new HashMap<>();
    static {
        SIGNAL_ALIASES.put("heat_source",        "cooking_block");
        SIGNAL_ALIASES.put("excess_heat",        "cooking_block");
        SIGNAL_ALIASES.put("utility_heat",       "cooking_block");
        SIGNAL_ALIASES.put("water_access",       "water_coverage");
        SIGNAL_ALIASES.put("bookshelf_count",    "bookshelf");
        SIGNAL_ALIASES.put("bookshelves",        "bookshelf");
        SIGNAL_ALIASES.put("plants",             "plant");
        SIGNAL_ALIASES.put("plant_overgrowth",   "plant");
        SIGNAL_ALIASES.put("botany",             "plant");
        SIGNAL_ALIASES.put("glass_count",        "glass");
        SIGNAL_ALIASES.put("sunlight",           "glass");
        SIGNAL_ALIASES.put("ventilation",        "glass");
        SIGNAL_ALIASES.put("food_storage",       "storage");
        SIGNAL_ALIASES.put("tool_storage",       "storage");
        SIGNAL_ALIASES.put("storage_count",      "storage");
        SIGNAL_ALIASES.put("work_surfaces",      "crafting_table");
        SIGNAL_ALIASES.put("sleep_blocks",       "bed");
        SIGNAL_ALIASES.put("enchanting_blocks",  "enchanting_table");
        SIGNAL_ALIASES.put("stone_materials",    "stone_or_metal_materials");
        SIGNAL_ALIASES.put("quiet_materials",    "stone_or_metal_materials");
        SIGNAL_ALIASES.put("wet_blocks",         "water_coverage");
        // Zone-type quality aliases
        SIGNAL_ALIASES.put("plant_diversity",    "plant");
        SIGNAL_ALIASES.put("hay_coverage",       "hay_bale");
        SIGNAL_ALIASES.put("wine_variety",       "vinery_barrel");
        SIGNAL_ALIASES.put("candles",            "candle");
        SIGNAL_ALIASES.put("terrarium_blocks",   "terrarium_block");
        SIGNAL_ALIASES.put("rotation_blocks",    "create_rotational");
        SIGNAL_ALIASES.put("arcane_focus",       "spectrum_crystal");
    }

    // --- Public API ---

    /** Initial-pass evaluation with no room type context (used before profile matching). */
    public static QualityBreakdown evaluate(int volume, float enclosureScore,
            Map<String, Integer> furnitureCounts, Map<String, Integer> signalCounts) {
        return evaluate(volume, enclosureScore, furnitureCounts, signalCounts, -1, null);
    }

    /**
     * Profile-aware evaluation. Called after the room type is matched so that
     * quality_signals and penalty_signals from the JSON profile drive the theme
     * component of the score.
     */
    public static QualityBreakdown evaluate(int volume, float enclosureScore,
            Map<String, Integer> furnitureCounts, Map<String, Integer> signalCounts,
            int lightLevel, @Nullable RoomProfile profile) {
        QualityBreakdown breakdown = new QualityBreakdown();

        breakdown.sizeScore      = evaluateSize(volume) * SIZE_MAX;
        breakdown.enclosureScore = Math.max(0, enclosureScore) * ENCLOSURE_MAX;

        float furnitureQuality   = evaluateFurniture(furnitureCounts);
        breakdown.furnitureScore = Math.min(1.0f, furnitureQuality) * FURNITURE_MAX;

        float rawTheme = (profile != null)
                ? evaluateSignalTheme(signalCounts, lightLevel, enclosureScore, profile)
                : evaluateCraftingSetup(signalCounts);
        breakdown.themeScore = rawTheme * THEME_MAX;

        breakdown.totalQuality = Math.min(1.0f,
                breakdown.sizeScore + breakdown.enclosureScore
                + breakdown.furnitureScore + breakdown.themeScore);

        breakdown.components.put("Size",      breakdown.sizeScore);
        breakdown.components.put("Enclosure", breakdown.enclosureScore);
        breakdown.components.put("Furniture", breakdown.furnitureScore);
        breakdown.components.put("Theme",     breakdown.themeScore);

        return breakdown;
    }

    // --- Theme scoring ---

    /**
     * Computes a [0,1] theme score from the profile's quality_signals and penalty_signals.
     *
     * Positive contribution: fraction of quality_signals that are satisfied.
     * Negative contribution: fraction of penalty_signals that are active, scaled by PENALTY_WEIGHT.
     *
     * Signals are resolved via direct signalCounts lookup, then SIGNAL_ALIASES, then a small
     * set of special properties (lighting via lightLevel, enclosure via enclosureScore).
     * Signals that cannot be resolved against any of these simply contribute nothing to the
     * numerator — they are counted in the denominator so rooms cannot trivially reach 1.0
     * by listing only signals that happen to be present.
     */
    private static float evaluateSignalTheme(Map<String, Integer> signalCounts,
            int lightLevel, float enclosureScore, RoomProfile profile) {
        List<String> qualitySignals = profile.qualitySignals();
        List<String> penaltySignals = profile.penaltySignals();

        if (qualitySignals.isEmpty()) {
            return evaluateCraftingSetup(signalCounts);
        }

        int satisfied = 0;
        for (String s : qualitySignals) {
            if (isSignalPresent(s, signalCounts, lightLevel, enclosureScore)) {
                satisfied++;
            }
        }
        float positiveScore = (float) satisfied / qualitySignals.size();

        float penaltyScore = 0;
        if (!penaltySignals.isEmpty()) {
            int active = 0;
            for (String s : penaltySignals) {
                if (isPenaltyActive(s, signalCounts, lightLevel, enclosureScore)) {
                    active++;
                }
            }
            penaltyScore = ((float) active / penaltySignals.size()) * PENALTY_WEIGHT;
        }

        return Math.max(0, Math.min(1, positiveScore - penaltyScore));
    }

    /**
     * Returns true when a quality signal is satisfied — the corresponding block or property
     * exists in the room at a non-zero count.
     */
    private static boolean isSignalPresent(String signal, Map<String, Integer> signalCounts,
            int lightLevel, float enclosureScore) {
        if (signalCounts.getOrDefault(signal, 0) > 0) return true;

        String alias = SIGNAL_ALIASES.get(signal);
        if (alias != null && signalCounts.getOrDefault(alias, 0) > 0) return true;

        return switch (signal) {
            case "lighting", "light" -> lightLevel >= 10;
            case "enclosure"         -> enclosureScore >= 0.80f;
            default                  -> false;
        };
    }

    /**
     * Returns true when a penalty signal is active.
     *
     * Signals beginning with "low_", "no_", or "missing_" express "absence of something good"
     * and are active when the base concept is NOT present. All other penalty signals are
     * active when the bad thing IS present.
     */
    private static boolean isPenaltyActive(String signal, Map<String, Integer> signalCounts,
            int lightLevel, float enclosureScore) {
        if (signal.startsWith("low_") || signal.startsWith("no_") || signal.startsWith("missing_")) {
            String base = signal.replaceFirst("^(low_|no_|missing_)", "");
            return !isSignalPresent(base, signalCounts, lightLevel, enclosureScore);
        }
        return isSignalPresent(signal, signalCounts, lightLevel, enclosureScore);
    }

    // --- Generic fallback (used when no profile is matched) ---

    private static float evaluateCraftingSetup(Map<String, Integer> signals) {
        if (signals.isEmpty()) return 0.0f;

        int count = 0;
        for (String signal : signals.keySet()) {
            if (isCraftingSignal(signal.toLowerCase())) {
                count += signals.get(signal);
            }
        }

        if (count >= 5) return 1.0f;
        if (count >= 3) return 0.6f;
        if (count >= 1) return 0.3f;
        return 0.0f;
    }

    private static boolean isCraftingSignal(String signal) {
        return signal.contains("brewing") || signal.contains("enchanting") || signal.contains("furnace")
                || signal.contains("cauldron") || signal.contains("loom") || signal.contains("stonecutter")
                || signal.contains("cartography") || signal.contains("smithing") || signal.contains("crafting");
    }

    // --- Size and furniture ---

    private static float evaluateSize(int volume) {
        if (volume < 30)  return 0.1f;
        if (volume < 50)  return 0.2f;
        if (volume < 100) return 0.5f;
        if (volume < 200) return 0.8f;
        return 1.0f;
    }

    private static float evaluateFurniture(Map<String, Integer> furnitureCounts) {
        if (furnitureCounts.isEmpty()) return 0.0f;

        float totalRarity = 0;
        int totalBlocks   = 0;

        for (Map.Entry<String, Integer> entry : furnitureCounts.entrySet()) {
            int count       = entry.getValue();
            float blockRarity = 0.5f;
            try {
                ResourceLocation rl = ResourceLocation.parse(entry.getKey());
                Block block         = BuiltInRegistries.BLOCK.get(rl);
                blockRarity         = BlockRarityCache.getRarity(block);
            } catch (Exception ignored) {
            }
            totalRarity  += blockRarity * count;
            totalBlocks  += count;
        }

        float avgRarity       = totalBlocks > 0 ? totalRarity / totalBlocks : 0.5f;
        float rarityNormalized = (avgRarity - 0.5f) / 1.5f;
        float diversityBonus  = Math.min(1.0f, furnitureCounts.size() / 5.0f);
        return rarityNormalized * 0.6f + diversityBonus * 0.4f;
    }

    // --- Breakdown record ---

    public static class QualityBreakdown {
        public float sizeScore;
        public float enclosureScore;
        public float furnitureScore;
        public float themeScore;
        public float totalQuality;

        public final Map<String, Float> components = new HashMap<>();

        public QualityBreakdown() {
        }
    }
}
