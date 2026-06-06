package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.engine.ReagentRollTemplate;

public record IntRangeDefinition(int min, int max) {
    private static final Codec<IntRangeDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("min").forGetter(IntRangeDefinition::min),
            Codec.INT.fieldOf("max").forGetter(IntRangeDefinition::max)
    ).apply(instance, IntRangeDefinition::new));
    public static final Codec<IntRangeDefinition> CODEC = RAW_CODEC.flatXmap(IntRangeDefinition::validate, DataResult::success);

    private static DataResult<IntRangeDefinition> validate(IntRangeDefinition range) {
        if (range.min < 0) {
            return SynthesisDataValidation.error("range min must not be negative");
        }
        if (range.max < range.min) {
            return SynthesisDataValidation.error("range max must be greater than or equal to min");
        }
        return DataResult.success(range);
    }

    public ReagentRollTemplate.IntRange toCore() {
        return new ReagentRollTemplate.IntRange(min, max);
    }
}
