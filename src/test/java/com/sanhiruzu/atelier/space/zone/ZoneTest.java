package com.sanhiruzu.atelier.space.zone;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneTest {

    @Test
    void zonesWithSameIdAreEqual() {
        UUID id = UUID.randomUUID();
        assertThat(new Zone(id)).isEqualTo(new Zone(id));
    }

    @Test
    void zonesWithDifferentIdsAreNotEqual() {
        assertThat(new Zone(UUID.randomUUID())).isNotEqualTo(new Zone(UUID.randomUUID()));
    }

    @Test
    void hashCodeIsConsistentWithEquals() {
        UUID id = UUID.randomUUID();
        assertThat(new Zone(id).hashCode()).isEqualTo(new Zone(id).hashCode());
    }

    @Test
    void getIdReturnsConstructedId() {
        UUID id = UUID.randomUUID();
        assertThat(new Zone(id).getId()).isEqualTo(id);
    }

    @Test
    void zoneDoesNotEqualNull() {
        assertThat(new Zone(UUID.randomUUID())).isNotEqualTo(null);
    }

    @Test
    void zoneEqualsItself() {
        Zone zone = new Zone(UUID.randomUUID());
        assertThat(zone).isEqualTo(zone);
    }
}
