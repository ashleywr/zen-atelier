package com.sanhiruzu.atelier.api;

import java.util.List;
import java.util.Map;

public record EnvironmentInfluenceResult(
        float score,
        int tier,
        List<CategoryContribution> categories
) {
    public EnvironmentInfluenceResult {
        categories = List.copyOf(categories == null ? List.of() : categories);
    }

    public int activeCategoryCount() {
        int active = 0;
        for (CategoryContribution category : categories) {
            if (category.contribution() > 0.0F) {
                active++;
            }
        }
        return active;
    }

    public CategoryContribution category(String id) {
        for (CategoryContribution category : categories) {
            if (category.id().equals(id)) {
                return category;
            }
        }
        return CategoryContribution.empty(id);
    }

    public Map<String, CategoryContribution> byCategory() {
        return categories.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                CategoryContribution::id,
                category -> category
        ));
    }

    public record CategoryContribution(
            String id,
            String translationKey,
            float rawContribution,
            float contribution,
            boolean capped,
            boolean diminished,
            List<String> effectIds
    ) {
        public CategoryContribution {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id must not be blank");
            }
            if (translationKey == null || translationKey.isBlank()) {
                throw new IllegalArgumentException("translationKey must not be blank");
            }
            rawContribution = Math.max(0.0F, rawContribution);
            contribution = Math.max(0.0F, contribution);
            effectIds = List.copyOf(effectIds == null ? List.of() : effectIds);
        }

        static CategoryContribution empty(String id) {
            return new CategoryContribution(id, id, 0.0F, 0.0F, false, false, List.of());
        }
    }
}
