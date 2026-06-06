package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record SynthesisProfileDefinition(
        int schema,
        ResourceLocation id,
        String category,
        int recipeTierCap,
        Optional<SynthesisBoardDefinition> board,
        List<SynthesisRequirementDefinition> requirements,
        List<SynthesisOutcomeDefinition> outcomes
) {
    private static final Codec<SynthesisProfileDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema", 1).forGetter(SynthesisProfileDefinition::schema),
            ResourceLocation.CODEC.fieldOf("id").forGetter(SynthesisProfileDefinition::id),
            Codec.STRING.optionalFieldOf("category", SynthesisRecipeCategory.MATERIALS).forGetter(SynthesisProfileDefinition::category),
            Codec.INT.fieldOf("recipe_tier_cap").forGetter(SynthesisProfileDefinition::recipeTierCap),
            SynthesisBoardDefinition.CODEC.optionalFieldOf("board").forGetter(SynthesisProfileDefinition::board),
            SynthesisRequirementDefinition.CODEC.listOf().fieldOf("requirements").forGetter(SynthesisProfileDefinition::requirements),
            SynthesisOutcomeDefinition.CODEC.listOf().fieldOf("outcomes").forGetter(SynthesisProfileDefinition::outcomes)
    ).apply(instance, SynthesisProfileDefinition::new));
    public static final Codec<SynthesisProfileDefinition> CODEC = RAW_CODEC.flatXmap(SynthesisProfileDefinition::validate, DataResult::success);

    public SynthesisProfileDefinition(
            int schema,
            ResourceLocation id,
            int recipeTierCap,
            List<SynthesisRequirementDefinition> requirements,
            List<SynthesisOutcomeDefinition> outcomes
    ) {
        this(schema, id, SynthesisRecipeCategory.MATERIALS, recipeTierCap, Optional.empty(), requirements, outcomes);
    }

    public SynthesisProfileDefinition(
            int schema,
            ResourceLocation id,
            String category,
            int recipeTierCap,
            List<SynthesisRequirementDefinition> requirements,
            List<SynthesisOutcomeDefinition> outcomes
    ) {
        this(schema, id, category, recipeTierCap, Optional.empty(), requirements, outcomes);
    }

    private static DataResult<SynthesisProfileDefinition> validate(SynthesisProfileDefinition definition) {
        if (SynthesisDataValidation.schema(definition.schema).error().isPresent()) {
            return SynthesisDataValidation.error("schema must be 1");
        }
        if (SynthesisDataValidation.tier("recipe_tier_cap", definition.recipeTierCap).error().isPresent()) {
            return SynthesisDataValidation.error("recipe_tier_cap must be between 1 and 6");
        }
        if (definition.requirements.isEmpty()) {
            return SynthesisDataValidation.error("requirements must not be empty");
        }
        if (definition.outcomes.isEmpty()) {
            return SynthesisDataValidation.error("outcomes must not be empty");
        }
        return DataResult.success(definition);
    }

    public SynthesisProfile toCore() {
        return new SynthesisProfile(
                id.toString(),
                SynthesisRecipeCategory.normalize(category),
                requirements.stream().map(SynthesisRequirementDefinition::toCore).toList(),
                recipeTierCap,
                board.map(SynthesisBoardDefinition::toCore).orElse(com.sanhiruzu.atelier.synthesis.engine.SynthesisBoard.CRUDE_3X3),
                outcomes.stream().map(SynthesisOutcomeDefinition::toCore).toList()
        );
    }
}
