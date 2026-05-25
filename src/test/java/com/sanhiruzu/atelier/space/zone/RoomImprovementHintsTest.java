package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.data.RequiredFeature;
import com.sanhiruzu.atelier.data.RoomProfile;
import com.sanhiruzu.atelier.data.RoomProfileRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoomImprovementHintsTest {
    @AfterEach
    void clearProfiles() {
        RoomProfileRegistry.replaceAll(Map.of());
    }

    @Test
    void missingRequiredFeatureProducesActionableHint() {
        ResourceLocation profileId = ResourceLocation.fromNamespaceAndPath("zen_atelier", "test_atelier");
        RoomProfileRegistry.replaceAll(Map.of(profileId, new RoomProfile(
                profileId,
                "room_type.zen_atelier.test_atelier",
                ResourceLocation.fromNamespaceAndPath("zen_atelier", "test_zone"),
                List.of(),
                List.of(new RequiredFeature("cauldron", 2)),
                List.of(),
                List.of(),
                List.of()
        )));

        RoomData room = new RoomData(UUID.randomUUID(), 80, 0.9f, Map.of(), Map.of("cauldron", 1), 0.5f);
        room.setZoneTypeId(profileId);

        assertThat(RoomImprovementHints.forRoom(room))
                .contains("Add 1 more cauldron.");
    }

    @Test
    void industrialPenaltyProducesSpecificCleanupHint() {
        ResourceLocation profileId = ResourceLocation.fromNamespaceAndPath("zen_atelier", "test_greenhouse");
        RoomProfileRegistry.replaceAll(Map.of(profileId, new RoomProfile(
                profileId,
                "room_type.zen_atelier.test_greenhouse",
                ResourceLocation.fromNamespaceAndPath("zen_atelier", "test_zone"),
                List.of(),
                List.of(),
                List.of(),
                List.of("industrial_blocks"),
                List.of()
        )));

        RoomData room = new RoomData(UUID.randomUUID(), 80, 0.9f, Map.of(), Map.of("industrial_blocks", 2), 0.5f);
        room.setZoneTypeId(profileId);

        assertThat(RoomImprovementHints.forRoom(room))
                .contains("Move machinery, concrete, chains, iron bars, or factory clutter away from this room.");
    }

    @Test
    void hintsAreLimitedAndDistinct() {
        Map<String, Integer> signals = new LinkedHashMap<>();
        RoomData room = new RoomData(UUID.randomUUID(), 12, 0.2f, Map.of(), signals, 0.1f);

        List<String> hints = RoomImprovementHints.forRoom(room);

        assertThat(hints).hasSizeLessThanOrEqualTo(4);
        assertThat(hints).doesNotHaveDuplicates();
    }
}
