package com.sanhiruzu.atelier.synthesis.core;

import java.util.Map;
import java.util.Set;

public record RoomAlchemyContext(
        String profileId,
        int tierCap,
        int quality,
        int stability,
        int riskBias,
        Map<String, Integer> elementBiases,
        Set<String> signals
) {
    public RoomAlchemyContext {
        profileId = profileId == null ? "" : profileId;
        tierCap = Math.clamp(tierCap, 1, 6);
        quality = Math.clamp(quality, 0, 100);
        stability = Math.clamp(stability, -100, 100);
        riskBias = Math.clamp(riskBias, -100, 100);
        elementBiases = Map.copyOf(elementBiases);
        signals = Set.copyOf(signals);
    }

    public static RoomAlchemyContext none() {
        return new RoomAlchemyContext("", 1, 0, 0, 0, Map.of(), Set.of());
    }

    /** Room no longer participates in tiering: tier cap 6 so CapResolver's min is driven by apparatus/recipe/etc. */
    public static RoomAlchemyContext neutral() {
        return new RoomAlchemyContext("", 6, 0, 0, 0, Map.of(), Set.of());
    }
}
