package com.sanhiruzu.atelier.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record ZoneDefinition(
        ResourceLocation id,
        String displayName,
        boolean sealed,
        float baseTemperature,
        float baseHumidity,
        boolean requiresCeiling
) {
    public static final Codec<ZoneDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(ZoneDefinition::id),
            Codec.STRING.fieldOf("display_name").forGetter(ZoneDefinition::displayName),
            Codec.BOOL.optionalFieldOf("sealed", false).forGetter(ZoneDefinition::sealed),
            Codec.FLOAT.optionalFieldOf("base_temperature", 20.0f).forGetter(ZoneDefinition::baseTemperature),
            Codec.FLOAT.optionalFieldOf("base_humidity", 50.0f).forGetter(ZoneDefinition::baseHumidity),
            Codec.BOOL.optionalFieldOf("requires_ceiling", false).forGetter(ZoneDefinition::requiresCeiling)
    ).apply(instance, ZoneDefinition::new));
}
