package com.sanhiruzu.atelier.api;

import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Set;

public record EnvironmentSnapshot(
        Map<String, Integer> signalCounts,
        boolean isCovered,
        boolean isIndoorsLike,
        boolean isEnclosed,
        EnvironmentTemperatureBand temperatureBand,
        Set<EnvironmentAirHazard> airHazards,
        Map<EntityType<?>, Integer> nearbyEntityCounts
) {
    public static final EnvironmentSnapshot AMBIENT = new EnvironmentSnapshot(
            Map.of(),
            false,
            false,
            false,
            EnvironmentTemperatureBand.PLEASANT,
            Set.of(),
            Map.of()
    );

    public EnvironmentSnapshot {
        signalCounts = sanitizeSignalCounts(signalCounts);
        airHazards = Set.copyOf(airHazards == null ? Set.of() : airHazards);
        nearbyEntityCounts = sanitizeEntityCounts(nearbyEntityCounts);
        if (temperatureBand == null) {
            temperatureBand = EnvironmentTemperatureBand.PLEASANT;
        }
    }

    public int countSignal(String signal) {
        requireSignal(signal);
        return signalCounts.getOrDefault(signal, 0);
    }

    public boolean hasSignal(String signal) {
        return countSignal(signal) > 0;
    }

    public boolean hasSignalAtLeast(String signal, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
        return countSignal(signal) >= count;
    }

    public boolean hasAirHazard(EnvironmentAirHazard hazard) {
        if (hazard == null) {
            throw new IllegalArgumentException("hazard must not be null");
        }
        return airHazards.contains(hazard);
    }

    public boolean nearEntity(EntityType<?> type) {
        return nearEntityAtLeast(type, 1);
    }

    public boolean nearEntityAtLeast(EntityType<?> type, int count) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
        return nearbyEntityCounts.getOrDefault(type, 0) >= count;
    }

    private static Map<String, Integer> sanitizeSignalCounts(Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Integer> sanitized = new java.util.LinkedHashMap<>();
        source.forEach((signal, count) -> {
            requireSignal(signal);
            sanitized.put(signal, Math.max(0, count == null ? 0 : count));
        });
        return Map.copyOf(sanitized);
    }

    private static Map<EntityType<?>, Integer> sanitizeEntityCounts(Map<EntityType<?>, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        java.util.LinkedHashMap<EntityType<?>, Integer> sanitized = new java.util.LinkedHashMap<>();
        source.forEach((type, count) -> {
            if (type == null) {
                throw new IllegalArgumentException("entity type must not be null");
            }
            sanitized.put(type, Math.max(0, count == null ? 0 : count));
        });
        return Map.copyOf(sanitized);
    }

    private static void requireSignal(String signal) {
        if (signal == null || signal.isBlank()) {
            throw new IllegalArgumentException("signal must not be blank");
        }
    }
}
