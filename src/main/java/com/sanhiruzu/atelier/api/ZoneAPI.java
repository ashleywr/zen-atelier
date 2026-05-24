package com.sanhiruzu.atelier.api;

import com.sanhiruzu.atelier.space.SpaceQuery;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Public API for interacting with the zone system.
 * Other mods should use this to query zones, check requirements, and attach custom data.
 * <p>
 * Safe for modders and modpack makers to use.
 */
public class ZoneAPI {

    /**
     * Get zone data at a block position.
     * Returns null if the position is not in a zone.
     */
    @Nullable
    public static ZoneData getZoneAt(Level level, BlockPos pos) {
        if (level.isClientSide()) return null;
        return SpaceQuery.getZoneAt(level, pos);
    }

    /**
     * Get zone data by UUID.
     * Returns null if the zone doesn't exist or hasn't been evaluated yet.
     */
    @Nullable
    public static ZoneData getZone(Level level, UUID zoneId) {
        if (level.isClientSide()) return null;
        return ZoneRegistry.get(level).getZone(zoneId);
    }

    /**
     * Check if a zone meets a quality threshold.
     * Returns false if zone is null or doesn't meet requirement.
     */
    public static boolean meetsQualityRequirement(Level level, BlockPos pos, float minQuality) {
        ZoneData zone = getZoneAt(level, pos);
        if (zone == null) return false;
        return zone instanceof com.sanhiruzu.atelier.space.zone.RoomData room && room.getQuality() >= minQuality;
    }

    /**
     * Check if a zone matches a type name.
     * Type matching is case-insensitive and checks if the type name contains the search term.
     * <p>
     * Example: isZoneType(zone, "greenhouse") matches zen_atelier:greenhouse, my_mod:greenhouse_deluxe, etc.
     */
    public static boolean isZoneType(ZoneData zone, String typeNamePart) {
        if (!(zone instanceof com.sanhiruzu.atelier.space.zone.RoomData room)) return false;
        var typeId = room.getZoneTypeId();
        return typeId != null && typeId.toString().toLowerCase().contains(typeNamePart.toLowerCase());
    }

    /**
     * Get or set custom data on a zone.
     * Data is stored per-zone and can be synced to clients if needed.
     * Use for storing modded data like DNA sequences, breeding state, etc.
     */
    public static class ZoneDataStore {
        private static final java.util.Map<UUID, java.util.Map<String, Object>> CUSTOM_DATA = new java.util.HashMap<>();

        /**
         * Set custom data on a zone. Data persists until the zone is removed.
         */
        public static void set(UUID zoneId, String key, Object value) {
            CUSTOM_DATA.computeIfAbsent(zoneId, k -> new java.util.HashMap<>()).put(key, value);
        }

        /**
         * Get custom data from a zone. Returns null if not found.
         */
        @Nullable
        public static Object get(UUID zoneId, String key) {
            var data = CUSTOM_DATA.get(zoneId);
            return data != null ? data.get(key) : null;
        }

        /**
         * Get custom data with type safety. Casts to the expected type.
         * Returns null if not found or type mismatch.
         */
        @Nullable
        @SuppressWarnings("unchecked")
        public static <T> T get(UUID zoneId, String key, Class<T> type) {
            Object value = get(zoneId, key);
            if (value == null || !type.isInstance(value)) return null;
            return (T) value;
        }

        /**
         * Check if custom data exists for a zone.
         */
        public static boolean has(UUID zoneId, String key) {
            var data = CUSTOM_DATA.get(zoneId);
            return data != null && data.containsKey(key);
        }

        /**
         * Remove custom data from a zone.
         */
        public static void remove(UUID zoneId, String key) {
            var data = CUSTOM_DATA.get(zoneId);
            if (data != null) {
                data.remove(key);
                if (data.isEmpty()) {
                    CUSTOM_DATA.remove(zoneId);
                }
            }
        }

        /**
         * Clear all custom data for a zone (called when zone is removed).
         */
        public static void clear(UUID zoneId) {
            CUSTOM_DATA.remove(zoneId);
        }
    }

    /**
     * Zone requirement helper for machines/blocks.
     * Modders can use this to check if a location meets zone requirements.
     */
    public static class ZoneRequirement {
        private final String typeNamePart;
        private final float minQuality;
        private final int minVolume;

        public ZoneRequirement(String typeNamePart, float minQuality, int minVolume) {
            this.typeNamePart = typeNamePart;
            this.minQuality = minQuality;
            this.minVolume = minVolume;
        }

        /**
         * Check if a location meets this requirement.
         */
        public boolean isMet(Level level, BlockPos pos) {
            ZoneData zone = getZoneAt(level, pos);
            if (zone == null) return false;

            if (minVolume > 0 && zone.getVolume() < minVolume) return false;

            if (!(zone instanceof com.sanhiruzu.atelier.space.zone.RoomData room)) return false;

            if (minQuality > 0 && room.getQuality() < minQuality) return false;

            return isZoneType(zone, typeNamePart);
        }

        @Override
        public String toString() {
            return String.format("ZoneRequirement(%s, minQuality=%.2f, minVolume=%d)", typeNamePart, minQuality, minVolume);
        }
    }
}
