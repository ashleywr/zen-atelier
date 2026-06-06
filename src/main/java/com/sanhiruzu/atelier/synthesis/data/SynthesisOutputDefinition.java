package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SynthesisOutputDefinition(
        ResourceLocation output,
        int count,
        int tier,
        int quality,
        List<ResourceLocation> affixes,
        boolean virtual
) {
    private static final Codec<SynthesisOutputDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("output").forGetter(SynthesisOutputDefinition::output),
            Codec.INT.optionalFieldOf("count", 1).forGetter(SynthesisOutputDefinition::count),
            Codec.INT.fieldOf("tier").forGetter(SynthesisOutputDefinition::tier),
            Codec.INT.optionalFieldOf("quality", 0).forGetter(SynthesisOutputDefinition::quality),
            ResourceLocation.CODEC.listOf().optionalFieldOf("affixes", List.of()).forGetter(SynthesisOutputDefinition::affixes),
            Codec.BOOL.optionalFieldOf("virtual", false).forGetter(SynthesisOutputDefinition::virtual)
    ).apply(instance, SynthesisOutputDefinition::new));
    public static final Codec<SynthesisOutputDefinition> CODEC = RAW_CODEC.flatXmap(SynthesisOutputDefinition::validate, DataResult::success);

    public SynthesisOutputDefinition(
            ResourceLocation output,
            int count,
            int tier,
            int quality,
            List<ResourceLocation> affixes
    ) {
        this(output, count, tier, quality, affixes, false);
    }

    private static DataResult<SynthesisOutputDefinition> validate(SynthesisOutputDefinition definition) {
        if (SynthesisDataValidation.positive("count", definition.count).error().isPresent()) {
            return SynthesisDataValidation.error("count must be positive");
        }
        if (SynthesisDataValidation.tier("tier", definition.tier).error().isPresent()) {
            return SynthesisDataValidation.error("tier must be between 1 and 6");
        }
        if (SynthesisDataValidation.percent("quality", definition.quality).error().isPresent()) {
            return SynthesisDataValidation.error("quality must be between 0 and 100");
        }
        if (!definition.virtual && !BuiltInRegistries.ITEM.containsKey(definition.output)) {
            return SynthesisDataValidation.error("output " + definition.output + " must be a registered item or set virtual=true");
        }
        return DataResult.success(definition);
    }

    public SynthesisOutput toCore() {
        return new SynthesisOutput(
                output.toString(),
                count,
                tier,
                quality,
                affixes.stream().map(ResourceLocation::toString).toList()
        );
    }
}
