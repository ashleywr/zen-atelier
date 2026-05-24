package com.sanhiruzu.atelier.zone.naming;

import com.sanhiruzu.atelier.space.zone.RoomData;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RoomEpithetRegistry {
    private static final List<RoomEpithet> EPITHETS = new CopyOnWriteArrayList<>();

    private RoomEpithetRegistry() {
    }

    public static void register(RoomEpithet epithet) {
        EPITHETS.removeIf(existing -> existing.id().equals(epithet.id()));
        EPITHETS.add(epithet);
        EPITHETS.sort(Comparator
                .comparing(RoomEpithet::priority, Comparator.reverseOrder())
                .thenComparing(RoomEpithet::minimumScore, Comparator.reverseOrder())
                .thenComparing(RoomEpithet::id));
    }

    public static Optional<RoomEpithet> findMatch(RoomData room, int score) {
        return EPITHETS.stream()
                .filter(epithet -> epithet.matches(room, score))
                .findFirst();
    }

    public static void clear() {
        EPITHETS.clear();
    }
}
