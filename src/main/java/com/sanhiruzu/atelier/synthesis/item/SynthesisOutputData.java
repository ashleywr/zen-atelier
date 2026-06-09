package com.sanhiruzu.atelier.synthesis.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record SynthesisOutputData(int tier, int quality, List<String> affixes) {
    public static final int QUALITY_IMPROVED = 50;
    public static final int QUALITY_STRONG   = 75;
    public static final int QUALITY_PERFECT  = 100;

    public static final Codec<SynthesisOutputData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("tier").forGetter(SynthesisOutputData::tier),
            Codec.INT.fieldOf("quality").forGetter(SynthesisOutputData::quality),
            Codec.STRING.listOf().optionalFieldOf("affixes", List.of()).forGetter(SynthesisOutputData::affixes)
    ).apply(instance, SynthesisOutputData::new));

    public SynthesisOutputData {
        tier = Math.clamp(tier, 1, 6);
        quality = Math.clamp(quality, 0, 100);
        affixes = List.copyOf(affixes);
    }

    /** Returns 0–3: base / improved (50+) / strong (75+) / perfect (100). */
    public int qualityTier() {
        if (quality >= QUALITY_PERFECT)  return 3;
        if (quality >= QUALITY_STRONG)   return 2;
        if (quality >= QUALITY_IMPROVED) return 1;
        return 0;
    }
}
