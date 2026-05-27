package com.sanhiruzu.atelier.ui.network;

import com.sanhiruzu.atelier.data.RequiredFeature;
import com.sanhiruzu.atelier.data.RoomProfile;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoomCatalogSyncPayloadTest {

    @Test
    void fromProfilesIncludesRuntimeProfilesWithHintsAndIcons() {
        ResourceLocation profileId = ResourceLocation.fromNamespaceAndPath("example_mod", "frog_habitat");
        RoomProfile profile = new RoomProfile(
                profileId,
                "room_type.example_mod.frog_habitat",
                ResourceLocation.fromNamespaceAndPath("example_mod", "damp_habitat"),
                "minecraft:lily_pad",
                List.of("#c:frog_plants"),
                List.of(new RequiredFeature("water_coverage", 2)),
                List.of(),
                List.of(),
                List.of()
        );

        RoomCatalogSyncPayload payload = RoomCatalogSyncPayload.fromProfiles(List.of(profile));

        assertThat(payload.entries()).hasSize(1);
        RoomCatalogSyncPayload.Entry entry = payload.entries().getFirst();
        assertThat(entry.profileId()).isEqualTo("example_mod:frog_habitat");
        assertThat(entry.displayName()).isEqualTo("room_type.example_mod.frog_habitat");
        assertThat(entry.iconItemId()).isEqualTo("minecraft:lily_pad");
        assertThat(entry.hints()).contains("Look for nearby source water.");
    }
}
