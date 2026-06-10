package com.sanhiruzu.atelier.space.zone;

import java.util.Map;

/**
 * Computed atmospheric properties for an indoor zone.
 * Derived at evaluation time from signal counts and enclosure score.
 * Consumed by the Thermoo integration (temperature), IAtmosphere adapter (humidity, air quality),
 * and any other mod querying interior environmental metrics via ZoneAPI.
 *
 * <p>Temperature offset scale: roughly -100 (deep ice room) to +100 (blazing forge).
 * A well-enclosed room with no sources is ~+12 from the indoor baseline alone.
 * Humidity is 0..1 (0 = very dry, 1 = soaking). Air quality is 0..1 (1 = clean).
 */
public record ZoneAtmosphere(int temperatureOffset, float humidity, float airQuality) {

    public static final ZoneAtmosphere NEUTRAL = new ZoneAtmosphere(0, 0.5f, 1.0f);

    public static ZoneAtmosphere fromSignals(Map<String, Integer> signals, float enclosureScore) {
        int heatSources    = signals.getOrDefault("heat_source", 0);
        int cookingBlocks  = signals.getOrDefault("cooking_block", 0);
        int smithingBlocks = signals.getOrDefault("smithing_or_repair_block", 0);
        int iceBlocks      = signals.getOrDefault("ice_block", 0);
        int waterCoverage  = signals.getOrDefault("water_coverage", 0);
        int plants         = signals.getOrDefault("plant", 0);

        // Heat from open fire/furnaces/smithing; cap so no room becomes arbitrarily extreme
        int heatOffset = Math.min(60, heatSources * 6 + cookingBlocks * 4 + smithingBlocks * 10);
        // Cold from ice/snow blocks; cap symmetrically
        int coldOffset = Math.min(50, iceBlocks * 8);
        // Well-enclosed rooms retain heat (max +12 at full enclosure)
        int indoorBaseline = Math.round(enclosureScore * 12f);

        int temperatureOffset = indoorBaseline + heatOffset - coldOffset;

        float humidity = Math.min(1.0f,
                0.3f
                + Math.min(0.4f, waterCoverage * 0.015f)
                + Math.min(0.15f, plants * 0.02f));

        float airQuality = Math.max(0.1f, Math.min(1.0f,
                0.7f + enclosureScore * 0.3f
                - (heatSources + cookingBlocks) * 0.015f
                - smithingBlocks * 0.02f));

        return new ZoneAtmosphere(temperatureOffset, humidity, airQuality);
    }
}
