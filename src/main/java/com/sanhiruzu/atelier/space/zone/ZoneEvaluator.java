package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.data.Signals;
import com.sanhiruzu.atelier.space.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.*;

public class ZoneEvaluator {

    public static RoomData computeRoomData(SpaceRegion region, Level level) {
        Set<BlockPos> mappedBlocks = SpaceRegionRegistry.get(level).getBlocksInRegion(region.getId());
        Zone zone = ZoneRegistry.get(level).getZoneById(region.getId());
        Set<BlockPos> airBlocks = zone != null ? zone.getInteriorBlocks() : mappedBlocks;

        // INSIDE positions are air; placed furniture got reclassified out of regionToBlocks.
        // Walk solid neighbors of the inside air instead, deduplicated, and drop anything that
        // also borders OUTSIDE air (walls/ceilings) so only interior blocks remain.
        Set<BlockPos> candidates = new HashSet<>();
        candidates.addAll(mappedBlocks);
        for (BlockPos pos : mappedBlocks) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                BlockState state = level.getBlockState(neighbor);
                if (!state.isAir() && !state.is(Blocks.WATER) && !state.is(Blocks.LAVA)) {
                    // Skip upper halves of multi-block structures to avoid double-counting.
                    // Beds: skip HEAD (count only FOOT). Doors: skip UPPER (count only LOWER).
                    if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD) continue;
                    if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER)
                        continue;
                    candidates.add(neighbor);
                }
            }
        }

        // The floor is one Y level below the lowest INSIDE air block.
        int floorY = airBlocks.stream().mapToInt(BlockPos::getY).min().orElse(Integer.MAX_VALUE) - 1;

        Map<String, Integer> furnitureCounts = new HashMap<>();
        Map<String, Integer> signalCounts = new HashMap<>();
        Map<String, Integer> surfaceCounts = new HashMap<>();
        for (BlockPos pos : candidates) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA)) continue;
            String key = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (bordersOutsideAir(pos, level) || pos.getY() == floorY) {
                surfaceCounts.merge(key, 1, Integer::sum);
            } else {
                furnitureCounts.merge(key, 1, Integer::sum);
                for (String signal : Signals.predicates().keySet()) {
                    if (Signals.matches(signal, state)) {
                        signalCounts.merge(signal, 1, Integer::sum);
                    }
                }
            }
        }

        // Use blocks.size() for volume — regionToBlocks is always accurate (updated by
        // mapBlockToRegion/unmapBlock on every furniture event), whereas SpaceRegion.volume
        // is only set at bootstrap and not updated on furniture placement/removal.
        int volume = airBlocks.size();
        float enclosureScore = volume == 0 ? 0f
                : 1f - ((float) region.getOpeningArea() / (float) (volume * 6));

        // Spaciousness: average interior height prorated against floor area.
        // floor_area = unique XZ positions occupied by INSIDE blocks.
        // spaciousness = volume / floor_area (1.0 = flat single layer, higher = taller room).
        Set<Long> floorXZ = new HashSet<>();
        for (BlockPos pos : airBlocks) {
            floorXZ.add((long) pos.getX() << 32 | (pos.getZ() & 0xFFFFFFFFL));
        }
        float spaciousness = floorXZ.isEmpty() ? 1f : (float) volume / floorXZ.size();

        QualityEvaluator.QualityBreakdown breakdown = QualityEvaluator.evaluate(volume, enclosureScore, furnitureCounts, signalCounts);
        RoomData room = new RoomData(region.getId(), volume, enclosureScore, furnitureCounts, signalCounts, surfaceCounts, breakdown.totalQuality);
        room.setQualityBreakdown(breakdown);
        room.setSpaciousness(spaciousness);
        room.setGeometryProfile(ZoneGeometryProfile.compute(airBlocks, level,
                zone != null ? Optional.ofNullable(zone.getMainEntry()) : Optional.empty()));
        return room;
    }

    public static OutdoorZoneData computeOutdoorZoneData(SpaceRegion region, Level level, BlockPos samplePos) {
        int volume = SpaceRegionRegistry.get(level).getBlocksInRegion(region.getId()).size();
        float enclosure = volume == 0 ? 0f
                : 1f - ((float) region.getOpeningArea() / (float) (volume * 6));
        return new OutdoorZoneData(region.getId(), volume, enclosure, samplePos);
    }

    public static OutdoorZoneData computeOutdoorZoneDataWithFurniture(SpaceRegion region, RoomData room, Level level, BlockPos samplePos) {
        int volume = SpaceRegionRegistry.get(level).getBlocksInRegion(region.getId()).size();
        float enclosure = volume == 0 ? 0f
                : 1f - ((float) region.getOpeningArea() / (float) (volume * 6));
        return new OutdoorZoneData(region.getId(), volume, enclosure, samplePos, room.getFurnitureCounts(), room.getSignalCounts());
    }

    private static boolean bordersOutsideAir(BlockPos pos, Level level) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            ChunkAccess chunk = level.getChunk(neighbor);
            ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
            int localX = neighbor.getX() & 15;
            int localZ = neighbor.getZ() & 15;
            if (data.getBlockState(localX, neighbor.getY(), localZ) == ClassificationState.OUTSIDE) {
                return true;
            }
        }
        return false;
    }

}
