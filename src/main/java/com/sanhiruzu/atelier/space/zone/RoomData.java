package com.sanhiruzu.atelier.space.zone;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoomData extends ZoneData {
    private final Map<String, Integer> furnitureCounts;
    private final Map<String, Integer> signalCounts;
    private final Map<String, Integer> surfaceCounts;
    private final float quality;
    @Nullable
    private QualityEvaluator.QualityBreakdown qualityBreakdown;
    @Nullable
    private ResourceLocation zoneTypeId;
    // True when the matched zone type's minimumEnclosureScore is not met — the room is
    // open enough to count as outdoors but still carries the type marker (e.g. an open-air
    // bedroom still has a bed, so the bed should give degraded sleep benefits).
    private boolean degraded;
    @Nullable
    private String epithetName;
    @Nullable
    private String generatedName;
    @Nullable
    private ZoneGeometryProfile geometryProfile;
    private float spaciousness = 1f;

    public RoomData(UUID regionId, int volume, float enclosureScore, Map<String, Integer> furnitureCounts, float quality) {
        this(regionId, volume, enclosureScore, furnitureCounts, Map.of(), Map.of(), quality);
    }

    public RoomData(UUID regionId, int volume, float enclosureScore, Map<String, Integer> furnitureCounts, Map<String, Integer> signalCounts, float quality) {
        this(regionId, volume, enclosureScore, furnitureCounts, signalCounts, Map.of(), quality);
    }

    public RoomData(UUID regionId, int volume, float enclosureScore, Map<String, Integer> furnitureCounts, Map<String, Integer> signalCounts, Map<String, Integer> surfaceCounts, float quality) {
        super(regionId, volume, enclosureScore);
        this.furnitureCounts = new HashMap<>(furnitureCounts);
        this.signalCounts = new HashMap<>(signalCounts);
        this.surfaceCounts = new HashMap<>(surfaceCounts);
        this.quality = quality;
        this.zoneTypeId = null;
        this.degraded = false;
    }

    @Override
    public boolean isOutdoor() {
        return false;
    }

    public Map<String, Integer> getFurnitureCounts() {
        return Collections.unmodifiableMap(furnitureCounts);
    }

    public Map<String, Integer> getSignalCounts() {
        return Collections.unmodifiableMap(signalCounts);
    }

    public Map<String, Integer> getSurfaceCounts() {
        return Collections.unmodifiableMap(surfaceCounts);
    }

    public float getQuality() {
        return quality;
    }

    @Nullable
    public ResourceLocation getZoneTypeId() {
        return zoneTypeId;
    }

    public void setZoneTypeId(@Nullable ResourceLocation zoneTypeId) {
        this.zoneTypeId = zoneTypeId;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    @Nullable
    public QualityEvaluator.QualityBreakdown getQualityBreakdown() {
        return qualityBreakdown;
    }

    public void setQualityBreakdown(@Nullable QualityEvaluator.QualityBreakdown breakdown) {
        this.qualityBreakdown = breakdown;
    }

    @Nullable
    public String getEpithetName() {
        return epithetName;
    }

    public void setEpithetName(@Nullable String epithetName) {
        this.epithetName = epithetName;
    }

    @Nullable
    public String getGeneratedName() {
        return generatedName;
    }

    public void setGeneratedName(@Nullable String generatedName) {
        this.generatedName = generatedName;
    }

    public float getSpaciousness() {
        return spaciousness;
    }

    public void setSpaciousness(float spaciousness) {
        this.spaciousness = spaciousness;
    }

    @Nullable
    public ZoneGeometryProfile getGeometryProfile() {
        return geometryProfile;
    }

    public void setGeometryProfile(@Nullable ZoneGeometryProfile geometryProfile) {
        this.geometryProfile = geometryProfile;
    }
}
