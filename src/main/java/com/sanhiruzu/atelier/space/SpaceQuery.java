package com.sanhiruzu.atelier.space;

import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class SpaceQuery {

    /**
     * Returns the classification for any position, including solid blocks like furniture or beds.
     * <p>
     * For air blocks this is a direct lookup. For solid blocks the six face-neighbors are probed
     * (above first, then cardinal directions, then below) and the first non-solid air neighbor's
     * classification is returned. This lets callers pass a bed's BlockPos and get the classification
     * of the room the bed sits in rather than always getting SOLID.
     * <p>
     * Returns SOLID if no air neighbor has a resolved classification.
     */
    public static ClassificationState getClassificationAt(Level level, BlockPos pos) {
        net.minecraft.world.level.chunk.ChunkAccess chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return ClassificationState.SOLID;
        ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
        ClassificationState state = data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);

        if (state != ClassificationState.SOLID) return state;

        // Probe neighbors: above is checked first since it's the most natural "which room is this in"
        // for floor-placed objects like beds, chests, and crafting tables.
        for (BlockPos neighbor : List.of(
                pos.above(), pos.north(), pos.south(), pos.east(), pos.west(), pos.below())) {
            if (neighbor.getY() < -64 || neighbor.getY() >= 320) continue;
            if (!level.getBlockState(neighbor).isAir()) continue;
            ChunkClassificationData neighborData = ChunkClassificationAttachment.get(level.getChunk(neighbor));
            ClassificationState neighborState = neighborData.getBlockState(
                    neighbor.getX() & 15, neighbor.getY(), neighbor.getZ() & 15);
            if (neighborState != ClassificationState.SOLID) return neighborState;
        }

        return ClassificationState.SOLID;
    }

    public static boolean isInside(Level level, BlockPos pos) {
        return getClassificationAt(level, pos) == ClassificationState.INSIDE;
    }

    public static boolean isOutside(Level level, BlockPos pos) {
        return getClassificationAt(level, pos) == ClassificationState.OUTSIDE;
    }

    public static SpaceRegion getRegionAt(Level level, BlockPos pos) {
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        return registry.getRegionAt(level, pos);
    }

    public static ClassificationState getTypeAt(Level level, BlockPos pos) {
        SpaceRegion region = getRegionAt(level, pos);
        return region != null ? region.getType() : ClassificationState.PARTIAL;
    }

    public static int getVolumeAt(Level level, BlockPos pos) {
        SpaceRegion region = getRegionAt(level, pos);
        return region != null ? region.getVolume() : 0;
    }

    public static int getOpeningAreaAt(Level level, BlockPos pos) {
        SpaceRegion region = getRegionAt(level, pos);
        return region != null ? region.getOpeningArea() : 0;
    }

    public static float getEnclosureScoreAt(Level level, BlockPos pos) {
        SpaceRegion region = getRegionAt(level, pos);
        return region != null ? region.getEnclosureScore() : 0.0f;
    }

    @Nullable
    public static ZoneData getRoomAt(Level level, BlockPos pos) {
        if (level.isClientSide()) return null;
        SpaceRegion region = getRegionAt(level, pos);
        if (region == null) return null;
        return ZoneRegistry.get(level).getOrEvaluateRoom(region.getId(), level);
    }

    /**
     * @deprecated Use {@link #getRoomAt(Level, BlockPos)} for player-facing room data.
     */
    @Deprecated
    @Nullable
    public static ZoneData getZoneAt(Level level, BlockPos pos) {
        return getRoomAt(level, pos);
    }

    public static float getZoneQuality(Level level, BlockPos pos) {
        ZoneData zone = getRoomAt(level, pos);
        return zone instanceof RoomData room ? room.getQuality() : 0.0f;
    }
}
