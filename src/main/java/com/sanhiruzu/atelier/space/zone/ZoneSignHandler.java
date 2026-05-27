package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.space.SpaceRegion;
import com.sanhiruzu.atelier.space.SpaceRegionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Utility class for sign-based zone naming. Players can write a custom name on a sign
// placed next to a zone, then interact with the sign to apply the name to the zone.
@SuppressWarnings("SameReturnValue")
public class ZoneSignHandler {
    private static final String ZONE_ID_TAG = "atelier_zone_id";

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
        if (!(level.getBlockEntity(signPos) instanceof SignBlockEntity signEntity)) {
            return;
        }

        signEntity.getPersistentData().putString(ZONE_ID_TAG, adjacentZoneId.toString());
    }

    @SubscribeEvent
    public static void onUseSign(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK || event.getLevel().isClientSide()) {
            return;
        }
        if (event.getPlayer() == null) {
            return;
        }
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof SignBlockEntity signEntity)) {
            return;
        }

        boolean emptyHand = event.getItemStack().isEmpty();
        boolean explicitApply = emptyHand && event.getPlayer().isShiftKeyDown();
        boolean emptyHandOnWaxedSign = emptyHand && signEntity.isWaxed();
        if (!explicitApply && !emptyHandOnWaxedSign) {
            return;
        }

        boolean front = signEntity.isFacingFrontText(event.getPlayer());
        boolean filtered = event.getPlayer().isTextFilteringEnabled();
        if (applySignNameToZone(event.getLevel(), event.getPos(), front, filtered)) {
            event.cancelWithResult(ItemInteractionResult.SUCCESS);
        }
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
    public static boolean applySignNameToZone(Level level, BlockPos signPos) {
        return applySignNameToZone(level, signPos, true, false);
    }

    public static boolean applySignNameToZone(Level level, BlockPos signPos, boolean front, boolean filtered) {
        if (level.isClientSide()) return false;

        if (!(level.getBlockEntity(signPos) instanceof SignBlockEntity signEntity)) {
            return false;
        }

        String zoneIdStr = signEntity.getPersistentData().getString(ZONE_ID_TAG);
        if (zoneIdStr.isEmpty()) {
            UUID adjacentZoneId = findAdjacentZone(level, signPos);
            if (adjacentZoneId == null) return false;
            zoneIdStr = adjacentZoneId.toString();
            signEntity.getPersistentData().putString(ZONE_ID_TAG, zoneIdStr);
        }

        try {
            UUID zoneId = UUID.fromString(zoneIdStr);
            String signText = getSignText(signEntity, front, filtered);
            if (signText.isEmpty()) {
                signText = getSignText(signEntity, !front, filtered);
            }
            if (!signText.isEmpty()) {
                ZoneRegistry.get(level).setCustomName(zoneId, signText);
                return true;
            }
        } catch (IllegalArgumentException e) {
            // Invalid UUID stored, ignore
        }
        return false;
    }

    private static String getSignText(SignBlockEntity signEntity, boolean front, boolean filtered) {
        return readSignText(signEntity.getText(front), filtered);
    }

    static String readSignText(SignText signText, boolean filtered) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < SignText.LINES; i++) {
            String line = signText.getMessage(i, filtered).getString().trim().replaceAll("\\s+", " ");
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        return String.join(" ", lines);
    }
}
