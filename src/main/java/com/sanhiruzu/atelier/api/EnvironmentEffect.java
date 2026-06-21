package com.sanhiruzu.atelier.api;

public record EnvironmentEffect(EnvironmentEffectType type, String id, float amount) {
    public EnvironmentEffect {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
