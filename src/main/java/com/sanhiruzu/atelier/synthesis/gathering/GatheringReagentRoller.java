package com.sanhiruzu.atelier.synthesis.gathering;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileDefinition;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileRegistry;
import com.sanhiruzu.atelier.synthesis.data.ReagentStackDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

public final class GatheringReagentRoller {
    private GatheringReagentRoller() {
    }

    public static ReagentStack roll(Level level, BlockPos pos, RandomSource random) {
        List<WeightedRoll> rolls = availableRolls();
        if (rolls.isEmpty()) {
            return fallback(random);
        }

        int totalWeight = rolls.stream().mapToInt(WeightedRoll::weight).sum();
        int selected = random.nextInt(totalWeight);
        for (WeightedRoll roll : rolls) {
            selected -= roll.weight();
            if (selected < 0) {
                long seed = level.getGameTime()
                        ^ pos.asLong()
                        ^ random.nextLong()
                        ^ ZenAtelier.MODID.hashCode();
                return roll.definition().toRollTemplate().roll(new SplittableRandom(seed), 1, roll.tierCap());
            }
        }
        return fallback(random);
    }

    private static List<WeightedRoll> availableRolls() {
        List<WeightedRoll> rolls = new ArrayList<>();
        for (ExtractionProfileDefinition profile : ExtractionProfileRegistry.all()) {
            for (var outcome : profile.outcomes()) {
                int weight = Math.max(1, outcome.weight());
                for (ReagentStackDefinition reagent : outcome.reagents()) {
                    rolls.add(new WeightedRoll(reagent, weight, profile.sourceTierCap()));
                }
                for (ReagentStackDefinition reagent : outcome.byproducts()) {
                    rolls.add(new WeightedRoll(reagent, Math.max(1, weight / 3), profile.sourceTierCap()));
                }
            }
        }
        return rolls;
    }

    private static ReagentStack fallback(RandomSource random) {
        return new ReagentStack(
                "zen_atelier:organic_reagent",
                Set.of("zen_atelier:organic"),
                random.nextIntBetweenInclusive(12, 24),
                1,
                random.nextIntBetweenInclusive(25, 55),
                random.nextIntBetweenInclusive(35, 70),
                random.nextIntBetweenInclusive(5, 35),
                Map.of("earth", 1),
                List.of("zen_atelier:fresh"),
                com.sanhiruzu.atelier.synthesis.core.ReagentShape.SINGLE,
                Set.of("zen_atelier:gathering_point")
        );
    }

    private record WeightedRoll(ReagentStackDefinition definition, int weight, int tierCap) {
    }
}
