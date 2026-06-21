package com.sanhiruzu.atelier.api;

import java.util.List;

public record EnvironmentInfluenceCategory(
        String id,
        String translationKey,
        float cap,
        List<String> effectIdPrefixes
) {
    public EnvironmentInfluenceCategory {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (translationKey == null || translationKey.isBlank()) {
            throw new IllegalArgumentException("translationKey must not be blank");
        }
        if (cap < 0.0F) {
            throw new IllegalArgumentException("cap must not be negative");
        }
        effectIdPrefixes = List.copyOf(effectIdPrefixes == null ? List.of() : effectIdPrefixes);
        for (String prefix : effectIdPrefixes) {
            if (prefix == null || prefix.isBlank()) {
                throw new IllegalArgumentException("effectIdPrefixes must not contain blank values");
            }
        }
    }

    boolean matches(String effectId) {
        for (String prefix : effectIdPrefixes) {
            if (effectId.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
