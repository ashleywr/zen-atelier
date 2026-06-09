package com.sanhiruzu.atelier.zone;

import com.sanhiruzu.atelier.api.IAtmosphere;
import com.sanhiruzu.atelier.api.ZoneAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Compatibility wrapper for accessing zone data.
 * Provides a unified interface for Create Kaizen to query zone information.
 */
public class ZoneManager {
    private final ServerLevel level;

    public ZoneManager(ServerLevel level) {
        this.level = level;
    }

    public static ZoneManager get(ServerLevel level) {
        return new ZoneManager(level);
    }

    @Nullable
    public static ZoneManager get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return new ZoneManager(serverLevel);
        }
        return null;
    }

    @Nullable
    public StandardZone getAt(BlockPos pos) {
        var zoneData = ZoneAPI.getZoneContaining(level, pos);
        if (zoneData == null) return null;
        return new StandardZone(zoneData);
    }

    @Nullable
    public StandardZone getIndoorZoneAt(BlockPos pos) {
        var zoneData = ZoneAPI.getIndoorZoneContaining(level, pos);
        if (zoneData == null) return null;
        return new StandardZone(zoneData);
    }

    public boolean isInsideRoom(BlockPos pos) {
        return ZoneAPI.isInsideRoom(level, pos);
    }

    public IAtmosphere getOrCreateOutdoorZone(BlockPos pos) {
        return AtmosphereManager.get(level).getOrCreateOutdoorZone(pos);
    }

    public void tick() {
        // Update zone-related systems each tick
        // This is called by Create Kaizen to update atmosphere effects
    }

    public void updateZoneBoundsAuthoritatively(StandardZone zone, net.minecraft.world.level.levelgen.structure.BoundingBox bounds) {
        // Update the zone's spatial bounds
        var zoneData = zone.getZoneData();
        if (zoneData != null) {
            zoneData.setSpatialExtent(bounds.minX(), bounds.minY(), bounds.minZ(),
                                      bounds.maxX(), bounds.maxY(), bounds.maxZ());
        }
    }

    public void removeZoneById(java.util.UUID zoneId) {
        // Remove a zone by ID
        // In a full implementation, this would mark the zone as removed
    }

    public void orphanZone(java.util.UUID zoneId) {
        // Orphan a zone (remove ownership but keep data)
        // In a full implementation, this would change the zone's owner
    }

    public StandardZone createOrReplaceManagedZone(java.util.UUID zoneId, net.minecraft.world.level.levelgen.structure.BoundingBox bounds,
                                                   String displayName, String atmosphereId, BlockPos pos, String identifier) {
        // Create or replace a managed zone
        var zoneData = getAt(pos);
        if (zoneData == null) {
            zoneData = new StandardZone(new com.sanhiruzu.atelier.space.zone.ZoneData(zoneId, 1000, 0.5f) {
                @Override
                public boolean isOutdoor() {
                    return false;
                }
            });
        }
        // Update the zone's spatial bounds
        updateZoneBoundsAuthoritatively(zoneData, bounds);
        return zoneData;
    }
}
