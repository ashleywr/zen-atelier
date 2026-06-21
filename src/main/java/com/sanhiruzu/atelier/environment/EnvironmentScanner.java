package com.sanhiruzu.atelier.environment;

import com.sanhiruzu.atelier.api.EnvironmentAirHazard;
import com.sanhiruzu.atelier.api.EnvironmentSnapshot;
import com.sanhiruzu.atelier.api.EnvironmentTemperatureBand;
import com.sanhiruzu.atelier.data.Signals;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class EnvironmentScanner {
    public static final int MAX_RADIUS = 16;

    private EnvironmentScanner() {
    }

    public static EnvironmentSnapshot scan(Level level, BlockPos center, int radius, EnvironmentScanProfile profile) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        if (center == null) {
            throw new IllegalArgumentException("center must not be null");
        }
        if (profile == null) {
            throw new IllegalArgumentException("profile must not be null");
        }
        int clampedRadius = Math.max(0, Math.min(MAX_RADIUS, radius));
        Map<String, Integer> counts = countSignals(level, center, clampedRadius);
        boolean covered = isCovered(level, center, clampedRadius);
        boolean indoorsLike = covered && boundaryMass(counts) >= Math.max(8, clampedRadius * 3);
        boolean enclosed = indoorsLike && hasSideMass(level, center, clampedRadius);
        EnvironmentTemperatureBand band = temperatureBand(counts);
        Set<EnvironmentAirHazard> hazards = hazards(counts);
        Map<EntityType<?>, Integer> entities = profile == EnvironmentScanProfile.BLOCKS_AND_ENTITIES
                ? countEntities(level, center, clampedRadius)
                : Map.of();
        return new EnvironmentSnapshot(counts, covered, indoorsLike, enclosed, band, hazards, entities);
    }

    private static Map<String, Integer> countSignals(Level level, BlockPos center, int radius) {
        Map<String, Integer> counts = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            Signals.predicates().forEach((signal, predicate) -> {
                if (predicate.test(state)) {
                    counts.merge(signal, 1, Integer::sum);
                }
            });
            for (String signal : EnvironmentFacetSignals.signalsFor(state)) {
                counts.merge(signal, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static boolean isCovered(Level level, BlockPos center, int radius) {
        int height = Math.max(2, Math.min(8, radius + 2));
        BlockPos.MutableBlockPos cursor = center.mutable();
        for (int y = 1; y <= height; y++) {
            cursor.set(center.getX(), center.getY() + y, center.getZ());
            if (level.getBlockState(cursor).isSolidRender(level, cursor)) {
                return true;
            }
        }
        return false;
    }

    private static int boundaryMass(Map<String, Integer> counts) {
        return counts.getOrDefault("wood_materials", 0)
                + counts.getOrDefault("stone_or_metal_materials", 0)
                + counts.getOrDefault("rough_stone", 0)
                + counts.getOrDefault("urban_block", 0)
                + counts.getOrDefault("glass", 0);
    }

    private static boolean hasSideMass(Level level, BlockPos center, int radius) {
        if (radius <= 0) {
            return false;
        }
        return sideHasSolid(level, center, radius, 1, 0)
                && sideHasSolid(level, center, radius, -1, 0)
                && sideHasSolid(level, center, radius, 0, 1)
                && sideHasSolid(level, center, radius, 0, -1);
    }

    private static boolean sideHasSolid(Level level, BlockPos center, int radius, int dx, int dz) {
        BlockPos.MutableBlockPos cursor = center.mutable();
        int sideX = center.getX() + dx * radius;
        int sideZ = center.getZ() + dz * radius;
        for (int y = -1; y <= 2; y++) {
            for (int offset = -radius; offset <= radius; offset++) {
                int x = dx == 0 ? center.getX() + offset : sideX;
                int z = dz == 0 ? center.getZ() + offset : sideZ;
                cursor.set(x, center.getY() + y, z);
                if (level.getBlockState(cursor).isSolidRender(level, cursor)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static EnvironmentTemperatureBand temperatureBand(Map<String, Integer> counts) {
        int heat = counts.getOrDefault("heat_source", 0) + counts.getOrDefault("lava_heat", 0) * 3;
        int cold = counts.getOrDefault("ice_block", 0);
        int score = heat - cold;
        if (score <= -6) return EnvironmentTemperatureBand.COLD;
        if (score <= -2) return EnvironmentTemperatureBand.COOL;
        if (score >= 8) return EnvironmentTemperatureBand.HOT;
        if (score >= 3) return EnvironmentTemperatureBand.WARM;
        return EnvironmentTemperatureBand.PLEASANT;
    }

    private static Set<EnvironmentAirHazard> hazards(Map<String, Integer> counts) {
        EnumSet<EnvironmentAirHazard> hazards = EnumSet.noneOf(EnvironmentAirHazard.class);
        if (counts.getOrDefault("smoke_source", 0) > 0) hazards.add(EnvironmentAirHazard.SMOKY);
        if (counts.getOrDefault("lava_heat", 0) > 0) hazards.add(EnvironmentAirHazard.LAVA_HEAT);
        if (counts.getOrDefault("heat_source", 0) > 0) hazards.add(EnvironmentAirHazard.FIRE_PARTICLES);
        if (counts.getOrDefault("damp_source", 0) > 0) hazards.add(EnvironmentAirHazard.DAMP);
        if (counts.getOrDefault("dust_source", 0) > 0) hazards.add(EnvironmentAirHazard.DUSTY);
        return hazards;
    }

    private static Map<EntityType<?>, Integer> countEntities(Level level, BlockPos center, int radius) {
        Map<EntityType<?>, Integer> counts = new HashMap<>();
        AABB bounds = new AABB(center).inflate(radius);
        for (Entity entity : level.getEntities(null, bounds)) {
            counts.merge(entity.getType(), 1, Integer::sum);
        }
        return counts;
    }
}
