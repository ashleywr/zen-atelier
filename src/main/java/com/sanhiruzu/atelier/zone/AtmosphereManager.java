package com.sanhiruzu.atelier.zone;

import com.sanhiruzu.atelier.api.IAtmosphere;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneAtmosphere;
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
        return new ZoneAtmosphereAdapter(zone);
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
     * IAtmosphere adapter backed by a zone's computed ZoneAtmosphere.
     * Reads temperature, humidity, and air quality from ZoneAtmosphere when available,
     * falling back to the ZoneDataStore for any values set explicitly by other mods.
     */
    private static class ZoneAtmosphereAdapter implements IAtmosphere {
        private final StandardZone zone;

        public ZoneAtmosphereAdapter(StandardZone zone) {
            this.zone = zone;
        }

        private @Nullable ZoneAtmosphere atmosphere() {
            return zone.getZoneData() instanceof RoomData room ? room.getAtmosphere() : null;
        }

        @Override
        public float getChemicalPurity() {
            ZoneAtmosphere atm = atmosphere();
            return atm != null ? atm.airQuality() * 100.0f : zone.getQuality() * 100.0f;
        }

        @Override
        public float getParticulateDensity() {
            return Math.max(0, 100.0f - getChemicalPurity());
        }

        @Override
        public float getTemperature() {
            ZoneAtmosphere atm = atmosphere();
            if (atm != null) return 20.0f + atm.temperatureOffset() * 0.5f;
            return getProperty("temperature", 20.0f);
        }

        @Override
        public float getHumidity() {
            ZoneAtmosphere atm = atmosphere();
            if (atm != null) return atm.humidity() * 100.0f;
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
            var zoneData = zone.getZoneData();
            com.sanhiruzu.atelier.api.ZoneAPI.ZoneDataStore.set(zoneData.getRegionId(), "atm_" + key, value);
        }

        @Override
        public float getProperty(String key, float defaultValue) {
            var zoneData = zone.getZoneData();
            Object value = com.sanhiruzu.atelier.api.ZoneAPI.ZoneDataStore.get(zoneData.getRegionId(), "atm_" + key);
            if (value instanceof Number n) {
                return n.floatValue();
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
