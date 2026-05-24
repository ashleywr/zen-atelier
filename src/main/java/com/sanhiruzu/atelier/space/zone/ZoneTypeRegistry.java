package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.data.RoomProfile;
import com.sanhiruzu.atelier.data.RoomProfileRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public class ZoneTypeRegistry {
    private static final LinkedHashMap<ResourceLocation, ZoneType> TYPES = new LinkedHashMap<>();

    public static void register(ZoneType type) {
        TYPES.put(type.getId(), type);
    }

    // Rebuild from the data-driven RoomProfileRegistry. Called after a datapack reload so
    // edits to room_profiles/*.json take effect without a restart. Profiles with more
    // required_features sort first so the more specific match wins when several apply (a
    // room with brewing_stand+cauldron should resolve as atelier, not church).
    public static void rebuildFromProfiles() {
        TYPES.clear();
        List<RoomProfile> sorted = new ArrayList<>(RoomProfileRegistry.all());
        sorted.sort(Comparator
                .comparingInt((RoomProfile p) -> p.requiredFeatures().size()).reversed()
                .thenComparing(p -> p.id().toString()));
        for (RoomProfile profile : sorted) {
            TYPES.put(profile.id(), new ProfileZoneType(profile));
        }
    }

    @Nullable
    public static ResourceLocation match(RoomData room, Level level, BlockPos pos) {
        for (ZoneType type : TYPES.values()) {
            if (type.matches(room, level, pos)) {
                return type.getId();
            }
        }
        return null;
    }

    @Nullable
    public static ZoneType get(ResourceLocation id) {
        return TYPES.get(id);
    }
}
