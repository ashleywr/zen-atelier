package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.space.zone.OutdoorZoneData;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.synthesis.core.RoomAlchemyContext;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoomAlchemyContextFactoryTest {
    @Test
    void convertsRoomDataToAlchemyContext() {
        RoomData room = new RoomData(
                UUID.randomUUID(),
                80,
                0.95f,
                Map.of("minecraft:bookshelf", 3, "minecraft:cauldron", 1, "minecraft:air", 0),
                0.82f
        );
        room.setZoneTypeId(ResourceLocation.fromNamespaceAndPath("zen_atelier", "atelier"));

        RoomAlchemyContext context = RoomAlchemyContextFactory.fromZoneData(room);

        assertThat(context.profileId()).isEqualTo("zen_atelier:atelier");
        assertThat(context.tierCap()).isEqualTo(3);
        assertThat(context.quality()).isEqualTo(82);
        assertThat(context.stability()).isEqualTo(20);
        assertThat(context.signals()).containsExactlyInAnyOrder("minecraft:bookshelf", "minecraft:cauldron");
    }

    @Test
    void mapsMidQualityRoomToTierTwo() {
        RoomData room = new RoomData(UUID.randomUUID(), 40, 0.8f, Map.of(), 0.45f);

        RoomAlchemyContext context = RoomAlchemyContextFactory.fromZoneData(room);

        assertThat(context.tierCap()).isEqualTo(2);
        assertThat(context.quality()).isEqualTo(45);
    }

    @Test
    void nonRoomDataReturnsEmptyContext() {
        OutdoorZoneData outdoor = new OutdoorZoneData(UUID.randomUUID(), 0, 0.0f, null);

        RoomAlchemyContext context = RoomAlchemyContextFactory.fromZoneData(outdoor);

        assertThat(context.profileId()).isBlank();
        assertThat(context.tierCap()).isEqualTo(1);
        assertThat(context.quality()).isZero();
        assertThat(context.signals()).isEmpty();
    }
}
