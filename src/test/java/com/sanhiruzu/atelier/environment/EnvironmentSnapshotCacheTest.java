package com.sanhiruzu.atelier.environment;

import com.sanhiruzu.atelier.api.EnvironmentSnapshot;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentSnapshotCacheTest {
    @Test
    void reusesSnapshotUntilNearbyDirtyMark() {
        EnvironmentSnapshotCache cache = new EnvironmentSnapshotCache();
        Object levelKey = new Object();
        AtomicInteger scans = new AtomicInteger();

        EnvironmentSnapshot first = cache.getOrCompute(levelKey, 10, 64, 10, 6, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });
        EnvironmentSnapshot second = cache.getOrCompute(levelKey, 10, 64, 10, 6, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });

        assertThat(second).isSameAs(first);
        assertThat(scans).hasValue(1);

        cache.markDirty(levelKey, 12, 64, 10, 3);
        cache.getOrCompute(levelKey, 10, 64, 10, 6, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });

        assertThat(scans).hasValue(2);
    }

    @Test
    void separatesProfilesAndLevels() {
        EnvironmentSnapshotCache cache = new EnvironmentSnapshotCache();
        Object levelA = new Object();
        Object levelB = new Object();
        AtomicInteger scans = new AtomicInteger();

        cache.getOrCompute(levelA, 0, 0, 0, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });
        cache.getOrCompute(levelA, 0, 0, 0, 4, EnvironmentScanProfile.BLOCKS_AND_ENTITIES, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });
        cache.getOrCompute(levelB, 0, 0, 0, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });

        assertThat(scans).hasValue(3);
    }

    @Test
    void clearsOneLevelWithoutClearingOthers() {
        EnvironmentSnapshotCache cache = new EnvironmentSnapshotCache();
        Object levelA = new Object();
        Object levelB = new Object();
        AtomicInteger scans = new AtomicInteger();

        cache.getOrCompute(levelA, 1, 2, 3, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });
        cache.getOrCompute(levelB, 1, 2, 3, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });

        cache.clearLevel(levelA);
        cache.getOrCompute(levelA, 1, 2, 3, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });
        cache.getOrCompute(levelB, 1, 2, 3, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });

        assertThat(scans).hasValue(3);
    }

    @Test
    void usesLevelKeyIdentityInsteadOfEquality() {
        EnvironmentSnapshotCache cache = new EnvironmentSnapshotCache();
        Object levelA = new EqualLevelKey("same");
        Object levelB = new EqualLevelKey("same");
        AtomicInteger scans = new AtomicInteger();

        cache.getOrCompute(levelA, 0, 0, 0, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });
        cache.getOrCompute(levelB, 0, 0, 0, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });

        assertThat(levelB).isEqualTo(levelA);
        assertThat(levelB).isNotSameAs(levelA);
        assertThat(scans).hasValue(2);
    }

    @Test
    void rejectsNullScannerBeforeCacheHit() {
        EnvironmentSnapshotCache cache = new EnvironmentSnapshotCache();
        Object levelKey = new Object();

        cache.getOrCompute(levelKey, 0, 0, 0, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> EnvironmentSnapshot.AMBIENT);

        assertThatThrownBy(() -> cache.getOrCompute(levelKey, 0, 0, 0, 4, EnvironmentScanProfile.BLOCKS_ONLY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scanner must not be null");
    }

    private record EqualLevelKey(String id) {
        @Override
        public int hashCode() {
            return 1;
        }
    }
}
