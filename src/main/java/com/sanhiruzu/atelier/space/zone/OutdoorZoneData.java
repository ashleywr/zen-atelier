package com.sanhiruzu.atelier.space.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OutdoorZoneData extends ZoneData {
    private final BlockPos samplePos;
    private final Map<String, Integer> furnitureCounts;
    private final Map<String, Integer> signalCounts;

    public OutdoorZoneData(UUID regionId, int volume, float enclosureScore, BlockPos samplePos) {
        this(regionId, volume, enclosureScore, samplePos, Map.of(), Map.of());
    }

    public OutdoorZoneData(UUID regionId, int volume, float enclosureScore, BlockPos samplePos, Map<String, Integer> furnitureCounts, Map<String, Integer> signalCounts) {
        super(regionId, volume, enclosureScore);
        this.samplePos = samplePos;
        this.furnitureCounts = new HashMap<>(furnitureCounts);
        this.signalCounts = new HashMap<>(signalCounts);
    }

    @Override
    public boolean isOutdoor() {
        return true;
    }

    public Holder<Biome> getBiome(Level level) {
        return level.getBiome(samplePos);
    }

    public float getTemperature(Level level) {
        return level.getBiome(samplePos).value().getBaseTemperature();
    }

    public boolean hasPrecipitation(Level level) {
        return level.getBiome(samplePos).value().hasPrecipitation();
    }

    public Map<String, Integer> getFurnitureCounts() {
        return Collections.unmodifiableMap(furnitureCounts);
    }

    public Map<String, Integer> getSignalCounts() {
        return Collections.unmodifiableMap(signalCounts);
    }
}
