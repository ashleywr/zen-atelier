package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisBoard;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record SynthesisBoardNodeDefinition(
        int x,
        int y,
        String type,
        Optional<String> requiredElement,
        int requiredElementValue,
        Optional<ResourceLocation> morphTarget,
        int qualityBonus,
        int perfectBonus
) {
    private static final Codec<SynthesisBoardNodeDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(SynthesisBoardNodeDefinition::x),
            Codec.INT.fieldOf("y").forGetter(SynthesisBoardNodeDefinition::y),
            Codec.STRING.fieldOf("type").forGetter(SynthesisBoardNodeDefinition::type),
            Codec.STRING.optionalFieldOf("required_element").forGetter(SynthesisBoardNodeDefinition::requiredElement),
            Codec.INT.optionalFieldOf("required_element_value", 1).forGetter(SynthesisBoardNodeDefinition::requiredElementValue),
            ResourceLocation.CODEC.optionalFieldOf("morph_target").forGetter(SynthesisBoardNodeDefinition::morphTarget),
            Codec.INT.optionalFieldOf("quality_bonus", 0).forGetter(SynthesisBoardNodeDefinition::qualityBonus),
            Codec.INT.optionalFieldOf("perfect_bonus", 0).forGetter(SynthesisBoardNodeDefinition::perfectBonus)
    ).apply(instance, SynthesisBoardNodeDefinition::new));
    public static final Codec<SynthesisBoardNodeDefinition> CODEC = RAW_CODEC.flatXmap(SynthesisBoardNodeDefinition::validate, DataResult::success);

    private static DataResult<SynthesisBoardNodeDefinition> validate(SynthesisBoardNodeDefinition definition) {
        if (definition.x < 0 || definition.y < 0) {
            return SynthesisDataValidation.error("node coordinates must be non-negative");
        }
        if (definition.type == null || definition.type.isBlank()) {
            return SynthesisDataValidation.error("node type must not be blank");
        }
        if (definition.requiredElementValue < 0) {
            return SynthesisDataValidation.error("required_element_value must be non-negative");
        }
        if (definition.qualityBonus < 0 || definition.perfectBonus < 0) {
            return SynthesisDataValidation.error("node bonuses must be non-negative");
        }
        return DataResult.success(definition);
    }

    public SynthesisBoard.Node toCore() {
        return new SynthesisBoard.Node(
                x,
                y,
                type,
                requiredElement,
                requiredElementValue,
                morphTarget.map(ResourceLocation::toString),
                qualityBonus,
                perfectBonus
        );
    }
}
