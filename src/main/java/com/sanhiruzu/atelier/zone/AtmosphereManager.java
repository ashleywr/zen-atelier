package com.sanhiruzu.atelier.zone;

import com.sanhiruzu.atelier.api.IAtmosphere;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Manages atmosphere properties for zones.
 * Provides access to air quality and environmental data.
 */
public class AtmosphereManager {
    private final ServerLevel level;

    public AtmosphereManager(ServerLevel level) {
        this.level = level;
    }

    public static AtmosphereManager get(ServerLevel level) {
        return new AtmosphereManager(level);
    }

    /**
     * Get atmosphere data at a specific block position.
     * Returns a default atmosphere if no zone exists at that position.
     */
    @Nullable
    public IAtmosphere getAtmosphereAt(BlockPos pos) {
        var manager = ZoneManager.get(level);
        if (manager == null) return new DefaultAtmosphere();
        var zone = manager.getAt(pos);
        if (zone == null) {
            return new DefaultAtmosphere();
        }
        return new ZoneAtmosphere(zone);
    }

    /**
     * Get or create an outdoor zone at the given position.
     * Used for areas outside enclosed rooms.
     */
    public IAtmosphere getOrCreateOutdoorZone(BlockPos pos) {
        return getAtmosphereAt(pos);
    }

    /**
     * Determine the atmosphere type at a position based on conditions.
     */
    public static String determineAtmosphere(IAtmosphere atmosphere) {
        if (atmosphere == null) return "neutral";

        float temp = atmosphere.getTemperature();
        float humidity = atmosphere.getHumidity();
        float purity = atmosphere.getChemicalPurity();

        if (temp > 60.0f) {
            return "hot";
        } else if (temp < -10.0f) {
            return "cold";
        } else if (humidity > 80.0f) {
            return "humid";
        } else if (purity < 30.0f) {
            return "polluted";
        }

        return "neutral";
    }

    /**
     * Default atmosphere implementation for areas outside zones.
     */
    private static class DefaultAtmosphere implements IAtmosphere {
        private float temperature = 20.0f;
        private float humidity = 50.0f;

        @Override
        public float getChemicalPurity() {
            return 100.0f; // Clean air by default
        }

        @Override
        public float getParticulateDensity() {
            return 0.0f; // No particles by default
        }

        @Override
        public float getTemperature() {
            return temperature;
        }

        @Override
        public float getHumidity() {
            return humidity;
        }

        @Override
        public void addHeat(float amount) {
            temperature += amount;
        }

        @Override
        public void addChemicalPollution(float amount) {
            // Reduce purity (tracked internally would be 100 - pollution)
        }

        @Override
        public void setProperty(String key, float value) {
            // No-op for default atmosphere
        }

        @Override
        public float getProperty(String key, float defaultValue) {
            return defaultValue;
        }
    }

    /**
     * Atmosphere data tied to a zone.
     */
    private static class ZoneAtmosphere implements IAtmosphere {
        private final StandardZone zone;

        public ZoneAtmosphere(StandardZone zone) {
            this.zone = zone;
        }

        @Override
        public float getChemicalPurity() {
            // Use zone quality as a proxy for chemical purity
            return zone.getQuality() * 100.0f;
        }

        @Override
        public float getParticulateDensity() {
            // Inverse relationship: better quality zones have fewer particles
            return Math.max(0, 100.0f - (zone.getQuality() * 100.0f));
        }

        @Override
        public float getTemperature() {
            // Use stored property or default
            return getProperty("temperature", 20.0f);
        }

        @Override
        public float getHumidity() {
            // Use stored property or default
            return getProperty("humidity", 50.0f);
        }

        @Override
        public void addHeat(float amount) {
            float current = getTemperature();
            setProperty("temperature", current + amount);
        }

        @Override
        public void addChemicalPollution(float amount) {
            float current = getProperty("pollution", 0.0f);
            setProperty("pollution", Math.max(0, current + amount));
        }

        @Override
        public void setProperty(String key, float value) {
            // Store as custom data on the zone
            var zoneData = zone.getZoneData();
            com.sanhiruzu.atelier.api.ZoneAPI.ZoneDataStore.set(zoneData.getRegionId(), "atm_" + key, value);
        }

        @Override
        public float getProperty(String key, float defaultValue) {
            var zoneData = zone.getZoneData();
            Object value = com.sanhiruzu.atelier.api.ZoneAPI.ZoneDataStore.get(zoneData.getRegionId(), "atm_" + key);
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return defaultValue;
        }
    }

    /**
     * Get atmosphere definition by ID.
     */
    public static com.sanhiruzu.atelier.api.AtmosphereRegistry.AtmosphereDef getAtmosphere(String atmosphereId) {
        // Return a default atmosphere definition
        return new com.sanhiruzu.atelier.api.AtmosphereRegistry.AtmosphereDef(
            atmosphereId, 50.0f, 80.0f, 20.0f
        );
    }
}
