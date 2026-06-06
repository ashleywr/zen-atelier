package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionProfile;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ExtractionProfileDefinition(
        int schema,
        ResourceLocation id,
        String source,
        int sourceTierCap,
        List<ExtractionOutcomeDefinition> outcomes
) {
    private static final Codec<String> SOURCE_CODEC = Codec.STRING.comapFlatMap(value -> {
        if (value.isBlank()) {
            return DataResult.error(() -> "source must not be blank");
        }
        if (value.startsWith("#")) {
            return ResourceLocation.read(value.substring(1)).map(ignored -> value);
        }
        return ResourceLocation.read(value).map(ignored -> value);
    }, value -> value);

    private static final Codec<ExtractionProfileDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema", 1).forGetter(ExtractionProfileDefinition::schema),
            ResourceLocation.CODEC.fieldOf("id").forGetter(ExtractionProfileDefinition::id),
            SOURCE_CODEC.fieldOf("source").forGetter(ExtractionProfileDefinition::source),
            Codec.INT.fieldOf("source_tier_cap").forGetter(ExtractionProfileDefinition::sourceTierCap),
            ExtractionOutcomeDefinition.CODEC.listOf().fieldOf("outcomes").forGetter(ExtractionProfileDefinition::outcomes)
    ).apply(instance, ExtractionProfileDefinition::new));
    public static final Codec<ExtractionProfileDefinition> CODEC = RAW_CODEC.flatXmap(ExtractionProfileDefinition::validate, DataResult::success);

    private static DataResult<ExtractionProfileDefinition> validate(ExtractionProfileDefinition definition) {
        if (SynthesisDataValidation.schema(definition.schema).error().isPresent()) {
            return SynthesisDataValidation.error("schema must be 1");
        }
        if (SynthesisDataValidation.tier("source_tier_cap", definition.sourceTierCap).error().isPresent()) {
            return SynthesisDataValidation.error("source_tier_cap must be between 1 and 6");
        }
        if (definition.outcomes.isEmpty()) {
            return SynthesisDataValidation.error("outcomes must not be empty");
        }
        return DataResult.success(definition);
    }

    public ExtractionProfile toCore() {
        return new ExtractionProfile(
                id.toString(),
                source,
                sourceTierCap,
                outcomes.stream().map(ExtractionOutcomeDefinition::toCore).toList()
        );
    }
}
