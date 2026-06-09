package com.sanhiruzu.atelier.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for custom atmosphere types.
 * Allows mods to register and retrieve atmosphere configurations.
 */
public class AtmosphereRegistry {
    private static final Map<String, AtmosphereType> REGISTRY = new HashMap<>();

    /**
     * Register a custom atmosphere type.
     */
    public static void register(String id, AtmosphereType type) {
        REGISTRY.put(id, type);
    }

    /**
     * Get a registered atmosphere type.
     */
    public static AtmosphereType get(String id) {
        return REGISTRY.getOrDefault(id, AtmosphereType.DEFAULT);
    }

    /**
     * Check if an atmosphere type is registered.
     */
    public static boolean isRegistered(String id) {
        return REGISTRY.containsKey(id);
    }

    /**
     * Represents a type of atmosphere with specific properties.
     */
    public static class AtmosphereType {
        public static final AtmosphereType DEFAULT = new AtmosphereType("default", 100.0f, 0.0f, 20.0f);

        private final String id;
        private final float chemicalPurity;
        private final float particulateDensity;
        private final float temperature;

        public AtmosphereType(String id, float chemicalPurity, float particulateDensity, float temperature) {
            this.id = id;
            this.chemicalPurity = chemicalPurity;
            this.particulateDensity = particulateDensity;
            this.temperature = temperature;
        }

        public String getId() {
            return id;
        }

        public float getChemicalPurity() {
            return chemicalPurity;
        }

        public float getParticulateDensity() {
            return particulateDensity;
        }

        public float getTemperature() {
            return temperature;
        }
    }
}
