package com.sanhiruzu.atelier.api;

import net.minecraft.world.entity.EntityType;

import java.util.Arrays;

public final class EnvironmentConditions {
    private EnvironmentConditions() {
    }

    public static EnvironmentCondition always() {
        return snapshot -> true;
    }

    public static EnvironmentCondition allOf(EnvironmentCondition... conditions) {
        EnvironmentCondition[] copy = copyConditions(conditions);
        return snapshot -> Arrays.stream(copy).allMatch(condition -> condition.matches(snapshot));
    }

    public static EnvironmentCondition anyOf(EnvironmentCondition... conditions) {
        EnvironmentCondition[] copy = copyConditions(conditions);
        return snapshot -> Arrays.stream(copy).anyMatch(condition -> condition.matches(snapshot));
    }

    public static EnvironmentCondition not(EnvironmentCondition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("condition must not be null");
        }
        return snapshot -> !condition.matches(snapshot);
    }

    public static EnvironmentCondition covered() {
        return EnvironmentSnapshot::isCovered;
    }

    public static EnvironmentCondition indoorsLike() {
        return EnvironmentSnapshot::isIndoorsLike;
    }

    public static EnvironmentCondition enclosed() {
        return EnvironmentSnapshot::isEnclosed;
    }

    public static EnvironmentCondition signalAtLeast(String signal, int count) {
        requireSignal(signal);
        requireCount(count);
        return snapshot -> snapshot.hasSignalAtLeast(signal, count);
    }

    public static EnvironmentCondition signalAbsent(String signal) {
        requireSignal(signal);
        return snapshot -> !snapshot.hasSignal(signal);
    }

    public static EnvironmentCondition temperature(EnvironmentTemperatureBand band) {
        if (band == null) {
            throw new IllegalArgumentException("band must not be null");
        }
        return snapshot -> snapshot.temperatureBand() == band;
    }

    public static EnvironmentCondition airHazard(EnvironmentAirHazard hazard) {
        if (hazard == null) {
            throw new IllegalArgumentException("hazard must not be null");
        }
        return snapshot -> snapshot.hasAirHazard(hazard);
    }

    public static EnvironmentCondition nearEntityAtLeast(EntityType<?> type, int count) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        requireCount(count);
        return snapshot -> snapshot.nearEntityAtLeast(type, count);
    }

    private static EnvironmentCondition[] copyConditions(EnvironmentCondition[] conditions) {
        if (conditions == null) {
            throw new IllegalArgumentException("conditions must not be null");
        }
        if (conditions.length == 0) {
            return new EnvironmentCondition[] { always() };
        }
        EnvironmentCondition[] copy = Arrays.copyOf(conditions, conditions.length);
        for (EnvironmentCondition condition : copy) {
            if (condition == null) {
                throw new IllegalArgumentException("condition must not be null");
            }
        }
        return copy;
    }

    private static void requireSignal(String signal) {
        if (signal == null || signal.isBlank()) {
            throw new IllegalArgumentException("signal must not be blank");
        }
    }

    private static void requireCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
    }
}
