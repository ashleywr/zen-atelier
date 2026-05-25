package com.sanhiruzu.atelier.space;

import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;

import java.util.Collection;
import java.util.Optional;

public record BlockPosBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static Optional<BlockPosBounds> enclosing(Collection<BlockPos> positions) {
        if (positions.isEmpty()) return Optional.empty();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return Optional.of(new BlockPosBounds(minX, minY, minZ, maxX, maxY, maxZ));
    }

    public BlockPosBounds includeAll(Collection<BlockPos> positions) {
        BlockPosBounds bounds = this;
        for (BlockPos pos : positions) {
            bounds = bounds.include(pos);
        }
        return bounds;
    }

    public BlockPosBounds include(BlockPos pos) {
        return new BlockPosBounds(
                Math.min(minX, pos.getX()),
                Math.min(minY, pos.getY()),
                Math.min(minZ, pos.getZ()),
                Math.max(maxX, pos.getX()),
                Math.max(maxY, pos.getY()),
                Math.max(maxZ, pos.getZ())
        );
    }

    public BlockPosBounds inflate(int margin) {
        return new BlockPosBounds(
                minX - margin, minY - margin, minZ - margin,
                maxX + margin, maxY + margin, maxZ + margin
        );
    }

    public BlockPos minCorner() {
        return new BlockPos(minX, minY, minZ);
    }

    public Vec3i size() {
        return new Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
    }

    public boolean overlaps(ChunkPos chunkPos) {
        return maxX >= chunkPos.getMinBlockX()
                && minX <= chunkPos.getMaxBlockX()
                && maxZ >= chunkPos.getMinBlockZ()
                && minZ <= chunkPos.getMaxBlockZ();
    }

    public void applyTo(ZoneData zone) {
        zone.setSpatialExtent(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
