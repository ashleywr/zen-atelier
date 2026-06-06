package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.synthesis.core.RoomAlchemyContext;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class RoomAlchemyContextFactory {
    private static final ResourceLocation ATELIER_ROOM_ID = ResourceLocation.fromNamespaceAndPath("zen_atelier", "atelier");

    private RoomAlchemyContextFactory() {
    }

    public static RoomAlchemyContext fromZoneData(ZoneData zoneData) {
        if (!(zoneData instanceof RoomData room)) {
            return RoomAlchemyContext.none();
        }

        int quality = Math.clamp(Math.round(room.getQuality() * 100.0f), 0, 100);
        int tierCap = tierCapForQuality(room, quality);
        int stability = Math.clamp(Math.round((room.getEnclosureScore() - 0.75f) * 100.0f), -100, 100);
        String profileId = room.getZoneTypeId() == null ? "" : room.getZoneTypeId().toString();
        Set<String> signals = room.getFurnitureCounts().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());

        return new RoomAlchemyContext(profileId, tierCap, quality, stability, 0, Map.of(), signals);
    }

    private static int tierCapForQuality(RoomData room, int quality) {
        boolean atelier = ATELIER_ROOM_ID.equals(room.getZoneTypeId());
        if (atelier && quality >= 75) {
            return 4;
        }
        if (quality >= 75) {
            return 3;
        }
        if (quality >= 40) {
            return 2;
        }
        return 1;
    }
}
