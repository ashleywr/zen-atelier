package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.ReagentShape;

import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

public record ReagentRollTemplate(
        String reagentId,
        IntRange amount,
        int tier,
        IntRange quality,
        IntRange purity,
        IntRange instability,
        Set<String> categories,
        Map<String, Integer> elements,
        java.util.List<String> traits,
        ReagentShape shape,
        Set<String> sourceHints
) {
    public ReagentRollTemplate(
            String reagentId,
            IntRange amount,
            int tier,
            IntRange quality,
            IntRange purity,
            IntRange instability,
            Map<String, Integer> elements,
            java.util.List<String> traits,
            Set<String> sourceHints
    ) {
        this(reagentId, amount, tier, quality, purity, instability, Set.of(), elements, traits, ReagentShape.SINGLE, sourceHints);
    }

    public ReagentRollTemplate {
        if (reagentId == null || reagentId.isBlank()) {
            throw new IllegalArgumentException("reagentId must not be blank");
        }
        if (amount == null || quality == null || purity == null || instability == null) {
            throw new IllegalArgumentException("ranges must not be null");
        }
        tier = Math.clamp(tier, 1, 6);
        categories = Set.copyOf(categories);
        elements = Map.copyOf(elements);
        traits = java.util.List.copyOf(traits);
        shape = shape == null ? ReagentShape.SINGLE : shape;
        sourceHints = Set.copyOf(sourceHints);
    }

    public static ReagentRollTemplate fixed(ReagentStack stack) {
        return new ReagentRollTemplate(
                stack.reagentId(),
                IntRange.fixed(stack.amount()),
                stack.tier(),
                IntRange.fixed(stack.quality()),
                IntRange.fixed(stack.purity()),
                IntRange.fixed(stack.instability()),
                stack.categories(),
                stack.elements(),
                stack.traits(),
                stack.shape(),
                stack.sourceHints()
        );
    }

    public ReagentStack roll(SplittableRandom random, int sourceAmount, int tierCap) {
        return new ReagentStack(
                reagentId,
                categories,
                amount.roll(random) * sourceAmount,
                Math.min(tier, tierCap),
                quality.roll(random),
                purity.roll(random),
                instability.roll(random),
                elements,
                traits,
                shape,
                sourceHints
        );
    }

    public record IntRange(int min, int max) {
        public IntRange {
            if (min < 0 || max < min) {
                throw new IllegalArgumentException("range must be non-negative and min <= max");
            }
        }

        public static IntRange fixed(int value) {
            return new IntRange(value, value);
        }

        public int roll(SplittableRandom random) {
            if (min == max) {
                return min;
            }
            return random.nextInt(min, max + 1);
        }
    }
}
