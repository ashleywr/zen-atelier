package com.sanhiruzu.atelier.client;

import java.util.UUID;

public class ClientZoneData {
    private static UUID currentZoneId;
    private static String currentZoneName;
    private static String currentGeneratedName;
    private static String currentUniqueName;
    private static String currentCustomName;
    private static String currentActiveProfiles;
    private static int currentZenScore;
    private static boolean inZone;

    public static void update(UUID id, String name, String generatedName, String uniqueName, String customName, String activeProfiles, int score) {
        currentZoneId = id;
        currentZoneName = name;
        currentGeneratedName = generatedName != null ? generatedName : "";
        currentUniqueName = uniqueName != null ? uniqueName : "";
        currentCustomName = customName != null ? customName : "";
        currentActiveProfiles = activeProfiles != null ? activeProfiles : "";
        currentZenScore = score;
        inZone = true;
    }

    public static void clear() {
        inZone = false;
    }

    public static boolean isInZone() {
        return inZone;
    }

    public static String getCurrentZoneName() {
        return currentZoneName;
    }

    public static String getCurrentGeneratedName() {
        return currentGeneratedName;
    }

    public static boolean hasDisplaySubtitle() {
        return currentGeneratedName != null
                && !currentGeneratedName.isBlank()
                && currentZoneName != null
                && !currentGeneratedName.equals(currentZoneName)
                && (!currentUniqueName.isBlank() || !currentCustomName.isBlank());
    }

    public static String getCurrentActiveProfiles() {
        return currentActiveProfiles;
    }

    public static int getCurrentZenScore() {
        return currentZenScore;
    }
}
