package com.sanhiruzu.atelier.zone;

import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.space.zone.RoomData;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for zone data providing a consistent interface for Create Kaizen.
 */
public class StandardZone {
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
        // Default room temperature, can be customized per zone
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

    private float getProperty(String key, float defaultValue) {
        Object value = zoneData.getRegionId(); // Use region ID for custom data storage
        // Store custom properties in API data store
        Object prop = com.sanhiruzu.atelier.api.ZoneAPI.ZoneDataStore.get(zoneData.getRegionId(), "zone_" + key);
        if (prop instanceof Number) {
            return ((Number) prop).floatValue();
        }
        return defaultValue;
    }

    private void setProperty(String key, float value) {
        com.sanhiruzu.atelier.api.ZoneAPI.ZoneDataStore.set(zoneData.getRegionId(), "zone_" + key, value);
    }

    public BlockPos getBounds() {
        // Return origin as a simple bounds reference
        return getOrigin();
    }

    public float getHumidity() {
        // Default humidity level
        return getProperty("humidity", 50.0f);
    }

    public void setHumidity(float humidity) {
        setProperty("humidity", humidity);
    }

    public float getChemicalPurity() {
        // Use quality as purity (0-1 becomes 0-100%)
        return getQuality() * 100.0f;
    }

    public boolean isRenderingWireframe() {
        return getProperty("render_wireframe", 0.0f) > 0.5f;
    }

    public void setRenderingWireframe(boolean rendering) {
        setProperty("render_wireframe", rendering ? 1.0f : 0.0f);
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
