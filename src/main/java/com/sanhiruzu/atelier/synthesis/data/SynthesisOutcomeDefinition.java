package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutcome;

import java.util.List;

public record SynthesisOutcomeDefinition(
        OutcomeClass outcomeClass,
        int weight,
        List<SynthesisOutputDefinition> outputs,
        List<ReagentStackDefinition> byproducts
) {
    private static final Codec<SynthesisOutcomeDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SynthesisCodecs.OUTCOME_CLASS.fieldOf("outcome").forGetter(SynthesisOutcomeDefinition::outcomeClass),
            Codec.INT.fieldOf("weight").forGetter(SynthesisOutcomeDefinition::weight),
            SynthesisOutputDefinition.CODEC.listOf().optionalFieldOf("outputs", List.of()).forGetter(SynthesisOutcomeDefinition::outputs),
            ReagentStackDefinition.CODEC.listOf().optionalFieldOf("byproducts", List.of()).forGetter(SynthesisOutcomeDefinition::byproducts)
    ).apply(instance, SynthesisOutcomeDefinition::new));
    public static final Codec<SynthesisOutcomeDefinition> CODEC = RAW_CODEC.flatXmap(SynthesisOutcomeDefinition::validate, DataResult::success);

    private static DataResult<SynthesisOutcomeDefinition> validate(SynthesisOutcomeDefinition definition) {
        if (SynthesisDataValidation.positive("weight", definition.weight).error().isPresent()) {
            return SynthesisDataValidation.error("weight must be positive");
        }
        return DataResult.success(definition);
    }

    public SynthesisOutcome toCore() {
        return new SynthesisOutcome(
                outcomeClass,
                weight,
                outputs.stream().map(SynthesisOutputDefinition::toCore).toList(),
                byproducts.stream().map(ReagentStackDefinition::toCore).toList()
        );
    }
}
