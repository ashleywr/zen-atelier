package com.sanhiruzu.atelier.integration.minecolonies;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.space.SpaceQuery;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Applies mob effects to living colonist entities every 200 ticks (10 s) based on the
 * Atelier room quality at their assigned work building and home building positions.
 *
 * Work effects: quality >= 40 % in a matching room type.
 * Home effects: quality >= 50 % in a bedroom, residence, restaurant, or farm room.
 *
 * Effects have no particles/icon so they don't clutter the colonist UI, and their
 * duration is long enough (400 ticks = 20 s) that each 10-second pass refreshes them
 * with a comfortable margin.
 */
final class ColonistRoomBonuses {

    private static Class<?> citizenEntityClass;

    // Profile id -> effect config for the colonist's work building room.
    private static final Map<String, EffectConfig> WORK_EFFECTS = new HashMap<>();

    // Profile id -> effect config for the colonist's home building room.
    private static final Map<String, EffectConfig> HOME_EFFECTS = new HashMap<>();

    static {
        WORK_EFFECTS.put("minecolonies:smithy",      new EffectConfig(MobEffects.DIG_SPEED,       400, 2));
        WORK_EFFECTS.put("minecolonies:builder_hut", new EffectConfig(MobEffects.DIG_SPEED,       400, 1));
        WORK_EFFECTS.put("minecolonies:workshop",    new EffectConfig(MobEffects.DIG_SPEED,       400, 1));
        WORK_EFFECTS.put("minecolonies:guard_post",  new EffectConfig(MobEffects.DAMAGE_BOOST,    400, 1));
        WORK_EFFECTS.put("minecolonies:restaurant",  new EffectConfig(MobEffects.SATURATION,      400, 1));
        WORK_EFFECTS.put("minecolonies:farm",        new EffectConfig(MobEffects.SATURATION,      400, 1));
        WORK_EFFECTS.put("minecolonies:library",     new EffectConfig(MobEffects.LUCK,            400, 1));
        WORK_EFFECTS.put("minecolonies:arcane_study",new EffectConfig(MobEffects.LUCK,            400, 2));
    }

    static {
        HOME_EFFECTS.put("minecolonies:residence",  new EffectConfig(MobEffects.REGENERATION, 400, 1));
        HOME_EFFECTS.put("zen_atelier:bedroom",     new EffectConfig(MobEffects.REGENERATION, 400, 1));
        HOME_EFFECTS.put("minecolonies:restaurant", new EffectConfig(MobEffects.SATURATION,   400, 1));
        HOME_EFFECTS.put("minecolonies:farm",       new EffectConfig(MobEffects.SATURATION,   400, 1));
    }

    private ColonistRoomBonuses() {
    }

    static void initialize() {
        try {
            citizenEntityClass = Class.forName("com.minecolonies.api.entity.citizen.AbstractEntityCitizen");
        } catch (ClassNotFoundException ex) {
            ZenAtelier.LOGGER.warn("MineColonies citizen class not found; colonist room bonuses disabled");
        }
    }

    static void tick(ServerLevel level, int tickCount) {
        if (citizenEntityClass == null || tickCount % 200 != 0) {
            return;
        }
        try {
            applyAll(level);
        } catch (ReflectiveOperationException | LinkageError ex) {
            ZenAtelier.LOGGER.debug("ColonistRoomBonuses tick error", ex);
        }
    }

    private static void applyAll(ServerLevel level) throws ReflectiveOperationException {
        for (Entity entity : level.getAllEntities()) {
            if (!citizenEntityClass.isInstance(entity) || !(entity instanceof LivingEntity living)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Optional<Object> dataOpt = (Optional<Object>) call(entity, "getCitizenData");
            if (dataOpt == null || dataOpt.isEmpty()) {
                continue;
            }
            Object citizen = dataOpt.get();

            applyWorkEffect(level, citizen, living);
            applyHomeEffect(level, citizen, living);
        }
    }

    private static void applyWorkEffect(ServerLevel level, Object citizen, LivingEntity entity)
            throws ReflectiveOperationException {
        Object workBuilding = call(citizen, "getWorkBuilding");
        if (workBuilding == null) return;

        BlockPos pos = posOf(workBuilding);
        if (pos == null) return;

        ZoneData zone = SpaceQuery.getRoomAt(level, pos);
        if (!(zone instanceof RoomData room) || room.getZoneTypeId() == null) return;

        int quality = Math.round(room.getQuality() * 100);
        if (quality < 40) return;

        EffectConfig cfg = WORK_EFFECTS.get(room.getZoneTypeId().toString());
        if (cfg == null) return;

        entity.addEffect(new MobEffectInstance(cfg.effect, cfg.duration, effectLevel(quality, cfg), false, false));
    }

    private static void applyHomeEffect(ServerLevel level, Object citizen, LivingEntity entity)
            throws ReflectiveOperationException {
        Object homeBuilding = call(citizen, "getHomeBuilding");
        if (homeBuilding == null) return;

        BlockPos pos = posOf(homeBuilding);
        if (pos == null) return;

        ZoneData zone = SpaceQuery.getRoomAt(level, pos);
        if (!(zone instanceof RoomData room) || room.getZoneTypeId() == null) return;

        int quality = Math.round(room.getQuality() * 100);
        if (quality < 50) return;

        EffectConfig cfg = HOME_EFFECTS.get(room.getZoneTypeId().toString());
        if (cfg == null) return;

        entity.addEffect(new MobEffectInstance(cfg.effect, cfg.duration, effectLevel(quality, cfg), false, false));
    }

    private static int effectLevel(int quality, EffectConfig cfg) {
        return (quality >= 75 && cfg.maxLevel > 1) ? cfg.maxLevel - 1 : 0;
    }

    private static BlockPos posOf(Object building) throws ReflectiveOperationException {
        Object result = call(building, "getPosition");
        return result instanceof BlockPos pos ? pos : null;
    }

    private static Object call(Object target, String method) throws ReflectiveOperationException {
        if (target == null) return null;
        Method m = target.getClass().getMethod(method);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private record EffectConfig(Holder<MobEffect> effect, int duration, int maxLevel) {
    }
}
