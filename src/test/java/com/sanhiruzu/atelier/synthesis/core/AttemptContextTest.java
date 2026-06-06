package com.sanhiruzu.atelier.synthesis.core;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttemptContextTest {
    @Test
    void clampsRoomAndApparatusFields() {
        ApparatusState apparatus = new ApparatusState("zen_atelier:test", 99, 150);
        RoomAlchemyContext room = new RoomAlchemyContext(
                "zen_atelier:atelier",
                99,
                150,
                -150,
                150,
                Map.of("fire", 2),
                Set.of("bookshelf")
        );

        assertThat(apparatus.tierCap()).isEqualTo(6);
        assertThat(apparatus.stabilityBonus()).isEqualTo(100);
        assertThat(room.tierCap()).isEqualTo(6);
        assertThat(room.quality()).isEqualTo(100);
        assertThat(room.stability()).isEqualTo(-100);
        assertThat(room.riskBias()).isEqualTo(100);
    }

    @Test
    void contextAppliesRoomRiskBiasAndApparatusStability() {
        ApparatusState apparatus = new ApparatusState("zen_atelier:stable_cauldron", 3, 20);
        RoomAlchemyContext room = new RoomAlchemyContext("zen_atelier:volatile_room", 4, 50, 0, 30, Map.of(), Set.of());

        AttemptContext context = new AttemptContext(apparatus, room, 5, 40);

        assertThat(context.apparatusTierCap()).isEqualTo(3);
        assertThat(context.roomTierCap()).isEqualTo(4);
        assertThat(context.configTierCap()).isEqualTo(5);
        assertThat(context.risk()).isEqualTo(50);
    }

    @Test
    void contextRiskIsClamped() {
        AttemptContext high = new AttemptContext(
                new ApparatusState("zen_atelier:crude", 1, -100),
                new RoomAlchemyContext("zen_atelier:risky", 1, 0, 0, 100, Map.of(), Set.of()),
                1,
                100
        );
        AttemptContext low = new AttemptContext(
                new ApparatusState("zen_atelier:stable", 1, 100),
                RoomAlchemyContext.none(),
                1,
                0
        );

        assertThat(high.risk()).isEqualTo(100);
        assertThat(low.risk()).isZero();
    }

    @Test
    void rejectsBlankApparatusIdAndNullContextParts() {
        assertThatThrownBy(() -> ApparatusState.crude(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AttemptContext(null, RoomAlchemyContext.none(), 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AttemptContext(ApparatusState.crude("zen_atelier:test"), null, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
