package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.api.EnvironmentEffectContext;
import com.sanhiruzu.atelier.api.EnvironmentEffectRegistry;
import com.sanhiruzu.atelier.api.EnvironmentEffects;
import com.sanhiruzu.atelier.api.EnvironmentInfluenceResult;
import com.sanhiruzu.atelier.api.EnvironmentSnapshot;
import com.sanhiruzu.atelier.api.EnvironmentTemperatureBand;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class SleepComfortEffectsTest {
    @Test
    void builtInSleepRulesRewardGoodBedroomSignalsThroughEnvironmentApi() {
        EnvironmentEffectRegistry registry = new EnvironmentEffectRegistry();
        registry.register(SleepComfortEffects::registerEnvironmentRules);
        EnvironmentSnapshot bedroom = new EnvironmentSnapshot(
                Map.of(
                        "bed", 1,
                        "wool", 2,
                        "carpet", 2,
                        "bookshelf", 2,
                        "flower_pots", 1,
                        "candle", 2,
                        "plant", 3
                ),
                true,
                true,
                true,
                EnvironmentTemperatureBand.PLEASANT,
                Set.of(),
                Map.of()
        );

        var effects = registry.evaluate(EnvironmentEffectContext.action(SleepComfortEffects.SLEEP_ACTION), bedroom);

        assertThat(effects).contains(
                EnvironmentEffects.modifyComfort("sleep_shelter_covered", 0.20f),
                EnvironmentEffects.modifyComfort("sleep_bedding_soft", 0.20f),
                EnvironmentEffects.modifyComfort("sleep_decor_restful", 0.20f),
                EnvironmentEffects.modifyComfort("sleep_lighting_pleasant", 0.10f),
                EnvironmentEffects.modifyComfort("sleep_nature_plants", 0.10f)
        );
    }

    @Test
    void normalizesComfortSoGoodBedroomsGrantMildTier() {
        SleepComfortEffects.SleepReward reward = SleepComfortEffects.rewardFor(List.of(
                EnvironmentEffects.modifyComfort("sleep_shelter_covered", 0.20f),
                EnvironmentEffects.modifyComfort("sleep_bedding_soft", 0.20f),
                EnvironmentEffects.modifyComfort("sleep_decor_restful", 0.20f),
                EnvironmentEffects.modifyComfort("sleep_lighting_pleasant", 0.10f),
                EnvironmentEffects.modifyComfort("sleep_nature_plants", 0.10f)
        ));

        assertThat(reward.tier()).isEqualTo(1);
        assertThat(reward.durationTicks()).isEqualTo(24000);
    }

    @Test
    void repeatedComfortSourcesHaveDiminishingReturnsBeforeTopTier() {
        EnvironmentInfluenceResult result = SleepComfortEffects.comfortResultFor(List.of(
                EnvironmentEffects.modifyComfort("sleep_bedding_wool", 0.50f),
                EnvironmentEffects.modifyComfort("sleep_bedding_carpet", 0.50f),
                EnvironmentEffects.modifyComfort("sleep_bedding_blanket", 0.50f),
                EnvironmentEffects.modifyComfort("sleep_lighting_pleasant", 0.20f)
        ));

        assertThat(result.tier()).isEqualTo(1);
        assertThat(result.category("bedding").capped()).isTrue();
        assertThat(result.category("bedding").diminished()).isTrue();
    }

    @Test
    void variedBedroomCategoriesOutperformRepeatedSingleCategoryComfort() {
        EnvironmentInfluenceResult result = SleepComfortEffects.comfortResultFor(List.of(
                EnvironmentEffects.modifyComfort("sleep_shelter_covered", 0.35f),
                EnvironmentEffects.modifyComfort("sleep_bedding_soft", 0.35f),
                EnvironmentEffects.modifyComfort("sleep_decor_restful", 0.35f)
        ));

        assertThat(result.tier()).isEqualTo(2);
        assertThat(result.activeCategoryCount()).isEqualTo(3);
    }

    @Test
    void exceptionalComfortCanReachTopTier() {
        SleepComfortEffects.SleepReward reward = SleepComfortEffects.rewardFor(List.of(
                EnvironmentEffects.modifyComfort("sleep_shelter_covered", 0.35f),
                EnvironmentEffects.modifyComfort("sleep_bedding_soft", 0.35f),
                EnvironmentEffects.modifyComfort("sleep_decor_restful", 0.35f),
                EnvironmentEffects.modifyComfort("sleep_lighting_pleasant", 0.25f),
                EnvironmentEffects.modifyComfort("sleep_nature_plants", 0.25f)
        ), new UUID(1L, 2L), 42L, new BlockPos(10, 64, -5), 2);

        assertThat(reward.tier()).isEqualTo(3);
    }

    @Test
    void exceptionalComfortStartsAtMildReward() {
        SleepComfortEffects.SleepReward reward = SleepComfortEffects.rewardFor(exceptionalComfortEffects());

        assertThat(reward.tier()).isEqualTo(1);
        assertThat(reward.boons()).hasSize(1);
    }

    @Test
    void sleepComfortBuildsOneTierAtATimeTowardTarget() {
        assertThat(SleepComfortEffects.nextRestedTier(0, 3)).isEqualTo(1);
        assertThat(SleepComfortEffects.nextRestedTier(1, 3)).isEqualTo(2);
        assertThat(SleepComfortEffects.nextRestedTier(2, 3)).isEqualTo(3);
    }

    @Test
    void sleepComfortOnlyBuildsOncePerDay() {
        assertThat(SleepComfortEffects.nextRestedTier(1, 3, 42L, 42L)).isEqualTo(1);
        assertThat(SleepComfortEffects.nextRestedTier(1, 3, 43L, 42L)).isEqualTo(2);
    }

    @Test
    void nearbyBedMovesKeepBelonging() {
        BlockPos originalBed = new BlockPos(10, 64, -5);

        assertThat(SleepComfortEffects.isSameBelongingBed(originalBed, new BlockPos(14, 66, -1))).isTrue();
        assertThat(SleepComfortEffects.isSameBelongingBed(originalBed, new BlockPos(15, 64, -5))).isFalse();
        assertThat(SleepComfortEffects.isSameBelongingBed(originalBed, new BlockPos(10, 67, -5))).isFalse();
    }

    @Test
    void sleepComfortDropsToLowerCurrentTarget() {
        assertThat(SleepComfortEffects.nextRestedTier(3, 1)).isEqualTo(1);
        assertThat(SleepComfortEffects.nextRestedTier(2, 0)).isZero();
    }

    @Test
    void dailyBoonsAreStableForSamePlayerDayAndBed() {
        var effects = exceptionalComfortEffects();
        UUID playerId = new UUID(1L, 2L);
        BlockPos bedPos = new BlockPos(10, 64, -5);

        SleepComfortEffects.SleepReward first = SleepComfortEffects.rewardFor(effects, playerId, 42L, bedPos, 2);
        SleepComfortEffects.SleepReward second = SleepComfortEffects.rewardFor(effects, playerId, 42L, bedPos, 2);

        assertThat(first.boons()).isEqualTo(second.boons());
        assertThat(first.boons()).hasSize(3);
    }

    @Test
    void dailyBoonsVaryAcrossDays() {
        var effects = exceptionalComfortEffects();
        UUID playerId = new UUID(1L, 2L);
        BlockPos bedPos = new BlockPos(10, 64, -5);

        Set<List<SleepComfortEffects.SleepBoon>> rolledBoons = LongStream.range(0, 20)
                .mapToObj(day -> SleepComfortEffects.rewardFor(effects, playerId, day, bedPos, 2).boons())
                .collect(Collectors.toSet());

        assertThat(rolledBoons).hasSizeGreaterThan(1);
    }

    @Test
    void rewardTierControlsBoonCount() {
        UUID playerId = new UUID(1L, 2L);
        BlockPos bedPos = new BlockPos(10, 64, -5);

        SleepComfortEffects.SleepReward tierOne = SleepComfortEffects.rewardFor(List.of(
                EnvironmentEffects.modifyComfort("sleep_shelter_covered", 0.20f),
                EnvironmentEffects.modifyComfort("sleep_bedding_soft", 0.20f),
                EnvironmentEffects.modifyComfort("sleep_decor_restful", 0.20f)
        ), playerId, 42L, bedPos);
        SleepComfortEffects.SleepReward tierTwo = SleepComfortEffects.rewardFor(List.of(
                EnvironmentEffects.modifyComfort("sleep_shelter_covered", 0.35f),
                EnvironmentEffects.modifyComfort("sleep_bedding_soft", 0.35f),
                EnvironmentEffects.modifyComfort("sleep_decor_restful", 0.35f)
        ), playerId, 42L, bedPos, 1);
        SleepComfortEffects.SleepReward tierThree = SleepComfortEffects.rewardFor(exceptionalComfortEffects(), playerId, 42L, bedPos, 2);

        assertThat(tierOne.boons()).hasSize(1);
        assertThat(tierTwo.boons()).hasSize(2);
        assertThat(tierThree.boons()).hasSize(3);
    }

    @Test
    void lowComfortDoesNotGrantReward() {
        SleepComfortEffects.SleepReward reward = SleepComfortEffects.rewardFor(List.of(
                EnvironmentEffects.modifyComfort("single_signal", 0.10f)
        ));

        assertThat(reward.tier()).isZero();
        assertThat(reward.durationTicks()).isZero();
    }

    @Test
    void sleepRewardsRequireRestingLongEnough() {
        assertThat(SleepComfortEffects.restedLongEnough(1_000L, 1_099L)).isFalse();
        assertThat(SleepComfortEffects.restedLongEnough(1_000L, 1_100L)).isTrue();
    }

    private static List<com.sanhiruzu.atelier.api.EnvironmentEffect> exceptionalComfortEffects() {
        return List.of(
                EnvironmentEffects.modifyComfort("sleep_shelter_covered", 0.35f),
                EnvironmentEffects.modifyComfort("sleep_bedding_soft", 0.35f),
                EnvironmentEffects.modifyComfort("sleep_decor_restful", 0.35f),
                EnvironmentEffects.modifyComfort("sleep_lighting_pleasant", 0.25f),
                EnvironmentEffects.modifyComfort("sleep_nature_plants", 0.25f)
        );
    }
}
