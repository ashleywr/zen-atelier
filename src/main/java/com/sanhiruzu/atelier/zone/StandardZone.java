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
        return zoneData.getOrigin();
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

    @Override
    public String toString() {
        return "StandardZone{" +
                "volume=" + getVolume() +
                ", quality=" + getQuality() +
                ", indoor=" + isIndoor() +
                '}';
    }
}
