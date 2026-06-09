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
}
