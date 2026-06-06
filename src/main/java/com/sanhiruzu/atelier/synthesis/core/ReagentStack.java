package com.sanhiruzu.atelier.synthesis.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ReagentStack(
        String reagentId,
        Set<String> categories,
        int amount,
        int tier,
        int quality,
        int purity,
        int instability,
        Map<String, Integer> elements,
        List<String> traits,
        ReagentShape shape,
        Set<String> sourceHints
) {
    public static final Codec<ReagentStack> CODEC = Serialized.CODEC.flatXmap(Serialized::toStack, Serialized::fromStack);

    public ReagentStack(
            String reagentId,
            int amount,
            int tier,
            int quality,
            int purity,
            int instability,
            Map<String, Integer> elements,
            List<String> traits,
            Set<String> sourceHints
    ) {
        this(reagentId, Set.of(), amount, tier, quality, purity, instability, elements, traits, ReagentShape.SINGLE, sourceHints);
    }

    public ReagentStack(
            String reagentId,
            int amount,
            int tier,
            int quality,
            int purity,
            int instability,
            Map<String, Integer> elements,
            List<String> traits,
            ReagentShape shape,
            Set<String> sourceHints
    ) {
        this(reagentId, Set.of(), amount, tier, quality, purity, instability, elements, traits, shape, sourceHints);
    }

    public ReagentStack {
        if (reagentId == null || reagentId.isBlank()) {
            throw new IllegalArgumentException("reagentId must not be blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        categories = Set.copyOf(categories);
        if (categories.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("categories must not contain blanks");
        }
        tier = clamp(tier, 1, 6);
        quality = clamp(quality, 0, 100);
        purity = clamp(purity, 0, 100);
        instability = clamp(instability, 0, 100);
        elements = Map.copyOf(elements);
        traits = List.copyOf(traits);
        shape = shape == null ? ReagentShape.SINGLE : shape;
        sourceHints = Set.copyOf(sourceHints);
    }

    public static ReagentStack simple(String reagentId, int amount, int tier) {
        return new ReagentStack(reagentId, Set.of(), amount, tier, 0, 0, 0, Map.of(), List.of(), ReagentShape.SINGLE, Set.of());
    }

    public ReagentStack withAmount(int newAmount) {
        return new ReagentStack(reagentId, categories, newAmount, tier, quality, purity, instability, elements, traits, shape, sourceHints);
    }

    public ReagentStack cappedAtTier(int tierCap) {
        return new ReagentStack(reagentId, categories, amount, Math.min(tier, tierCap), quality, purity, instability, elements, traits, shape, sourceHints);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Serialized(
            String reagentId,
            List<String> categories,
            int amount,
            int tier,
            int quality,
            int purity,
            int instability,
            Map<String, Integer> elements,
            List<String> traits,
            ReagentShape shape,
            List<String> sourceHints
    ) {
        private static final Codec<Serialized> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("reagent").forGetter(Serialized::reagentId),
                Codec.STRING.listOf().optionalFieldOf("categories", List.of()).forGetter(Serialized::categories),
                Codec.INT.fieldOf("amount").forGetter(Serialized::amount),
                Codec.INT.fieldOf("tier").forGetter(Serialized::tier),
                Codec.INT.optionalFieldOf("quality", 0).forGetter(Serialized::quality),
                Codec.INT.optionalFieldOf("purity", 0).forGetter(Serialized::purity),
                Codec.INT.optionalFieldOf("instability", 0).forGetter(Serialized::instability),
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("elements", Map.of()).forGetter(Serialized::elements),
                Codec.STRING.listOf().optionalFieldOf("traits", List.of()).forGetter(Serialized::traits),
                ReagentShape.CODEC.optionalFieldOf("shape", ReagentShape.SINGLE).forGetter(Serialized::shape),
                Codec.STRING.listOf().optionalFieldOf("source_hints", List.of()).forGetter(Serialized::sourceHints)
        ).apply(instance, Serialized::new));

        private DataResult<ReagentStack> toStack() {
            if (reagentId == null || reagentId.isBlank()) {
                return DataResult.error(() -> "reagent must not be blank");
            }
            if (amount <= 0) {
                return DataResult.error(() -> "amount must be positive");
            }
            if (tier < 1 || tier > 6) {
                return DataResult.error(() -> "tier must be between 1 and 6");
            }
            if (!percent(quality) || !percent(purity) || !percent(instability)) {
                return DataResult.error(() -> "quality, purity, and instability must be between 0 and 100");
            }
            if (categories.stream().anyMatch(String::isBlank)) {
                return DataResult.error(() -> "categories must not contain blanks");
            }
            for (Map.Entry<String, Integer> entry : elements.entrySet()) {
                if (entry.getKey().isBlank()) {
                    return DataResult.error(() -> "element keys must not be blank");
                }
                if (entry.getValue() <= 0) {
                    return DataResult.error(() -> "element values must be positive");
                }
            }
            return DataResult.success(new ReagentStack(
                    reagentId,
                    Set.copyOf(categories),
                    amount,
                    tier,
                    quality,
                    purity,
                    instability,
                    elements,
                    traits,
                    shape,
                    Set.copyOf(sourceHints)
            ));
        }

        private static DataResult<Serialized> fromStack(ReagentStack stack) {
            return DataResult.success(new Serialized(
                    stack.reagentId(),
                    stack.categories().stream().sorted().toList(),
                    stack.amount(),
                    stack.tier(),
                    stack.quality(),
                    stack.purity(),
                    stack.instability(),
                    stack.elements(),
                    stack.traits(),
                    stack.shape(),
                    stack.sourceHints().stream().sorted().toList()
            ));
        }

        private static boolean percent(int value) {
            return value >= 0 && value <= 100;
        }
    }
}
