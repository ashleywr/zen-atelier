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
}
