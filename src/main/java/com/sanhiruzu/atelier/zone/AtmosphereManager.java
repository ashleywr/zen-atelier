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
        var zone = ZoneManager.get(level).getAt(pos);
        if (zone == null) {
            return new DefaultAtmosphere();
        }
        return new ZoneAtmosphere(zone);
    }

    /**
     * Default atmosphere implementation for areas outside zones.
     */
    private static class DefaultAtmosphere implements IAtmosphere {
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
            return 20.0f; // Room temperature
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
            // Default room temperature
            return 20.0f;
        }

        @Override
        public void setProperty(String key, float value) {
            // Store as custom data on the zone
            var zoneData = zone.getZoneData();
            com.sanhiruzu.atelier.api.ZoneAPI.ZoneDataStore.set(zoneData.getId(), "atm_" + key, value);
        }

        @Override
        public float getProperty(String key, float defaultValue) {
            var zoneData = zone.getZoneData();
            Object value = com.sanhiruzu.atelier.api.ZoneAPI.ZoneDataStore.get(zoneData.getId(), "atm_" + key);
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return defaultValue;
        }
    }
}
