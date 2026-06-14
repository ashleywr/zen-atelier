package com.sanhiruzu.atelier.space;

import com.sanhiruzu.atelier.space.analyze.CandidateDecision;
import com.sanhiruzu.atelier.space.commit.CommittedZone;
import com.sanhiruzu.atelier.space.zone.OutdoorZoneData;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class SpaceQuery {
    private static final Direction[] ROOM_PROBE_DIRECTIONS = {
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST,
            Direction.DOWN
    };

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

        ClassificationState neighborState = firstContainingRoomNeighbor(
                pos,
                neighbor -> level.getBlockState(neighbor).isAir(),
                neighbor -> {
                    ChunkClassificationData neighborData = ChunkClassificationAttachment.get(level.getChunk(neighbor));
                    ClassificationState stateAtNeighbor = neighborData.getBlockState(
                            neighbor.getX() & 15, neighbor.getY(), neighbor.getZ() & 15);
                    return stateAtNeighbor == ClassificationState.SOLID ? null : stateAtNeighbor;
                }
        );

        return neighborState == null ? ClassificationState.SOLID : neighborState;
    }

    public static boolean isInside(Level level, BlockPos pos) {
        return getClassificationAt(level, pos) == ClassificationState.INSIDE;
    }

    public static boolean isOutside(Level level, BlockPos pos) {
        return getClassificationAt(level, pos) == ClassificationState.OUTSIDE;
    }

    public static SpaceRegion getRegionAt(Level level, BlockPos pos) {
        return getRegionContaining(level, pos);
    }

    @Nullable
    public static SpaceRegion getRegionContaining(Level level, BlockPos pos) {
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        SpaceRegion region = registry.getRegionAt(level, pos);
        if (region != null) return region;

        net.minecraft.world.level.chunk.ChunkAccess chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return null;
        ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
        ClassificationState state = data.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
        if (state != ClassificationState.SOLID) return null;

        return firstContainingRoomNeighbor(
                pos,
                neighbor -> level.getBlockState(neighbor).isAir(),
                neighbor -> registry.getRegionAt(level, neighbor)
        );
    }

    @Nullable
    static <T> T firstContainingRoomNeighbor(
            BlockPos pos,
            Predicate<BlockPos> canUseNeighbor,
            Function<BlockPos, T> lookup
    ) {
        for (Direction direction : ROOM_PROBE_DIRECTIONS) {
            BlockPos neighbor = pos.relative(direction);
            if (neighbor.getY() < -64 || neighbor.getY() >= 320) continue;
            if (!canUseNeighbor.test(neighbor)) continue;
            T value = lookup.apply(neighbor);
            if (value != null) return value;
        }
        return null;
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
        return getRoomContaining(level, pos);
    }

    /**
     * Returns the room containing {@code pos}, including when {@code pos} is a solid block placed in
     * the room. This is the right lookup for furniture, workstations, storage, beds, and item/block
     * positions that occupy room space instead of being air cells.
     *
     * Falls back to the new candidate pipeline when the old SpaceRegionRegistry has no entry.
     */
    @Nullable
    public static ZoneData getRoomContaining(Level level, BlockPos pos) {
        if (level.isClientSide()) return null;
        SpaceRegion region = getRegionContaining(level, pos);
        if (region != null) return ZoneRegistry.get(level).getOrEvaluateRoom(region.getId(), level);

        if (level instanceof ServerLevel serverLevel) {
            CommittedZone committed = ZoneRegistry.get(level).getCommittedZoneAt(pos, serverLevel);
            if (committed != null) return zoneDataFromCommitted(committed, pos, serverLevel);
        }
        return null;
    }

    private static ZoneData zoneDataFromCommitted(CommittedZone zone, BlockPos pos, ServerLevel level) {
        // Prefer fully-evaluated RoomData from the eval cache
        RoomData evaluated = ClassificationTickHandler.getEvalCache(level).get(zone.uuid());
        if (evaluated != null) return evaluated;

        // Fallback: minimal stub until evaluation runs
        int volume = zone.walkablePositions().length;
        if (zone.kind() == CandidateDecision.ACCEPT_OUTDOOR_FUNCTIONAL) {
            OutdoorZoneData outdoor = new OutdoorZoneData(zone.uuid(), volume, zone.enclosureScore(), pos);
            outdoor.setSpatialExtent(zone.minX(), zone.minY(), zone.minZ(), zone.maxX(), zone.maxY(), zone.maxZ());
            return outdoor;
        }
        RoomData room = new RoomData(zone.uuid(), volume, zone.enclosureScore(), Map.of(), zone.quality());
        room.setSpatialExtent(zone.minX(), zone.minY(), zone.minZ(), zone.maxX(), zone.maxY(), zone.maxZ());
        return room;
    }

    @Nullable
    public static ZoneData getIndoorRoomContaining(Level level, BlockPos pos) {
        ZoneData room = getRoomContaining(level, pos);
        if (room == null || room.isOutdoor() || !room.hasSpatialExtent()) {
            return null;
        }
        return getClassificationAt(level, pos) == ClassificationState.INSIDE ? room : null;
    }

    public static boolean isInsideRoom(Level level, BlockPos pos) {
        return getIndoorRoomContaining(level, pos) != null;
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
