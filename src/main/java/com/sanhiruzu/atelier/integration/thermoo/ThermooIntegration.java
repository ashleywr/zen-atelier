package com.sanhiruzu.atelier.integration.thermoo;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.api.ZoneAPI;
import com.sanhiruzu.atelier.space.zone.ZoneAtmosphere;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.reflect.Method;

/**
 * Applies zone climate (from {@link ZoneAtmosphere}) to player temperature via Thermoo.
 *
 * <p>Thermoo injects temperature management into {@link net.minecraft.world.entity.LivingEntity}
 * using Mixin. The injected methods are accessed by name via reflection so this integration
 * requires no compile-time dependency on Thermoo.
 *
 * <p>Expected Thermoo Mixin API on LivingEntity:
 * <pre>
 *   int  thermoo$getTemperature()
 *   void thermoo$setTemperature(int temperature)
 * </pre>
 *
 * <p>Temperature model: the zone's {@link ZoneAtmosphere#temperatureOffset()} (Atelier units,
 * roughly -100 to +100) is translated to Thermoo units and used as the equilibrium target.
 * Each application interval, the player's temperature is nudged 10% of the remaining distance
 * toward that target. This competes naturally with Thermoo's environmental factors (biome cold,
 * equipment insulation, etc.) so equilibrium reflects both the room and the outside world.
 *
 * <p>Tune {@link #THERMOO_UNITS_PER_ATELIER_UNIT} if the zone effect feels too weak or strong
 * relative to your Thermoo installation's temperature range (typically ±2600 for extreme states).
 */
public final class ThermooIntegration {

    private static final String MOD_ID = "thermoo";

    /** How often to sample and apply zone temperature (once per second). */
    private static final int APPLY_INTERVAL_TICKS = 20;

    /**
     * Conversion factor from Atelier temperature units to Thermoo temperature units.
     * A zone offset of +40 (warm room) maps to a Thermoo target of +40 × FACTOR.
     * Increase if zone warmth feels negligible; decrease if it overwhelms Thermoo's own factors.
     */
    private static final int THERMOO_UNITS_PER_ATELIER_UNIT = 20;

    /** How fast the player moves toward the zone's temperature target per interval (0..1). */
    private static final float NUDGE_RATE = 0.10f;

    private static boolean initialized = false;
    private static Method thermooGetTemperature;
    private static Method thermooSetTemperature;

    private ThermooIntegration() {}

    public static void initialize() {
        if (initialized || !ModList.get().isLoaded(MOD_ID)) return;
        try {
            thermooGetTemperature = ServerPlayer.class.getMethod("thermoo$getTemperature");
            thermooSetTemperature = ServerPlayer.class.getMethod("thermoo$setTemperature", int.class);
            NeoForge.EVENT_BUS.addListener(ThermooIntegration::onServerTick);
            initialized = true;
            ZenAtelier.LOGGER.info("Thermoo integration enabled: zone climate influences player temperature");
        } catch (NoSuchMethodException ex) {
            ZenAtelier.LOGGER.warn(
                    "Thermoo integration: thermoo$getTemperature / thermoo$setTemperature not found on ServerPlayer. "
                    + "Zone climate will not affect player temperature. "
                    + "This is expected if your Thermoo version uses a different API surface.", ex);
        }
    }

    public static boolean isActive() {
        return initialized;
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        int tick = event.getServer().getTickCount();
        if (tick % APPLY_INTERVAL_TICKS != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                applyZoneClimate(player, level);
            }
        }
    }

    private static void applyZoneClimate(ServerPlayer player, ServerLevel level) {
        ZoneData zone = ZoneAPI.getIndoorZoneContaining(level, player.blockPosition());
        ZoneAtmosphere atmosphere = ZoneAPI.getAtmosphere(zone);
        if (atmosphere == null || atmosphere.temperatureOffset() == 0) return;

        int target = atmosphere.temperatureOffset() * THERMOO_UNITS_PER_ATELIER_UNIT;
        try {
            int current = (int) thermooGetTemperature.invoke(player);
            // Nudge current temperature toward zone target; never overshoot
            int delta = (int) ((target - current) * NUDGE_RATE);
            if (delta == 0) {
                delta = target > current ? 1 : -1;
            }
            int next = target > current
                    ? Math.min(target, current + delta)
                    : Math.max(target, current + delta);
            if (next != current) {
                thermooSetTemperature.invoke(player, next);
            }
        } catch (ReflectiveOperationException ex) {
            // Suppress per-tick errors after initialization succeeded
        }
    }
}
