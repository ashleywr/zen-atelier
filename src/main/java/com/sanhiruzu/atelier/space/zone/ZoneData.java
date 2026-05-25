package com.sanhiruzu.atelier.space.zone;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public abstract class ZoneData {
    private final UUID regionId;
    private final int volume;
    private final float enclosureScore;
    private boolean hasExtent = false;
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private boolean disabled = false;
    private long disabledAtGameTime = -1L;
    private boolean initialized = false;

    protected ZoneData(UUID regionId, int volume, float enclosureScore) {
        this.regionId = regionId;
        this.volume = volume;
        this.enclosureScore = enclosureScore;
    }

    public UUID getRegionId() {
        return regionId;
    }

    public int getVolume() {
        return volume;
    }

    public float getEnclosureScore() {
        return enclosureScore;
    }

    public abstract boolean isOutdoor();

    public void setSpatialExtent(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.hasExtent = true;
    }

    public boolean contains(BlockPos pos) {
        return hasExtent
                && pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public boolean hasSpatialExtent() {
        return hasExtent;
    }

    public void setDisabled(boolean disabled, long gameTime) {
        this.disabled = disabled;
        if (disabled) {
            this.disabledAtGameTime = gameTime;
        } else {
            this.disabledAtGameTime = -1L;
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isExpired(long currentGameTime, long gracePeriodTicks) {
        return disabled && disabledAtGameTime >= 0 && (currentGameTime - disabledAtGameTime) >= gracePeriodTicks;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isInitialized() {
        return initialized;
    }
}