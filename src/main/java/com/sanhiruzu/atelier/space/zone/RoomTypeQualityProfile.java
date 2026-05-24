package com.sanhiruzu.atelier.space.zone;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines quality weighting for a specific room type.
 * Different room types value different aspects: a greenhouse cares about plants,
 * an enchanting room cares about magic aesthetics, a smithy cares about metal/heat.
 */
public class RoomTypeQualityProfile {
    private final float sizeWeight;
    private final float enclosureWeight;
    private final float furnitureWeight;
    private final float themeWeight;

    // Theme-specific bonus factors (multipliers on theme score)
    private final Map<String, Float> themeFactors = new HashMap<>();

    public RoomTypeQualityProfile(float size, float enclosure, float furniture, float theme) {
        this.sizeWeight = size;
        this.enclosureWeight = enclosure;
        this.furnitureWeight = furniture;
        this.themeWeight = theme;
        normalizeWeights();
    }

    private void normalizeWeights() {
        float total = sizeWeight + enclosureWeight + furnitureWeight + themeWeight;
        if (total <= 0) return;
        // Weights should sum to ~1.0, but we let them be flexible
    }

    public void addThemeFactor(String keyword, float multiplier) {
        themeFactors.put(keyword, multiplier);
    }

    public float getSizeWeight() {
        return sizeWeight;
    }

    public float getEnclosureWeight() {
        return enclosureWeight;
    }

    public float getFurnitureWeight() {
        return furnitureWeight;
    }

    public float getThemeWeight() {
        return themeWeight;
    }

    public Map<String, Float> getThemeFactors() {
        return themeFactors;
    }

    // Default profiles for common room types
    public static final RoomTypeQualityProfile BEDROOM = createBedroom();
    public static final RoomTypeQualityProfile GREENHOUSE = createGreenhouse();
    public static final RoomTypeQualityProfile ENCHANTING = createEnchanting();
    public static final RoomTypeQualityProfile SMITHY = createSmithing();

    private static RoomTypeQualityProfile createBedroom() {
        RoomTypeQualityProfile profile = new RoomTypeQualityProfile(0.25f, 0.35f, 0.25f, 0.15f);
        profile.addThemeFactor("bed", 2.0f);
        profile.addThemeFactor("wool", 1.5f);
        profile.addThemeFactor("carpet", 1.5f);
        profile.addThemeFactor("pink", 1.2f);
        profile.addThemeFactor("soft", 1.3f);
        return profile;
    }

    private static RoomTypeQualityProfile createGreenhouse() {
        RoomTypeQualityProfile profile = new RoomTypeQualityProfile(0.20f, 0.30f, 0.20f, 0.30f);
        profile.addThemeFactor("green", 2.0f);
        profile.addThemeFactor("plant", 2.0f);
        profile.addThemeFactor("leaves", 1.8f);
        profile.addThemeFactor("grass", 1.5f);
        profile.addThemeFactor("flower", 1.5f);
        profile.addThemeFactor("moss", 1.5f);
        profile.addThemeFactor("vine", 1.3f);
        profile.addThemeFactor("glass", 1.2f);  // Light matters
        profile.addThemeFactor("light", 1.3f);
        return profile;
    }

    private static RoomTypeQualityProfile createEnchanting() {
        RoomTypeQualityProfile profile = new RoomTypeQualityProfile(0.25f, 0.30f, 0.20f, 0.25f);
        profile.addThemeFactor("enchanting_table", 2.0f);
        profile.addThemeFactor("bookshelf", 1.8f);
        profile.addThemeFactor("amethyst", 1.6f);
        profile.addThemeFactor("purple", 1.5f);
        profile.addThemeFactor("quartz", 1.4f);
        profile.addThemeFactor("ender", 1.5f);
        profile.addThemeFactor("lantern", 1.3f);
        return profile;
    }

    private static RoomTypeQualityProfile createSmithing() {
        RoomTypeQualityProfile profile = new RoomTypeQualityProfile(0.30f, 0.25f, 0.20f, 0.25f);
        profile.addThemeFactor("anvil", 2.0f);
        profile.addThemeFactor("furnace", 1.8f);
        profile.addThemeFactor("iron", 1.7f);
        profile.addThemeFactor("metal", 1.6f);
        profile.addThemeFactor("copper", 1.5f);
        profile.addThemeFactor("stone", 1.3f);
        profile.addThemeFactor("fire", 1.4f);  // Heat matters
        profile.addThemeFactor("magma", 1.5f);
        return profile;
    }
}
