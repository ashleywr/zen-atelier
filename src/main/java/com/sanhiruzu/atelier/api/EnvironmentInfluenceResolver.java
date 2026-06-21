package com.sanhiruzu.atelier.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EnvironmentInfluenceResolver {
    private EnvironmentInfluenceResolver() {
    }

    public static EnvironmentInfluenceResult resolve(List<EnvironmentEffect> effects, EnvironmentInfluenceProfile profile) {
        Objects.requireNonNull(effects, "effects must not be null");
        Objects.requireNonNull(profile, "profile must not be null");

        Map<String, MutableContribution> byCategory = new LinkedHashMap<>();
        for (EnvironmentInfluenceCategory category : profile.categories()) {
            byCategory.put(category.id(), new MutableContribution(category));
        }
        if (profile.fallbackCategory() != null) {
            byCategory.put(profile.fallbackCategory().id(), new MutableContribution(profile.fallbackCategory()));
        }

        for (EnvironmentEffect effect : effects) {
            if (effect == null || effect.type() != profile.effectType()) {
                continue;
            }
            float amount = Math.max(0.0F, effect.amount());
            if (amount <= 0.0F) {
                continue;
            }
            EnvironmentInfluenceCategory category = profile.categoryFor(effect.id());
            if (category == null) {
                continue;
            }
            byCategory.computeIfAbsent(category.id(), ignored -> new MutableContribution(category))
                    .add(effect.id(), amount);
        }

        List<EnvironmentInfluenceResult.CategoryContribution> categories = new ArrayList<>();
        float score = 0.0F;
        int activeCategories = 0;
        for (MutableContribution mutable : byCategory.values()) {
            EnvironmentInfluenceResult.CategoryContribution contribution = mutable.toContribution();
            categories.add(contribution);
            score += contribution.contribution();
            if (contribution.contribution() > 0.0F) {
                activeCategories++;
            }
        }
        if (activeCategories > 1) {
            score += (activeCategories - 1) * profile.varietyBonus();
        }
        return new EnvironmentInfluenceResult(round(score), tierFor(score, profile.tierThresholds()), categories);
    }

    private static int tierFor(float score, List<Float> thresholds) {
        int tier = 0;
        for (Float threshold : thresholds) {
            if (score >= threshold) {
                tier++;
            }
        }
        return tier;
    }

    private static float round(float value) {
        return Math.round(value * 100_000.0F) / 100_000.0F;
    }

    private static final class MutableContribution {
        private final EnvironmentInfluenceCategory category;
        private final List<String> effectIds = new ArrayList<>();
        private float rawContribution;

        private MutableContribution(EnvironmentInfluenceCategory category) {
            this.category = category;
        }

        private void add(String effectId, float amount) {
            rawContribution += amount;
            effectIds.add(effectId);
        }

        private EnvironmentInfluenceResult.CategoryContribution toContribution() {
            float contribution = Math.min(rawContribution, category.cap());
            return new EnvironmentInfluenceResult.CategoryContribution(
                    category.id(),
                    category.translationKey(),
                    round(rawContribution),
                    round(contribution),
                    rawContribution > category.cap(),
                    rawContribution > contribution,
                    effectIds
            );
        }
    }
}
