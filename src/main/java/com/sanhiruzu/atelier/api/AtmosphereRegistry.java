package com.sanhiruzu.atelier.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for custom atmosphere types.
 * Allows mods to register and retrieve atmosphere configurations.
 */
public class AtmosphereRegistry {
    private static final Map<String, AtmosphereType> REGISTRY = new HashMap<>();
    private static final Map<String, AtmosphereDef> DEF_REGISTRY = new HashMap<>();

    /**
     * Register a custom atmosphere type.
     */
    public static void register(String id, AtmosphereType type) {
        REGISTRY.put(id, type);
    }

    /**
     * Register a default atmosphere definition.
     */
    public static void registerDefault(String id, AtmosphereDef def) {
        DEF_REGISTRY.put(id, def);
    }

    /**
     * Get a registered atmosphere definition.
     */
    public static AtmosphereDef getDefault(String id) {
        return DEF_REGISTRY.getOrDefault(id, new AtmosphereDef("default", 50.0f, 70.0f, 20.0f));
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

    /**
     * Atmosphere definition with requirements and properties.
     */
    public static class AtmosphereDef {
        private final String id;
        private final float target_humidity;
        private final float min_purity;
        private final float target_temp;
        private final String displayName;

        public AtmosphereDef(String id, float target_humidity, float min_purity, float target_temp) {
            this.id = id;
            this.target_humidity = target_humidity;
            this.min_purity = min_purity;
            this.target_temp = target_temp;
            this.displayName = id.replaceAll("_", " ").replaceAll("(?i)atmosphere", "").trim();
        }

        public AtmosphereDef(String id, float target_humidity, float min_purity, float target_temp, String displayName) {
            this.id = id;
            this.target_humidity = target_humidity;
            this.min_purity = min_purity;
            this.target_temp = target_temp;
            this.displayName = displayName;
        }

        public String getId() {
            return id;
        }

        public float target_humidity() {
            return target_humidity;
        }

        public float min_purity() {
            return min_purity;
        }

        public float target_temp() {
            return target_temp;
        }

        public float target_temperature() {
            return target_temp;
        }

        public String display_name() {
            return displayName;
        }
    }
}
