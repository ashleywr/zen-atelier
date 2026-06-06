package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionOutcome;

import java.util.List;

public record ExtractionOutcomeDefinition(
        OutcomeClass outcomeClass,
        int weight,
        List<ReagentStackDefinition> reagents,
        List<ReagentStackDefinition> byproducts
) {
    private static final Codec<ExtractionOutcomeDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SynthesisCodecs.OUTCOME_CLASS.fieldOf("outcome").forGetter(ExtractionOutcomeDefinition::outcomeClass),
            Codec.INT.fieldOf("weight").forGetter(ExtractionOutcomeDefinition::weight),
            ReagentStackDefinition.CODEC.listOf().optionalFieldOf("reagents", List.of()).forGetter(ExtractionOutcomeDefinition::reagents),
            ReagentStackDefinition.CODEC.listOf().optionalFieldOf("byproducts", List.of()).forGetter(ExtractionOutcomeDefinition::byproducts)
    ).apply(instance, ExtractionOutcomeDefinition::new));
    public static final Codec<ExtractionOutcomeDefinition> CODEC = RAW_CODEC.flatXmap(ExtractionOutcomeDefinition::validate, DataResult::success);

    private static DataResult<ExtractionOutcomeDefinition> validate(ExtractionOutcomeDefinition definition) {
        if (SynthesisDataValidation.positive("weight", definition.weight).error().isPresent()) {
            return SynthesisDataValidation.error("weight must be positive");
        }
        return DataResult.success(definition);
    }

    public ExtractionOutcome toCore() {
        return ExtractionOutcome.fromTemplates(
                outcomeClass,
                weight,
                reagents.stream().map(ReagentStackDefinition::toRollTemplate).toList(),
                byproducts.stream().map(ReagentStackDefinition::toRollTemplate).toList()
        );
    }
}
