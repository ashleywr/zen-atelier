package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.api.EnvironmentConditions;
import com.sanhiruzu.atelier.api.EnvironmentEffect;
import com.sanhiruzu.atelier.api.EnvironmentEffectContext;
import com.sanhiruzu.atelier.api.EnvironmentEffectRegistrar;
import com.sanhiruzu.atelier.api.EnvironmentEffects;
import com.sanhiruzu.atelier.api.EnvironmentEffectType;
import com.sanhiruzu.atelier.api.EnvironmentInfluenceCategory;
import com.sanhiruzu.atelier.api.EnvironmentInfluenceProfile;
import com.sanhiruzu.atelier.api.EnvironmentInfluenceResolver;
import com.sanhiruzu.atelier.api.EnvironmentInfluenceResult;
import com.sanhiruzu.atelier.api.ZoneAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import java.util.List;

public final class SleepComfortEffects {
    public static final String SLEEP_ACTION = "zen_atelier:sleep";
    public static final String WELL_RESTED_TIER_KEY = "zen_atelier.well_rested_tier";
    public static final String WELL_RESTED_UNTIL_KEY = "zen_atelier.well_rested_until";
    private static final String SLEEP_BED_X_KEY = "zen_atelier.sleep_bed_x";
    private static final String SLEEP_BED_Y_KEY = "zen_atelier.sleep_bed_y";
    private static final String SLEEP_BED_Z_KEY = "zen_atelier.sleep_bed_z";
    private static final String SLEEP_STARTED_AT_KEY = "zen_atelier.sleep_started_at";
    private static final int REWARD_DURATION_TICKS = 24000;
    private static final int MIN_REST_TICKS = 100;
    private static final int ENVIRONMENT_RADIUS = 6;
    private static final EnvironmentInfluenceProfile SLEEP_COMFORT_PROFILE = new EnvironmentInfluenceProfile(
            EnvironmentEffectType.COMFORT_MODIFIER,
            List.of(
                    new EnvironmentInfluenceCategory("shelter", "environment_influence.zen_atelier.sleep.shelter", 0.35F, List.of("sleep_shelter")),
                    new EnvironmentInfluenceCategory("bedding", "environment_influence.zen_atelier.sleep.bedding", 0.35F, List.of("sleep_bedding")),
                    new EnvironmentInfluenceCategory("decor", "environment_influence.zen_atelier.sleep.decor", 0.35F, List.of("sleep_decor")),
                    new EnvironmentInfluenceCategory("lighting", "environment_influence.zen_atelier.sleep.lighting", 0.25F, List.of("sleep_lighting")),
                    new EnvironmentInfluenceCategory("nature", "environment_influence.zen_atelier.sleep.nature", 0.25F, List.of("sleep_nature"))
            ),
            new EnvironmentInfluenceCategory("other", "environment_influence.zen_atelier.sleep.other", 0.20F, List.of()),
            0.05F,
            List.of(0.45F, 1.10F, 1.45F)
    );
    private static boolean registeredEnvironmentRules;

    private SleepComfortEffects() {
    }

    public static void initialize() {
        if (registeredEnvironmentRules) {
            return;
        }
        registeredEnvironmentRules = true;
        ZoneAPI.registerEnvironmentEffects(SleepComfortEffects::registerEnvironmentRules);
    }

    public static void registerEnvironmentRules(EnvironmentEffectRegistrar registrar) {
        registrar.forAction(SLEEP_ACTION)
                .when(EnvironmentConditions.allOf(
                        EnvironmentConditions.covered(),
                        EnvironmentConditions.indoorsLike(),
                        EnvironmentConditions.signalAtLeast("bed", 1)
                ))
                .then(EnvironmentEffects.modifyComfort("sleep_shelter_covered", 0.20f));
        registrar.forAction(SLEEP_ACTION)
                .when(EnvironmentConditions.anyOf(
                        EnvironmentConditions.signalAtLeast("wool", 2),
                        EnvironmentConditions.signalAtLeast("carpet", 2)
                ))
                .then(EnvironmentEffects.modifyComfort("sleep_bedding_soft", 0.20f));
        registrar.forAction(SLEEP_ACTION)
                .when(EnvironmentConditions.anyOf(
                        EnvironmentConditions.signalAtLeast("bookshelf", 2),
                        EnvironmentConditions.signalAtLeast("flower_pots", 1)
                ))
                .then(EnvironmentEffects.modifyComfort("sleep_decor_restful", 0.20f));
        registrar.forAction(SLEEP_ACTION)
                .when(EnvironmentConditions.signalAtLeast("candle", 1))
                .then(EnvironmentEffects.modifyComfort("sleep_lighting_pleasant", 0.10f));
        registrar.forAction(SLEEP_ACTION)
                .when(EnvironmentConditions.signalAtLeast("plant", 2))
                .then(EnvironmentEffects.modifyComfort("sleep_nature_plants", 0.10f));
    }

    public static SleepReward rewardFor(List<EnvironmentEffect> effects) {
        int tier = comfortResultFor(effects).tier();
        return tier <= 0 ? SleepReward.NONE : new SleepReward(tier, REWARD_DURATION_TICKS);
    }

    public static EnvironmentInfluenceResult comfortResultFor(List<EnvironmentEffect> effects) {
        return EnvironmentInfluenceResolver.resolve(effects, SLEEP_COMFORT_PROFILE);
    }

    static boolean restedLongEnough(long startedAt, long wokeAt) {
        return wokeAt - startedAt >= MIN_REST_TICKS;
    }

    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        if (event.getProblem() != null) {
            return;
        }
        storeSleepBed(event.getEntity(), event.getPos(), event.getEntity().level().getGameTime());
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos bedPos = consumeSleepBed(player);
        if (bedPos == null) {
            return;
        }
        long startedAt = consumeSleepStartedAt(player);
        if (!restedLongEnough(startedAt, level.getGameTime())) {
            return;
        }
        List<EnvironmentEffect> effects = ZoneAPI.evaluateEnvironmentEffects(
                EnvironmentEffectContext.action(SLEEP_ACTION),
                level,
                bedPos,
                ENVIRONMENT_RADIUS
        );
        SleepReward reward = rewardFor(effects);
        if (reward.tier() <= 0) {
            return;
        }
        applyReward(player, reward, level.getGameTime());
    }

    private static void applyReward(ServerPlayer player, SleepReward reward, long gameTime) {
        CompoundTag data = player.getPersistentData();
        data.putInt(WELL_RESTED_TIER_KEY, reward.tier());
        data.putLong(WELL_RESTED_UNTIL_KEY, gameTime + reward.durationTicks());
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, reward.durationTicks(), 0));
        if (reward.tier() >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, reward.durationTicks(), 0));
        }
        if (reward.tier() >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 0));
        }
        player.displayClientMessage(Component.translatable("message.zen_atelier.sleep_comfort.reward", reward.tier()), true);
    }

    private static void storeSleepBed(ServerPlayer player, BlockPos pos, long gameTime) {
        CompoundTag data = player.getPersistentData();
        data.putInt(SLEEP_BED_X_KEY, pos.getX());
        data.putInt(SLEEP_BED_Y_KEY, pos.getY());
        data.putInt(SLEEP_BED_Z_KEY, pos.getZ());
        data.putLong(SLEEP_STARTED_AT_KEY, gameTime);
    }

    private static BlockPos consumeSleepBed(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(SLEEP_BED_X_KEY) || !data.contains(SLEEP_BED_Y_KEY) || !data.contains(SLEEP_BED_Z_KEY)) {
            return null;
        }
        BlockPos pos = new BlockPos(
                data.getInt(SLEEP_BED_X_KEY),
                data.getInt(SLEEP_BED_Y_KEY),
                data.getInt(SLEEP_BED_Z_KEY)
        );
        data.remove(SLEEP_BED_X_KEY);
        data.remove(SLEEP_BED_Y_KEY);
        data.remove(SLEEP_BED_Z_KEY);
        return pos;
    }

    private static long consumeSleepStartedAt(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        long startedAt = data.getLong(SLEEP_STARTED_AT_KEY);
        data.remove(SLEEP_STARTED_AT_KEY);
        return startedAt;
    }

    public record SleepReward(int tier, int durationTicks) {
        public static final SleepReward NONE = new SleepReward(0, 0);
    }
}
