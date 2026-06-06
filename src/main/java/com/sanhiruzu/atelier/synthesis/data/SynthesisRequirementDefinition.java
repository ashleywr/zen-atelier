package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;

public record SynthesisRequirementDefinition(
        ReagentQueryDefinition query,
        int amount
) {
    private static final Codec<SynthesisRequirementDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ReagentQueryDefinition.CODEC.fieldOf("query").forGetter(SynthesisRequirementDefinition::query),
            Codec.INT.fieldOf("amount").forGetter(SynthesisRequirementDefinition::amount)
    ).apply(instance, SynthesisRequirementDefinition::new));
    public static final Codec<SynthesisRequirementDefinition> CODEC = RAW_CODEC.flatXmap(SynthesisRequirementDefinition::validate, DataResult::success);

    private static DataResult<SynthesisRequirementDefinition> validate(SynthesisRequirementDefinition definition) {
        if (SynthesisDataValidation.positive("amount", definition.amount).error().isPresent()) {
            return SynthesisDataValidation.error("amount must be positive");
        }
        return DataResult.success(definition);
    }

    public SynthesisRequirement toCore() {
        return new SynthesisRequirement(query.toCore(), amount);
    }
}
