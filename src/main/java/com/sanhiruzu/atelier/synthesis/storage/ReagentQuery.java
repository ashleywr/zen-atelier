package com.sanhiruzu.atelier.synthesis.storage;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;

import java.util.Map;
import java.util.Set;

public record ReagentQuery(
        Set<String> reagentIds,
        int minTier,
        int maxTier,
        int minQuality,
        int minPurity,
        int maxInstability,
        Set<String> requiredCategories,
        Map<String, Integer> minElements,
        Set<String> requiredTraits,
        Set<String> requiredSourceHints
) {
    public static final String DEBUG_UNIVERSAL_REAGENT_ID = "zen_atelier:debug_universal_reagent";

    public ReagentQuery {
        reagentIds = Set.copyOf(reagentIds);
        minTier = minTier <= 0 ? 1 : minTier;
        maxTier = maxTier <= 0 ? 6 : maxTier;
        minQuality = clamp(minQuality, 0, 100);
        minPurity = clamp(minPurity, 0, 100);
        maxInstability = maxInstability <= 0 ? 100 : clamp(maxInstability, 0, 100);
        requiredCategories = Set.copyOf(requiredCategories);
        minElements = Map.copyOf(minElements);
        requiredTraits = Set.copyOf(requiredTraits);
        requiredSourceHints = Set.copyOf(requiredSourceHints);
    }

    public static ReagentQuery any() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean matches(ReagentStack stack) {
        if (DEBUG_UNIVERSAL_REAGENT_ID.equals(stack.reagentId())) {
            return true;
        }
        if (!reagentIds.isEmpty() && !reagentIds.contains(stack.reagentId())) {
            return false;
        }
        if (stack.tier() < minTier || stack.tier() > maxTier) {
            return false;
        }
        if (stack.quality() < minQuality || stack.purity() < minPurity || stack.instability() > maxInstability) {
            return false;
        }
        if (!stack.categories().containsAll(requiredCategories)) {
            return false;
        }
        for (Map.Entry<String, Integer> required : minElements.entrySet()) {
            if (stack.elements().getOrDefault(required.getKey(), 0) < required.getValue()) {
                return false;
            }
        }
        if (!stack.traits().containsAll(requiredTraits)) {
            return false;
        }
        return stack.sourceHints().containsAll(requiredSourceHints);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Builder {
        private Set<String> reagentIds = Set.of();
        private int minTier = 1;
        private int maxTier = 6;
        private int minQuality = 0;
        private int minPurity = 0;
        private int maxInstability = 100;
        private Set<String> requiredCategories = Set.of();
        private Map<String, Integer> minElements = Map.of();
        private Set<String> requiredTraits = Set.of();
        private Set<String> requiredSourceHints = Set.of();

        public Builder reagentIds(Set<String> reagentIds) {
            this.reagentIds = Set.copyOf(reagentIds);
            return this;
        }

        public Builder minTier(int minTier) {
            this.minTier = minTier;
            return this;
        }

        public Builder maxTier(int maxTier) {
            this.maxTier = maxTier;
            return this;
        }

        public Builder minQuality(int minQuality) {
            this.minQuality = minQuality;
            return this;
        }

        public Builder minPurity(int minPurity) {
            this.minPurity = minPurity;
            return this;
        }

        public Builder maxInstability(int maxInstability) {
            this.maxInstability = maxInstability;
            return this;
        }

        public Builder requiredCategories(Set<String> requiredCategories) {
            this.requiredCategories = Set.copyOf(requiredCategories);
            return this;
        }

        public Builder minElements(Map<String, Integer> minElements) {
            this.minElements = Map.copyOf(minElements);
            return this;
        }

        public Builder requiredTraits(Set<String> requiredTraits) {
            this.requiredTraits = Set.copyOf(requiredTraits);
            return this;
        }

        public Builder requiredSourceHints(Set<String> requiredSourceHints) {
            this.requiredSourceHints = Set.copyOf(requiredSourceHints);
            return this;
        }

        public ReagentQuery build() {
            return new ReagentQuery(
                    reagentIds,
                    minTier,
                    maxTier,
                    minQuality,
                    minPurity,
                    maxInstability,
                    requiredCategories,
                    minElements,
                    requiredTraits,
                    requiredSourceHints
            );
        }
    }
}
