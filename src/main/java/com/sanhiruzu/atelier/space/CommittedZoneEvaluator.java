package com.sanhiruzu.atelier.space;

import com.sanhiruzu.atelier.data.RoomProfile;
import com.sanhiruzu.atelier.data.RoomProfileRegistry;
import com.sanhiruzu.atelier.data.Signals;
import com.sanhiruzu.atelier.space.commit.CommittedZone;
import com.sanhiruzu.atelier.space.zone.QualityEvaluator;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneType;
import com.sanhiruzu.atelier.space.zone.ZoneTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates a committed zone against room profiles using the live world.
 * Must run on the server thread. Produces a full {@link RoomData} with
 * signal counts, a matched zone type, and a profile-aware quality score.
 */
public final class CommittedZoneEvaluator {
    private static final float INDOOR_ENCLOSURE_THRESHOLD = 0.85f;

    private CommittedZoneEvaluator() {}

    public static RoomData evaluate(CommittedZone zone, ServerLevel level) {
        Set<BlockPos> airBlocks = new HashSet<>(zone.walkablePositions().length);
        int minY = Integer.MAX_VALUE;
        for (long packed : zone.walkablePositions()) {
            BlockPos pos = BlockPos.of(packed);
            airBlocks.add(pos);
            if (pos.getY() < minY) minY = pos.getY();
        }

        int floorY = (minY == Integer.MAX_VALUE) ? 0 : minY - 1;

        Map<String, Integer> furnitureCounts = new HashMap<>();
        Map<String, Integer> signalCounts = new HashMap<>();
        // Track visited positions so each solid neighbor is only processed once
        Set<BlockPos> visited = new HashSet<>(airBlocks);

        for (BlockPos pos : airBlocks) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (!visited.add(neighbor)) continue;
                BlockState state = level.getBlockState(neighbor);
                if (state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA)) continue;
                // Skip upper halves of multi-block structures
                if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD) continue;
                if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) continue;
                // Blocks at or below floor level are floor/surface, not furniture
                if (neighbor.getY() <= floorY) continue;
                String key = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                furnitureCounts.merge(key, 1, Integer::sum);
                for (String signal : Signals.predicates().keySet()) {
                    if (Signals.matches(signal, state)) {
                        signalCounts.merge(signal, 1, Integer::sum);
                    }
                }
            }
        }

        // Water adjacency signal
        Set<BlockPos> waterSeen = new HashSet<>();
        int waterCount = 0;
        for (BlockPos pos : airBlocks) {
            for (Direction dir : Direction.values()) {
                BlockPos waterPos = pos.relative(dir);
                if (waterSeen.add(waterPos) && level.getBlockState(waterPos).is(Blocks.WATER)) {
                    waterCount++;
                }
            }
        }
        if (waterCount > 0) signalCounts.put("water_coverage", waterCount);

        int volume = airBlocks.isEmpty() ? zone.walkablePositions().length : airBlocks.size();
        float enclosureScore = zone.enclosureScore();
        int lightLevel = computeLightLevel(airBlocks, level);
        BlockPos samplePos = airBlocks.isEmpty()
                ? new BlockPos(zone.minX(), zone.minY(), zone.minZ())
                : airBlocks.iterator().next();

        // Profile matching uses a probe with signal counts
        RoomData probe = new RoomData(zone.uuid(), volume, enclosureScore,
                furnitureCounts, signalCounts, Map.of(), 0f, lightLevel);
        ResourceLocation zoneTypeId = ZoneTypeRegistry.match(probe, level, samplePos);
        RoomProfile profile = zoneTypeId != null ? RoomProfileRegistry.get(zoneTypeId) : null;

        // Quality evaluation — two-pass: generic first, then profile-aware
        QualityEvaluator.QualityBreakdown breakdown = QualityEvaluator.evaluate(
                volume, enclosureScore, furnitureCounts, signalCounts, lightLevel, profile);

        RoomData room = new RoomData(zone.uuid(), volume, enclosureScore,
                furnitureCounts, signalCounts, Map.of(), breakdown.totalQuality, lightLevel);
        room.setZoneTypeId(zoneTypeId);
        room.setQualityBreakdown(breakdown);
        room.setSpatialExtent(zone.minX(), zone.minY(), zone.minZ(),
                zone.maxX(), zone.maxY(), zone.maxZ());

        if (zoneTypeId != null) {
            ZoneType type = ZoneTypeRegistry.get(zoneTypeId);
            if (type != null && enclosureScore < type.minimumEnclosureScore()) {
                room.setDegraded(true);
            }
        } else if (enclosureScore < INDOOR_ENCLOSURE_THRESHOLD) {
            room.setDegraded(true);
        }

        return room;
    }

    private static int computeLightLevel(Set<BlockPos> airBlocks, ServerLevel level) {
        if (airBlocks.isEmpty()) return -1;
        int min = 15;
        for (BlockPos pos : airBlocks) {
            min = Math.min(min, level.getMaxLocalRawBrightness(pos));
        }
        return min;
    }
}
