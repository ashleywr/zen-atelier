package com.sanhiruzu.atelier.environment;

import com.sanhiruzu.atelier.api.EnvironmentSnapshot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class EnvironmentSnapshotCache {
    private final Map<Key, Entry> entries = new HashMap<>();
    private final LongSupplier ticker;

    public EnvironmentSnapshotCache() {
        this(System::nanoTime);
    }

    EnvironmentSnapshotCache(LongSupplier ticker) {
        this.ticker = Objects.requireNonNull(ticker, "ticker");
    }

    public synchronized EnvironmentSnapshot getOrCompute(
            Object levelKey,
            int x,
            int y,
            int z,
            int radius,
            EnvironmentScanProfile profile,
            Supplier<EnvironmentSnapshot> scanner
    ) {
        return getOrCompute(levelKey, x, y, z, radius, profile, -1L, false, scanner);
    }

    public synchronized EnvironmentSnapshot getOrCompute(
            Object levelKey,
            int x,
            int y,
            int z,
            int radius,
            EnvironmentScanProfile profile,
            long ttlNanos,
            Supplier<EnvironmentSnapshot> scanner
    ) {
        if (ttlNanos < 0) {
            throw new IllegalArgumentException("ttlNanos must be non-negative");
        }
        return getOrCompute(levelKey, x, y, z, radius, profile, ttlNanos, true, scanner);
    }

    private EnvironmentSnapshot getOrCompute(
            Object levelKey,
            int x,
            int y,
            int z,
            int radius,
            EnvironmentScanProfile profile,
            long ttlNanos,
            boolean expires,
            Supplier<EnvironmentSnapshot> scanner
    ) {
        if (scanner == null) {
            throw new IllegalArgumentException("scanner must not be null");
        }
        long now = expires ? ticker.getAsLong() : 0L;
        Key key = new Key(levelKey, x, y, z, radius, profile);
        Entry entry = entries.get(key);
        if (entry != null && !entry.dirty && !entry.isExpired(expires, now, ttlNanos)) {
            return entry.snapshot;
        }
        EnvironmentSnapshot snapshot = Objects.requireNonNull(scanner.get(), "scanner returned null");
        entries.put(key, new Entry(snapshot, false, now));
        return snapshot;
    }

    public synchronized void markDirty(Object levelKey, int x, int y, int z, int dirtyRadius) {
        for (Map.Entry<Key, Entry> cacheEntry : entries.entrySet()) {
            Key key = cacheEntry.getKey();
            if (key.levelKey != levelKey) {
                continue;
            }
            int reach = key.radius + Math.max(0, dirtyRadius);
            if (Math.abs(key.x - x) <= reach && Math.abs(key.y - y) <= reach && Math.abs(key.z - z) <= reach) {
                cacheEntry.getValue().dirty = true;
            }
        }
    }

    public synchronized void clearLevel(Object levelKey) {
        Iterator<Key> iterator = entries.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().levelKey == levelKey) {
                iterator.remove();
            }
        }
    }

    public synchronized void clearAll() {
        entries.clear();
    }

    private static final class Key {
        private final Object levelKey;
        private final int x;
        private final int y;
        private final int z;
        private final int radius;
        private final EnvironmentScanProfile profile;

        private Key(Object levelKey, int x, int y, int z, int radius, EnvironmentScanProfile profile) {
            this.levelKey = Objects.requireNonNull(levelKey, "levelKey");
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.profile = Objects.requireNonNull(profile, "profile");
            if (radius < 0) {
                throw new IllegalArgumentException("radius must be non-negative");
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return levelKey == key.levelKey
                    && x == key.x
                    && y == key.y
                    && z == key.z
                    && radius == key.radius
                    && profile == key.profile;
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(levelKey), x, y, z, radius, profile);
        }
    }

    private static final class Entry {
        private final EnvironmentSnapshot snapshot;
        private final long cachedAtNanos;
        private boolean dirty;

        private Entry(EnvironmentSnapshot snapshot, boolean dirty, long cachedAtNanos) {
            this.snapshot = snapshot;
            this.dirty = dirty;
            this.cachedAtNanos = cachedAtNanos;
        }

        private boolean isExpired(boolean expires, long now, long ttlNanos) {
            return expires && now - cachedAtNanos >= ttlNanos;
        }
    }
}
