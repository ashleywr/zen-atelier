package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.space.SpaceRegion;
import com.sanhiruzu.atelier.space.SpaceRegionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.util.UUID;

// Utility class for sign-based zone naming. Players can write a custom name on a sign
// placed next to a zone, then interact with the sign to apply the name to the zone.
public class ZoneSignHandler {

    @SubscribeEvent
    public static void onSignPlace(BlockEvent.EntityPlaceEvent event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof SignBlock)) {
            return;
        }

        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        BlockPos signPos = event.getPos();
        UUID adjacentZoneId = findAdjacentZone(level, signPos);
        if (adjacentZoneId == null) return;

        // Store zone UUID on the sign entity for later reference
        SignBlockEntity signEntity = (SignBlockEntity) level.getBlockEntity(signPos);
        if (signEntity == null) return;

        signEntity.getPersistentData().putString("atelier_zone_id", adjacentZoneId.toString());
    }

    @Nullable
    public static UUID findAdjacentZone(Level level, BlockPos signPos) {
        // Check all 6 directions for an adjacent zone
        for (Direction dir : Direction.values()) {
            BlockPos checkPos = signPos.relative(dir);
            SpaceRegion region = SpaceRegionRegistry.get(level).getRegionAt(level, checkPos);
            if (region != null) {
                return region.getId();
            }
        }
        return null;
    }

    // Called when a player interacts with a sign that's adjacent to a zone
    // Reads the sign text and applies it as the zone's custom name
    public static void applySignNameToZone(Level level, BlockPos signPos) {
        if (level.isClientSide()) return;

        SignBlockEntity signEntity = (SignBlockEntity) level.getBlockEntity(signPos);
        if (signEntity == null) return;

        String zoneIdStr = signEntity.getPersistentData().getString("atelier_zone_id");
        if (zoneIdStr.isEmpty()) return;

        try {
            UUID zoneId = UUID.fromString(zoneIdStr);
            String signText = getSignText(signEntity);
            if (!signText.isEmpty()) {
                ZoneRegistry.get(level).setCustomName(zoneId, signText);
            }
        } catch (IllegalArgumentException e) {
            // Invalid UUID stored, ignore
        }
    }

    @Nullable
    private static String getSignText(SignBlockEntity signEntity) {
        // Sign text reading is API-dependent on MC version - simplified for now
        // TODO: Implement proper sign text reading once sign API is confirmed for 1.21.1
        return null;
    }
}
