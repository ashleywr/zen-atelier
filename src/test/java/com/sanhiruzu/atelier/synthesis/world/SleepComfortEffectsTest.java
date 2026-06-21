package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.api.EnvironmentEffectContext;
import com.sanhiruzu.atelier.api.EnvironmentEffectRegistry;
import com.sanhiruzu.atelier.api.EnvironmentEffects;
import com.sanhiruzu.atelier.api.EnvironmentInfluenceResult;
import com.sanhiruzu.atelier.api.EnvironmentSnapshot;
import com.sanhiruzu.atelier.api.EnvironmentTemperatureBand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
        ));

        assertThat(reward.tier()).isEqualTo(3);
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
}
