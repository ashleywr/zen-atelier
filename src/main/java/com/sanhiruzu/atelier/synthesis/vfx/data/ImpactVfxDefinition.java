package com.sanhiruzu.atelier.synthesis.vfx.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** A complete impact effect: an ordered list of emitter layers plus an impact sound. */
public record ImpactVfxDefinition(
        ResourceLocation id,
        Optional<ResourceLocation> sound,
        List<EmitterDefinition> emitters
) {
    private static final Codec<ImpactVfxDefinition> RAW = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(ImpactVfxDefinition::id),
            ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(ImpactVfxDefinition::sound),
            EmitterDefinition.CODEC.listOf().fieldOf("emitters").forGetter(ImpactVfxDefinition::emitters)
    ).apply(instance, ImpactVfxDefinition::new));

    public static final Codec<ImpactVfxDefinition> CODEC = RAW.flatXmap(ImpactVfxDefinition::validate, DataResult::success);

    private static DataResult<ImpactVfxDefinition> validate(ImpactVfxDefinition def) {
        if (def.emitters.isEmpty()) {
            return DataResult.error(() -> "impact_vfx '" + def.id + "' must declare at least one emitter");
        }
        return DataResult.success(def);
    }
}
