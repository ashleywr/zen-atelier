package com.sanhiruzu.atelier;

/**
 * Configuration class for Zen Atelier compatibility.
 * Provides common configuration values used by mods like Create Kaizen.
 */
public class ZenZonesConfig {

    /**
     * Get the minimum quality threshold for a zone to be considered viable.
     */
    public static float getMinimumQualityThreshold() {
        return 0.3f; // 30% quality
    }

    /**
     * Get the maximum quality value a zone can reach.
     */
    public static float getMaximumQuality() {
        return 1.0f; // 100%
    }

    /**
     * Check if a quality value is acceptable for basic operations.
     */
    public static boolean isQualityAcceptable(float quality) {
        return quality >= getMinimumQualityThreshold();
    }

    /**
     * Check if a quality value is optimal for operations.
     */
    public static boolean isQualityOptimal(float quality) {
        return quality >= 0.8f; // 80% or higher
     }

    /**
     * Get the default volume required for a zone to function.
     */
    public static int getMinimumZoneVolume() {
        return 64; // 4x4x4 blocks
    }

    /**
     * Check if a volume is sufficient for basic operations.
     */
    public static boolean isVolumeSufficient(int volume) {
        return volume >= getMinimumZoneVolume();
    }

    /**
     * Get the ticks between atmosphere quality updates.
     */
    public static int getAtmosphereUpdateInterval() {
        return 20; // Once per second
    }

    /**
     * Check if atmosphere effects should be applied at a position.
     */
    public static boolean shouldApplyAtmosphereEffects(float quality) {
        return quality < 0.5f; // Apply effects when quality drops below 50%
    }

    /**
     * Format temperature for display.
     */
    public static String formatTemperature(float celsius) {
        return String.format("%.1f°C", celsius);
    }
}
