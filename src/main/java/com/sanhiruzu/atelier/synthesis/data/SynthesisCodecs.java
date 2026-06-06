package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;

final class SynthesisCodecs {
    static final Codec<OutcomeClass> OUTCOME_CLASS = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(OutcomeClass.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return DataResult.error(() -> "Unknown outcome class: " + value);
        }
    }, outcome -> outcome.name().toLowerCase());

    private SynthesisCodecs() {
    }
}
