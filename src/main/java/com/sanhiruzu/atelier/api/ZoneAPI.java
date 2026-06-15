package com.sanhiruzu.atelier.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * Point-of-use query facade for the (forthcoming) atmosphere substrate.
 * <p>
 * There are no room/zone objects. Callers sample a location when an event fires.
 * {@link #hasNearby} is live now (a bounded scan); {@link #atmosphereAt} returns a
 * neutral reading until the scalar-field substrate exists.
 */
public final class ZoneAPI {
    private ZoneAPI() {
    }

    /** Neutral/ambient atmosphere reading. Placeholder until the substrate lands. */
    public static AtmosphereReading atmosphereAt(Level level, BlockPos pos) {
        return AtmosphereReading.AMBIENT;
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

    /** Anonymous, identity-free local reading. Scalars are neutral until the substrate fills them in. */
    public record AtmosphereReading(float heat, float humidity, float particulate) {
        public static final AtmosphereReading AMBIENT = new AtmosphereReading(0f, 0f, 0f);
    }
}
