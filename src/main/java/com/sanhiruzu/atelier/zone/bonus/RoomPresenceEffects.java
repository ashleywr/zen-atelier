package com.sanhiruzu.atelier.zone.bonus;

import com.sanhiruzu.atelier.space.zone.RoomData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;

public final class RoomPresenceEffects {
    private static final Map<String, EffectConfig> PROFILE_EFFECTS = new HashMap<>();

    static {
        // Profile ID -> effect with base duration and max level
        PROFILE_EFFECTS.put("zen_atelier:atelier", new EffectConfig(MobEffects.LUCK, 200, 2));
        PROFILE_EFFECTS.put("zen_atelier:enchanting_room", new EffectConfig(MobEffects.LUCK, 200, 2));
        PROFILE_EFFECTS.put("zen_atelier:smithy", new EffectConfig(MobEffects.DIG_SPEED, 200, 2));
        PROFILE_EFFECTS.put("zen_atelier:masonry", new EffectConfig(MobEffects.DIG_SPEED, 200, 1));
        PROFILE_EFFECTS.put("zen_atelier:workshop", new EffectConfig(MobEffects.DIG_SPEED, 200, 1));
        PROFILE_EFFECTS.put("zen_atelier:kitchen", new EffectConfig(MobEffects.SATURATION, 200, 1));
        PROFILE_EFFECTS.put("zen_atelier:greenhouse", new EffectConfig(MobEffects.SATURATION, 200, 1));
        PROFILE_EFFECTS.put("zen_atelier:gardener_shed", new EffectConfig(MobEffects.SATURATION, 200, 1));
        PROFILE_EFFECTS.put("zen_atelier:library", new EffectConfig(MobEffects.LUCK, 200, 1));
        PROFILE_EFFECTS.put("zen_atelier:loom_room", new EffectConfig(MobEffects.LUCK, 200, 1));
        PROFILE_EFFECTS.put("zen_atelier:map_room", new EffectConfig(MobEffects.MOVEMENT_SPEED, 200, 1));
        PROFILE_EFFECTS.put("zen_atelier:fletchery", new EffectConfig(MobEffects.DAMAGE_BOOST, 200, 1));
        PROFILE_EFFECTS.put("zen_atelier:tannery", new EffectConfig(MobEffects.DAMAGE_BOOST, 200, 1));
        PROFILE_EFFECTS.put("zen_atelier:church", new EffectConfig(MobEffects.REGENERATION, 200, 1));
    }

    private RoomPresenceEffects() {
    }

    public static void applyPresenceEffects(ServerPlayer player, RoomData room) {
        int qualityScore = Math.round(room.getQuality() * 100);
        if (qualityScore < 25) {
            return;
        }

        if (room.getZoneTypeId() == null) {
            return;
        }

        String profileId = room.getZoneTypeId().toString();
        EffectConfig config = PROFILE_EFFECTS.get(profileId);
        if (config == null) {
            return;
        }

        int duration = getDuration(qualityScore);
        int level = getLevel(qualityScore, config);

        player.addEffect(new MobEffectInstance(config.effect, duration, level, false, false));
    }

    private static int getDuration(int qualityScore) {
        if (qualityScore >= 50) {
            return 200;
        } else {
            return 120;
        }
    }

    private static int getLevel(int qualityScore, EffectConfig config) {
        if (qualityScore >= 75 && config.maxLevel > 0) {
            return config.maxLevel - 1;
        }
        return 0;
    }

    private static class EffectConfig {
        final Holder<MobEffect> effect;
        final int baseDuration;
        final int maxLevel;

        EffectConfig(Holder<MobEffect> effect, int baseDuration, int maxLevel) {
            this.effect = effect;
            this.baseDuration = baseDuration;
            this.maxLevel = maxLevel;
        }
    }
}
