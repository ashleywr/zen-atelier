package com.sanhiruzu.atelier.api;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record EnvironmentInfluenceProfile(
        EnvironmentEffectType effectType,
        List<EnvironmentInfluenceCategory> categories,
        EnvironmentInfluenceCategory fallbackCategory,
        float varietyBonus,
        List<Float> tierThresholds
) {
    public EnvironmentInfluenceProfile {
        if (effectType == null) {
            throw new IllegalArgumentException("effectType must not be null");
        }
        categories = List.copyOf(categories == null ? List.of() : categories);
        if (categories.isEmpty() && fallbackCategory == null) {
            throw new IllegalArgumentException("at least one category or fallbackCategory is required");
        }
        Set<String> ids = new HashSet<>();
        for (EnvironmentInfluenceCategory category : categories) {
            if (category == null) {
                throw new IllegalArgumentException("categories must not contain null values");
            }
            if (!ids.add(category.id())) {
                throw new IllegalArgumentException("category ids must be unique");
            }
        }
        if (fallbackCategory != null && !ids.add(fallbackCategory.id())) {
            throw new IllegalArgumentException("fallbackCategory id must be unique");
        }
        if (varietyBonus < 0.0F) {
            throw new IllegalArgumentException("varietyBonus must not be negative");
        }
        tierThresholds = List.copyOf(tierThresholds == null ? List.of() : tierThresholds);
        float previous = 0.0F;
        for (Float threshold : tierThresholds) {
            if (threshold == null || threshold <= 0.0F || threshold < previous) {
                throw new IllegalArgumentException("tier thresholds must be positive and ascending");
            }
            previous = threshold;
        }
    }

    EnvironmentInfluenceCategory categoryFor(String effectId) {
        for (EnvironmentInfluenceCategory category : categories) {
            if (category.matches(effectId)) {
                return category;
            }
        }
        return fallbackCategory;
    }
}
