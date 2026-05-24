package com.sanhiruzu.atelier.ui.client;

import java.util.HashMap;
import java.util.Map;

public final class ClientDiscoveryData {
    private static final Map<String, Integer> BEST_SCORES = new HashMap<>();

    private ClientDiscoveryData() {
    }

    public static void update(Map<String, Integer> data) {
        BEST_SCORES.clear();
        BEST_SCORES.putAll(data);
    }

    public static boolean isDiscovered(String profileId) {
        return BEST_SCORES.containsKey(profileId);
    }

    public static int getBestScore(String profileId) {
        return BEST_SCORES.getOrDefault(profileId, -1);
    }

    public static void clear() {
        BEST_SCORES.clear();
    }
}
