package com.sanhiruzu.atelier.integration.minecolonies;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

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

    private static boolean initialized;

    // Profile id -> effect config for the colonist's work building room.
    private static final Map<String, EffectConfig> WORK_EFFECTS = new HashMap<>();

    // Profile id -> effect config for the colonist's home building room.
    private static final Map<String, EffectConfig> HOME_EFFECTS = new HashMap<>();

    static {
        // Merged into base Atelier profiles — hut blocks already satisfy the required signals.
        WORK_EFFECTS.put("zen_atelier:smithy",       new EffectConfig(MobEffects.DIG_SPEED,       400, 2));
        WORK_EFFECTS.put("zen_atelier:workshop",     new EffectConfig(MobEffects.DIG_SPEED,       400, 1));
        WORK_EFFECTS.put("zen_atelier:kitchen",      new EffectConfig(MobEffects.SATURATION,      400, 1));
        // MineColonies-only profiles with no base equivalent.
        WORK_EFFECTS.put("minecolonies:guard_post",  new EffectConfig(MobEffects.DAMAGE_BOOST,    400, 1));
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
        initialized = true;
    }

    static void tick(ServerLevel level, int tickCount) {
        if (!initialized || tickCount % 200 != 0) {
            return;
        }
        try {
            applyAll(level);
        } catch (LinkageError ex) {
            ZenAtelier.LOGGER.debug("ColonistRoomBonuses tick error", ex);
        }
    }

    private static void applyAll(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof AbstractEntityCitizen citizenEntity) || !(entity instanceof LivingEntity living)) {
                continue;
            }

            ICitizenData citizen = citizenEntity.getCitizenData();
            if (citizen == null) {
                continue;
            }

            applyWorkEffect(level, citizen, living);
            applyHomeEffect(level, citizen, living);
        }
    }

    private static void applyWorkEffect(ServerLevel level, ICitizenData citizen, LivingEntity entity) {
        // Room bonuses disabled pending the atmosphere substrate.
    }

    private static void applyHomeEffect(ServerLevel level, ICitizenData citizen, LivingEntity entity) {
        // Room bonuses disabled pending the atmosphere substrate.
    }

    private static int effectLevel(int quality, EffectConfig cfg) {
        return (quality >= 75 && cfg.maxLevel > 1) ? cfg.maxLevel - 1 : 0;
    }

    private record EffectConfig(Holder<MobEffect> effect, int duration, int maxLevel) {
    }
}
