package com.sanhiruzu.atelier.api;

import com.sanhiruzu.atelier.environment.EnvironmentCacheInvalidation;
import com.sanhiruzu.atelier.environment.EnvironmentScanProfile;
import com.sanhiruzu.atelier.environment.EnvironmentScanner;
import com.sanhiruzu.atelier.environment.EnvironmentSnapshotCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Point-of-use query facade for local environment facts and declarative effects.
 * There are no room/zone objects. Callers sample a location when an event fires.
 */
public final class ZoneAPI {
    private static final EnvironmentSnapshotCache SNAPSHOT_CACHE = new EnvironmentSnapshotCache();

    private ZoneAPI() {
    }

    public static EnvironmentSnapshot environmentAt(Level level, BlockPos pos, int radius) {
        requireEnvironmentLevel(level);
        requireEnvironmentPos(pos);
        int clampedRadius = clampScanRadius(radius);
        EnvironmentScanProfile profile = EnvironmentScanProfile.BLOCKS_AND_ENTITIES;
        return SNAPSHOT_CACHE.getOrCompute(
                EnvironmentCacheInvalidation.levelKey(level),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                clampedRadius,
                profile,
                () -> EnvironmentScanner.scan(level, pos, clampedRadius, profile)
        );
    }

    static Level requireEnvironmentLevel(Level level) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        return level;
    }

    static BlockPos requireEnvironmentPos(BlockPos pos) {
        if (pos == null) {
            throw new IllegalArgumentException("pos must not be null");
        }
        return pos;
    }

    static int clampScanRadius(int radius) {
        return Math.max(0, Math.min(EnvironmentScanner.MAX_RADIUS, radius));
    }

    public static List<EnvironmentEffect> evaluateEnvironmentEffects(
            EnvironmentEffectContext context,
            Level level,
            BlockPos pos,
            int radius
    ) {
        return EnvironmentEffectRegistry.global().evaluate(context, environmentAt(level, pos, radius));
    }

    public static void registerEnvironmentEffects(Consumer<EnvironmentEffectRegistrar> registration) {
        EnvironmentEffectRegistry.global().register(registration);
    }

    public static void markEnvironmentDirty(Level level, BlockPos pos) {
        EnvironmentCacheInvalidation.markDirty(SNAPSHOT_CACHE, level, pos);
    }

    public static void clearEnvironmentCache(Level level) {
        EnvironmentCacheInvalidation.clearLevel(SNAPSHOT_CACHE, level);
    }

    /** True if any block within {@code radius} of {@code pos} matches {@code predicate}. Bounded, server-or-client safe. */
    public static boolean hasNearby(Level level, BlockPos pos, Predicate<BlockState> predicate, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(
                pos.offset(-radius, -radius, -radius),
                pos.offset(radius, radius, radius))) {
            if (predicate.test(level.getBlockState(p))) {
                return true;
            }
        }
        return false;
    }

    /** Neutral/ambient atmosphere reading retained for compatibility with existing callers. */
    public static AtmosphereReading atmosphereAt(Level level, BlockPos pos) {
        return AtmosphereReading.AMBIENT;
    }

    /** Anonymous, identity-free local reading. Scalars are neutral until the new environment facts replace callers. */
    public record AtmosphereReading(float heat, float humidity, float particulate) {
        public static final AtmosphereReading AMBIENT = new AtmosphereReading(0f, 0f, 0f);
    }
}
