package com.sanhiruzu.atelier.synthesis.data;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class TraitFusionRegistry {
    private static final Map<String, TraitFusionRule> BY_PAIR_KEY = new LinkedHashMap<>();
    private static final Map<String, TraitFusionRule> BY_ID = new LinkedHashMap<>();

    private TraitFusionRegistry() {
    }

    public static void replaceAll(Collection<TraitFusionRule> rules) {
        BY_PAIR_KEY.clear();
        BY_ID.clear();
        for (TraitFusionRule rule : rules) {
            BY_PAIR_KEY.put(rule.pairKey(), rule);
            BY_ID.put(rule.id(), rule);
        }
    }

    public static Optional<TraitFusionRule> find(String traitA, String traitB) {
        return Optional.ofNullable(BY_PAIR_KEY.get(pairKey(traitA, traitB)));
    }

    public static Optional<TraitFusionRule> findById(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Collection<TraitFusionRule> all() {
        return Collections.unmodifiableCollection(BY_PAIR_KEY.values());
    }

    static String pairKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }
}
