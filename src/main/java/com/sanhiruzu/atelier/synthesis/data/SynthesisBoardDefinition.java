package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisBoard;

import java.util.List;

public record SynthesisBoardDefinition(
        int size,
        int emptyCellSuccessPenalty,
        int emptyCellPerfectPenalty,
        List<SynthesisBoardNodeDefinition> nodes
) {
    private static final Codec<SynthesisBoardDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("size", 3).forGetter(SynthesisBoardDefinition::size),
            Codec.INT.optionalFieldOf("empty_cell_success_penalty", 0).forGetter(SynthesisBoardDefinition::emptyCellSuccessPenalty),
            Codec.INT.optionalFieldOf("empty_cell_perfect_penalty", 0).forGetter(SynthesisBoardDefinition::emptyCellPerfectPenalty),
            SynthesisBoardNodeDefinition.CODEC.listOf().optionalFieldOf("nodes", List.of()).forGetter(SynthesisBoardDefinition::nodes)
    ).apply(instance, SynthesisBoardDefinition::new));
    public static final Codec<SynthesisBoardDefinition> CODEC = RAW_CODEC.flatXmap(SynthesisBoardDefinition::validate, DataResult::success);

    private static DataResult<SynthesisBoardDefinition> validate(SynthesisBoardDefinition definition) {
        if (definition.size < 3 || definition.size > 7) {
            return SynthesisDataValidation.error("board size must be between 3 and 7");
        }
        if (definition.emptyCellSuccessPenalty < 0 || definition.emptyCellPerfectPenalty < 0) {
            return SynthesisDataValidation.error("empty cell penalties must be non-negative");
        }
        for (SynthesisBoardNodeDefinition node : definition.nodes) {
            if (node.x() >= definition.size || node.y() >= definition.size) {
                return SynthesisDataValidation.error("board nodes must be inside board bounds");
            }
        }
        return DataResult.success(definition);
    }

    public SynthesisBoard toCore() {
        return new SynthesisBoard(
                size,
                emptyCellSuccessPenalty,
                emptyCellPerfectPenalty,
                nodes.stream().map(SynthesisBoardNodeDefinition::toCore).toList()
        );
    }
}
