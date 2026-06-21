package com.sanhiruzu.atelier.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class EnvironmentCacheInvalidation {
    public static final int DEFAULT_DIRTY_RADIUS = 16;

    private EnvironmentCacheInvalidation() {
    }

    public static Object levelKey(Level level) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        return level;
    }

    public static void markDirty(EnvironmentSnapshotCache cache, Level level, BlockPos pos) {
        if (cache == null || level == null || pos == null) {
            return;
        }
        cache.markDirty(levelKey(level), pos.getX(), pos.getY(), pos.getZ(), DEFAULT_DIRTY_RADIUS);
    }

    public static void clearLevel(EnvironmentSnapshotCache cache, Level level) {
        if (cache != null && level != null) {
            cache.clearLevel(levelKey(level));
        }
    }
}
