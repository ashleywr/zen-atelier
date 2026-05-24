package com.sanhiruzu.atelier.ui.client;

import java.util.UUID;

public final class ClientZoneData {
    private static UUID currentZoneId;
    private static String currentZoneName = "";
    private static String currentGeneratedName = "";
    private static String currentUniqueName = "";
    private static String currentCustomName = "";
    private static String currentActiveProfiles = "";
    private static String qualityBreakdown = "";
    private static int currentZenScore;
    private static boolean inZone;
    private static boolean debugMode;
    private static int roomHudTicks = 0;
    private static final int ROOM_HUD_REVEAL_TICKS = 100; // 5 seconds at 20 ticks/s

    // "Room lost" flash: shown for LOST_FLASH_TICKS after the zone disappears.
    private static int zoneLostFlashTicks = 0;
    private static final int LOST_FLASH_TICKS = 80; // 4 seconds at 20 ticks/s

    // Grace period — zone lost its entry, countdown before permanent deletion
    private static UUID gracePeriodZoneId = null;
    private static int gracePeriodTicksRemaining = 0;

    private ClientZoneData() {
    }

    public static void update(UUID id, String name, String generatedName, String uniqueName, String customName, String activeProfiles, int score, String qualityBreakdown) {
        boolean enteredOrChangedRoom = !inZone || !java.util.Objects.equals(currentZoneId, id);
        currentZoneId = id;
        currentZoneName = name != null ? name : "";
        currentGeneratedName = generatedName != null ? generatedName : "";
        currentUniqueName = uniqueName != null ? uniqueName : "";
        currentCustomName = customName != null ? customName : "";
        currentActiveProfiles = activeProfiles != null ? activeProfiles : "";
        ClientZoneData.qualityBreakdown = qualityBreakdown != null ? qualityBreakdown : "";
        currentZenScore = Math.max(0, Math.min(100, score));
        if (enteredOrChangedRoom || debugMode) roomHudTicks = ROOM_HUD_REVEAL_TICKS;
        if (!inZone) zoneLostFlashTicks = 0; // entering a zone cancels the flash
        inZone = true;
    }

    public static void clear() {
        if (inZone) zoneLostFlashTicks = LOST_FLASH_TICKS;
        if (inZone) roomHudTicks = ROOM_HUD_REVEAL_TICKS;
        currentZoneId = null;
        inZone = false;
    }

    /**
     * Clear without showing the "Room lost" flash — used when the player walks out of a zone that still exists.
     */
    public static void clearSilently() {
        if (inZone) roomHudTicks = ROOM_HUD_REVEAL_TICKS;
        currentZoneId = null;
        inZone = false;
    }

    /**
     * Must be called once per client tick to advance the flash countdown.
     */
    public static void tick() {
        if (roomHudTicks > 0) roomHudTicks--;
        if (zoneLostFlashTicks > 0) zoneLostFlashTicks--;
        if (gracePeriodTicksRemaining > 0) {
            gracePeriodTicksRemaining--;
            if (gracePeriodTicksRemaining <= 0) gracePeriodZoneId = null;
        }
    }

    public static void revealRoomHud() {
        roomHudTicks = ROOM_HUD_REVEAL_TICKS;
    }

    public static boolean shouldShowRoomHud() {
        return hasRoomHudSnapshot() && roomHudTicks > 0;
    }

    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        if (enabled && inZone) revealRoomHud();
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    private static boolean hasRoomHudSnapshot() {
        return currentZoneId != null || !currentZoneName.isBlank();
    }

    public static void startGracePeriod(UUID zoneId, int ticks) {
        gracePeriodZoneId = zoneId;
        gracePeriodTicksRemaining = ticks;
    }

    public static void cancelGracePeriod(UUID zoneId) {
        if (java.util.Objects.equals(gracePeriodZoneId, zoneId)) {
            gracePeriodZoneId = null;
            gracePeriodTicksRemaining = 0;
        }
    }

    public static boolean isShowingGracePeriod() {
        return gracePeriodZoneId != null && gracePeriodTicksRemaining > 0;
    }

    public static UUID getGracePeriodZoneId() {
        return gracePeriodZoneId;
    }

    public static int getGracePeriodTicksRemaining() {
        return gracePeriodTicksRemaining;
    }

    public static boolean isInZone() {
        return inZone;
    }

    /**
     * True while the "Room lost" flash is still showing.
     */
    public static boolean isShowingZoneLost() {
        return !inZone && zoneLostFlashTicks > 0;
    }

    /**
     * 1.0 = just lost, fades to 0.0 over {@code LOST_FLASH_TICKS} ticks.
     */
    public static float zoneLostFade() {
        return zoneLostFlashTicks / (float) LOST_FLASH_TICKS;
    }

    public static UUID getCurrentZoneId() {
        return currentZoneId;
    }

    public static String getCurrentZoneName() {
        return currentZoneName;
    }

    public static String getCurrentGeneratedName() {
        return currentGeneratedName;
    }

    public static boolean hasDisplaySubtitle() {
        return !currentGeneratedName.isBlank()
                && !currentZoneName.isBlank()
                && !currentGeneratedName.equals(currentZoneName)
                && (!currentUniqueName.isBlank() || !currentCustomName.isBlank());
    }

    public static String getCurrentActiveProfiles() {
        return currentActiveProfiles;
    }

    public static String getQualityBreakdown() {
        return qualityBreakdown;
    }

    public static int getCurrentZenScore() {
        return currentZenScore;
    }

    public static void forgetRoomSnapshot() {
        currentZoneId = null;
        currentZoneName = "";
        currentGeneratedName = "";
        currentUniqueName = "";
        currentCustomName = "";
        currentActiveProfiles = "";
        qualityBreakdown = "";
        currentZenScore = 0;
        roomHudTicks = 0;
    }

    static void resetForTest() {
        currentZoneId = null;
        currentZoneName = "";
        currentGeneratedName = "";
        currentUniqueName = "";
        currentCustomName = "";
        currentActiveProfiles = "";
        qualityBreakdown = "";
        currentZenScore = 0;
        inZone = false;
        debugMode = false;
        roomHudTicks = 0;
        zoneLostFlashTicks = 0;
        gracePeriodZoneId = null;
        gracePeriodTicksRemaining = 0;
    }
}
