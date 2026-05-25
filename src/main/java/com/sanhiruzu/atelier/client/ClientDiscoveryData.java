package com.sanhiruzu.atelier.client;

import java.util.HashMap;
import java.util.Map;

public final class ClientDiscoveryData {
    private static final Map<String, Integer> bestScores = new HashMap<>();

    private ClientDiscoveryData() {
    }

    public static void update(Map<String, Integer> data) {
        bestScores.clear();
        bestScores.putAll(data);
    }

    public static boolean isDiscovered(String profileId) {
        return bestScores.containsKey(profileId);
    }

    public static int getBestScore(String profileId) {
        return bestScores.getOrDefault(profileId, -1);
    }

    public static Map<String, Integer> getAllScores() {
        return new HashMap<>(bestScores);
    }

    public static void clear() {
        bestScores.clear();
    }
}
