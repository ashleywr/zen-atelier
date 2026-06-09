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

    @Override
    public String toString() {
        return "StandardZone{" +
                "volume=" + getVolume() +
                ", quality=" + getQuality() +
                ", indoor=" + isIndoor() +
                '}';
    }
}
