package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record ReagentQueryDefinition(
        List<ResourceLocation> reagents,
        int minTier,
        int maxTier,
        int minQuality,
        int minPurity,
        int maxInstability,
        List<ResourceLocation> requiredCategories,
        Map<String, Integer> minElements,
        List<ResourceLocation> requiredTraits,
        List<ResourceLocation> requiredSourceHints
) {
    public ReagentQueryDefinition(
            List<ResourceLocation> reagents,
            int minTier,
            int maxTier,
            int minQuality,
            int minPurity,
            int maxInstability,
            Map<String, Integer> minElements,
            List<ResourceLocation> requiredTraits,
            List<ResourceLocation> requiredSourceHints
    ) {
        this(
                reagents,
                minTier,
                maxTier,
                minQuality,
                minPurity,
                maxInstability,
                List.of(),
                minElements,
                requiredTraits,
                requiredSourceHints
        );
    }

    private static final Codec<ReagentQueryDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("reagents", List.of()).forGetter(ReagentQueryDefinition::reagents),
            Codec.INT.optionalFieldOf("min_tier", 1).forGetter(ReagentQueryDefinition::minTier),
            Codec.INT.optionalFieldOf("max_tier", 6).forGetter(ReagentQueryDefinition::maxTier),
            Codec.INT.optionalFieldOf("min_quality", 0).forGetter(ReagentQueryDefinition::minQuality),
            Codec.INT.optionalFieldOf("min_purity", 0).forGetter(ReagentQueryDefinition::minPurity),
            Codec.INT.optionalFieldOf("max_instability", 100).forGetter(ReagentQueryDefinition::maxInstability),
            ResourceLocation.CODEC.listOf().optionalFieldOf("required_categories", List.of()).forGetter(ReagentQueryDefinition::requiredCategories),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("min_elements", Map.of()).forGetter(ReagentQueryDefinition::minElements),
            ResourceLocation.CODEC.listOf().optionalFieldOf("required_traits", List.of()).forGetter(ReagentQueryDefinition::requiredTraits),
            ResourceLocation.CODEC.listOf().optionalFieldOf("required_source_hints", List.of()).forGetter(ReagentQueryDefinition::requiredSourceHints)
    ).apply(instance, ReagentQueryDefinition::new));
    public static final Codec<ReagentQueryDefinition> CODEC = RAW_CODEC.flatXmap(ReagentQueryDefinition::validate, DataResult::success);

    private static DataResult<ReagentQueryDefinition> validate(ReagentQueryDefinition definition) {
        if (SynthesisDataValidation.tier("min_tier", definition.minTier).error().isPresent()) {
            return SynthesisDataValidation.error("min_tier must be between 1 and 6");
        }
        if (SynthesisDataValidation.tier("max_tier", definition.maxTier).error().isPresent()) {
            return SynthesisDataValidation.error("max_tier must be between 1 and 6");
        }
        if (definition.minTier > definition.maxTier) {
            return SynthesisDataValidation.error("min_tier must not exceed max_tier");
        }
        if (SynthesisDataValidation.percent("min_quality", definition.minQuality).error().isPresent()) {
            return SynthesisDataValidation.error("min_quality must be between 0 and 100");
        }
        if (SynthesisDataValidation.percent("min_purity", definition.minPurity).error().isPresent()) {
            return SynthesisDataValidation.error("min_purity must be between 0 and 100");
        }
        if (SynthesisDataValidation.percent("max_instability", definition.maxInstability).error().isPresent()) {
            return SynthesisDataValidation.error("max_instability must be between 0 and 100");
        }
        if (SynthesisDataValidation.positiveElementValues("min_elements", definition.minElements).error().isPresent()) {
            return SynthesisDataValidation.error("min_elements values must be positive and keys must not be blank");
        }
        return DataResult.success(definition);
    }

    public ReagentQuery toCore() {
        return new ReagentQuery(
                toStringSet(reagents),
                minTier,
                maxTier,
                minQuality,
                minPurity,
                maxInstability,
                toStringSet(requiredCategories),
                minElements,
                toStringSet(requiredTraits),
                toStringSet(requiredSourceHints)
        );
    }

    private static Set<String> toStringSet(List<ResourceLocation> ids) {
        return ids.stream().map(ResourceLocation::toString).collect(Collectors.toUnmodifiableSet());
    }
}
