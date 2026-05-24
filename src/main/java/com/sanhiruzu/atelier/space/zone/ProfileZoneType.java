package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.data.RequiredFeature;
import com.sanhiruzu.atelier.data.RequiredSurface;
import com.sanhiruzu.atelier.data.RoomProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class ProfileZoneType extends ZoneType {
    private final RoomProfile profile;

    public ProfileZoneType(RoomProfile profile) {
        this.profile = profile;
    }

    public RoomProfile profile() {
        return profile;
    }

    @Override
    public ResourceLocation getId() {
        return profile.id();
    }

    @Override
    public boolean matches(RoomData room, Level level, BlockPos pos) {
        for (RequiredFeature feature : profile.requiredFeatures()) {
            int count = room.getSignalCounts().getOrDefault(feature.signal(), 0);
            if (count < feature.minimum()) return false;
        }
        for (RequiredSurface surf : profile.requiredSurfaces()) {
            if (!checkSurfaceRequirement(room, surf)) return false;
        }
        return true;
    }

    private boolean checkSurfaceRequirement(RoomData room, RequiredSurface surf) {
        int count = 0;
        String spec = surf.block();
        for (var entry : room.getSurfaceCounts().entrySet()) {
            if (spec.startsWith("#")) {
                TagKey<Block> tag = TagKey.create(Registries.BLOCK,
                        ResourceLocation.parse(spec.substring(1)));
                Block block = BuiltInRegistries.BLOCK.get(
                        ResourceLocation.parse(entry.getKey()));
                if (block != null && block.defaultBlockState().is(tag)) {
                    count += entry.getValue();
                }
            } else {
                if (entry.getKey().equals(spec)) {
                    count += entry.getValue();
                }
            }
        }
        return count >= surf.minimum();
    }

    @Override
    public float qualityMultiplier() {
        return 1.0f;
    }
}
