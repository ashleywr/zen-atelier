package com.sanhiruzu.atelier.zone;

import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.space.zone.RoomData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for zone data providing a consistent interface for Create Kaizen.
 */
public class StandardZone implements com.sanhiruzu.atelier.api.IAtmosphere {
    private final ZoneData zoneData;

    public StandardZone(ZoneData zoneData) {
        this.zoneData = zoneData;
    }

    public ZoneData getZoneData() {
        return zoneData;
    }

    @Nullable
    public RoomData asRoomData() {
        return zoneData instanceof RoomData ? (RoomData) zoneData : null;
    }

    public int getVolume() {
        return zoneData.getVolume();
    }

    public BlockPos getOrigin() {
        // Return the center of the spatial extent, or the minimum corner
        if (zoneData.hasSpatialExtent()) {
            return new BlockPos(
                (zoneData.getMinX() + zoneData.getMaxX()) / 2,
                (zoneData.getMinY() + zoneData.getMaxY()) / 2,
                (zoneData.getMinZ() + zoneData.getMaxZ()) / 2
            );
        }
        return BlockPos.ZERO;
    }

    public float getQuality() {
        if (zoneData instanceof RoomData room) {
            return room.getQuality();
        }
        return 0.0f;
    }

    @Nullable
    public String getZoneType() {
        if (zoneData instanceof RoomData room) {
            var typeId = room.getZoneTypeId();
            return typeId != null ? typeId.toString() : null;
        }
        return null;
    }

    public boolean isIndoor() {
        return zoneData instanceof RoomData;
    }

    public boolean contains(BlockPos pos) {
        return zoneData.contains(pos);
    }

    public float getZenScore() {
        // Quality score from 0-100
        return getQuality() * 100.0f;
    }

    public float getNetFlux() {
        // Net heat flux in the zone (0 by default, can be modified)
        return 0.0f;
    }

    public float getTemperature() {
        if (zoneData instanceof RoomData room) {
            var atm = room.getAtmosphere();
            if (atm != null) return 20.0f + atm.temperatureOffset() * 0.5f;
        }
        return getProperty("temperature", 20.0f);
    }

    public void setTemperature(float temp) {
        setProperty("temperature", temp);
    }

    public String getName() {
        // Generate a name based on zone type or ID
        String zoneType = getZoneType();
        if (zoneType != null) {
            return zoneType.replaceAll(".*:", "").replace("_", " ");
        }
        return "Unknown Zone";
    }

    public float getProperty(String key, float defaultValue) {
        Object value = zoneData.getRegionId(); // Use region ID for custom data storage
        // Store custom properties in API data store
        Object prop = com.sanhiruzu.atelier.api.ZoneAPI.ZoneDataStore.get(zoneData.getRegionId(), "zone_" + key);
        if (prop instanceof Number) {
            return ((Number) prop).floatValue();
        }
        return defaultValue;
    }

    public void setProperty(String key, float value) {
        com.sanhiruzu.atelier.api.ZoneAPI.ZoneDataStore.set(zoneData.getRegionId(), "zone_" + key, value);
    }

    public void addHeat(float amount) {
        // Add heat to the zone
        float current = getTemperature();
        setTemperature(current + amount);
    }

    public void addChemicalPollution(float amount) {
        // Add chemical pollution to the zone
        float current = getProperty("chemical_pollution", 0.0f);
        setProperty("chemical_pollution", current + amount);
    }

    public BoundingBox getBounds() {
        // Return the spatial extent as a bounding box
        if (zoneData.hasSpatialExtent()) {
            return new BoundingBox(
                zoneData.getMinX(), zoneData.getMinY(), zoneData.getMinZ(),
                zoneData.getMaxX(), zoneData.getMaxY(), zoneData.getMaxZ()
            );
        }
        // Return a default 1x1x1 box at origin if no extent
        BlockPos origin = getOrigin();
        return new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                               origin.getX(), origin.getY(), origin.getZ());
    }

    public float getHumidity() {
        if (zoneData instanceof RoomData room) {
            var atm = room.getAtmosphere();
            if (atm != null) return atm.humidity() * 100.0f;
        }
        return getProperty("humidity", 50.0f);
    }

    public void setHumidity(float humidity) {
        setProperty("humidity", humidity);
    }

    public float getChemicalPurity() {
        if (zoneData instanceof RoomData room) {
            var atm = room.getAtmosphere();
            if (atm != null) return atm.airQuality() * 100.0f;
        }
        return getQuality() * 100.0f;
    }

    public float getParticulateDensity() {
        return 100.0f - getChemicalPurity();
    }

    public boolean isRenderingWireframe() {
        return getProperty("render_wireframe", 0.0f) > 0.5f;
    }

    public void setRenderingWireframe(boolean rendering) {
        setProperty("render_wireframe", rendering ? 1.0f : 0.0f);
    }

    public java.util.UUID getId() {
        return zoneData.getRegionId();
    }

    public void setZenScore(int score) {
        setProperty("zen_score", (float) score);
    }

    public void setSealed(boolean sealed) {
        setProperty("sealed", sealed ? 1.0f : 0.0f);
    }

    public void initializeFromBiome(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        // Initialize zone properties from biome data
        // Stub implementation
    }

    @Override
    public String toString() {
        return "StandardZone{" +
                "volume=" + getVolume() +
                ", quality=" + getQuality() +
                ", indoor=" + isIndoor() +
                '}';
    }
}
