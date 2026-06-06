package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.engine.ReagentRollTemplate;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ReagentStackDefinition(
        ResourceLocation reagent,
        int amount,
        int tier,
        int quality,
        int purity,
        int instability,
        Optional<IntRangeDefinition> amountRange,
        Optional<IntRangeDefinition> qualityRange,
        Optional<IntRangeDefinition> purityRange,
        Optional<IntRangeDefinition> instabilityRange,
        List<ResourceLocation> categories,
        Map<String, Integer> elements,
        List<ResourceLocation> traits,
        ReagentShape shape,
        List<ResourceLocation> sourceHints
) {
    public ReagentStackDefinition(
            ResourceLocation reagent,
            int amount,
            int tier,
            int quality,
            int purity,
            int instability,
            Map<String, Integer> elements,
            List<ResourceLocation> traits,
            List<ResourceLocation> sourceHints
    ) {
        this(
                reagent,
                amount,
                tier,
                quality,
                purity,
                instability,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                elements,
                traits,
                ReagentShape.SINGLE,
                sourceHints
        );
    }

    private static final Codec<ReagentStackDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("reagent").forGetter(ReagentStackDefinition::reagent),
            Codec.INT.fieldOf("amount").forGetter(ReagentStackDefinition::amount),
            Codec.INT.fieldOf("tier").forGetter(ReagentStackDefinition::tier),
            Codec.INT.optionalFieldOf("quality", 0).forGetter(ReagentStackDefinition::quality),
            Codec.INT.optionalFieldOf("purity", 0).forGetter(ReagentStackDefinition::purity),
            Codec.INT.optionalFieldOf("instability", 0).forGetter(ReagentStackDefinition::instability),
            IntRangeDefinition.CODEC.optionalFieldOf("amount_range").forGetter(ReagentStackDefinition::amountRange),
            IntRangeDefinition.CODEC.optionalFieldOf("quality_range").forGetter(ReagentStackDefinition::qualityRange),
            IntRangeDefinition.CODEC.optionalFieldOf("purity_range").forGetter(ReagentStackDefinition::purityRange),
            IntRangeDefinition.CODEC.optionalFieldOf("instability_range").forGetter(ReagentStackDefinition::instabilityRange),
            ResourceLocation.CODEC.listOf().optionalFieldOf("categories", List.of()).forGetter(ReagentStackDefinition::categories),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("elements", Map.of()).forGetter(ReagentStackDefinition::elements),
            ResourceLocation.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(ReagentStackDefinition::traits),
            ReagentShape.CODEC.optionalFieldOf("shape", ReagentShape.SINGLE).forGetter(ReagentStackDefinition::shape),
            ResourceLocation.CODEC.listOf().optionalFieldOf("source_hints", List.of()).forGetter(ReagentStackDefinition::sourceHints)
    ).apply(instance, ReagentStackDefinition::new));
    public static final Codec<ReagentStackDefinition> CODEC = RAW_CODEC.flatXmap(ReagentStackDefinition::validate, DataResult::success);

    private static DataResult<ReagentStackDefinition> validate(ReagentStackDefinition definition) {
        if (SynthesisDataValidation.positive("amount", definition.amount).error().isPresent()) {
            return SynthesisDataValidation.error("amount must be positive");
        }
        if (SynthesisDataValidation.tier("tier", definition.tier).error().isPresent()) {
            return SynthesisDataValidation.error("tier must be between 1 and 6");
        }
        if (SynthesisDataValidation.percent("quality", definition.quality).error().isPresent()) {
            return SynthesisDataValidation.error("quality must be between 0 and 100");
        }
        if (SynthesisDataValidation.percent("purity", definition.purity).error().isPresent()) {
            return SynthesisDataValidation.error("purity must be between 0 and 100");
        }
        if (SynthesisDataValidation.percent("instability", definition.instability).error().isPresent()) {
            return SynthesisDataValidation.error("instability must be between 0 and 100");
        }
        if (definition.amountRange.isPresent() && definition.amountRange.get().min() <= 0) {
            return SynthesisDataValidation.error("amount_range min must be positive");
        }
        if (definition.qualityRange.isPresent() && !percentRange(definition.qualityRange.get())) {
            return SynthesisDataValidation.error("quality_range must be between 0 and 100");
        }
        if (definition.purityRange.isPresent() && !percentRange(definition.purityRange.get())) {
            return SynthesisDataValidation.error("purity_range must be between 0 and 100");
        }
        if (definition.instabilityRange.isPresent() && !percentRange(definition.instabilityRange.get())) {
            return SynthesisDataValidation.error("instability_range must be between 0 and 100");
        }
        if (SynthesisDataValidation.positiveElementValues("elements", definition.elements).error().isPresent()) {
            return SynthesisDataValidation.error("elements values must be positive and keys must not be blank");
        }
        return DataResult.success(definition);
    }

    public ReagentStack toCore() {
        return new ReagentStack(
                reagent.toString(),
                categories.stream().map(ResourceLocation::toString).collect(java.util.stream.Collectors.toSet()),
                amount,
                tier,
                quality,
                purity,
                instability,
                elements,
                traits.stream().map(ResourceLocation::toString).toList(),
                shape,
                sourceHints.stream().map(ResourceLocation::toString).collect(java.util.stream.Collectors.toSet())
        );
    }

    public ReagentRollTemplate toRollTemplate() {
        return new ReagentRollTemplate(
                reagent.toString(),
                amountRange.map(IntRangeDefinition::toCore).orElse(ReagentRollTemplate.IntRange.fixed(amount)),
                tier,
                qualityRange.map(IntRangeDefinition::toCore).orElse(ReagentRollTemplate.IntRange.fixed(quality)),
                purityRange.map(IntRangeDefinition::toCore).orElse(ReagentRollTemplate.IntRange.fixed(purity)),
                instabilityRange.map(IntRangeDefinition::toCore).orElse(ReagentRollTemplate.IntRange.fixed(instability)),
                categories.stream().map(ResourceLocation::toString).collect(java.util.stream.Collectors.toSet()),
                elements,
                traits.stream().map(ResourceLocation::toString).toList(),
                shape,
                sourceHints.stream().map(ResourceLocation::toString).collect(java.util.stream.Collectors.toSet())
        );
    }

    private static boolean percentRange(IntRangeDefinition range) {
        return range.min() >= 0 && range.max() <= 100;
    }
}
