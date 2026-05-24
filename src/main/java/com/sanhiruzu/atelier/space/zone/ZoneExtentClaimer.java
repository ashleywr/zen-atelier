package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.space.ChunkClassificationAttachment;
import com.sanhiruzu.atelier.space.ChunkClassificationData;
import com.sanhiruzu.atelier.space.ClassificationState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Handles claiming zone extents by updating ChunkClassificationData to mark interior
 * blocks as INSIDE and boundary blocks as PARTIAL. This protects expanded zones from
 * being reclassified by the chunk classifier.
 */
public class ZoneExtentClaimer {
    private static final int[] DX = {0, 0, 0, 0, 1, -1};
    private static final int[] DY = {1, -1, 0, 0, 0, 0};
    private static final int[] DZ = {0, 0, 1, -1, 0, 0};

    private final ServerLevel level;

    public ZoneExtentClaimer(ServerLevel level) {
        this.level = level;
    }

    /**
     * Claims a zone's spatial extent by marking interior air blocks as INSIDE
     * and boundary air blocks as PARTIAL in all affected chunks.
     */
    public void claimExtent(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        // Collect all chunks that will be affected
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int chunkX = x >> 4;
                int chunkZ = z >> 4;
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
                ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);

                // Mark blocks within this chunk's portion of the extent
                for (int y = minY; y <= maxY; y++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        continue; // Skip solid blocks
                    }

                    ClassificationState state = determineState(x, y, z, minX, minY, minZ, maxX, maxY, maxZ);
                    int lx = x & 15;
                    int lz = z & 15;
                    data.setBlockState(lx, y, lz, state);
                }
            }
        }
    }

    /**
     * Determines whether an air block should be INSIDE or PARTIAL based on whether
     * it has neighbors outside the zone extent.
     */
    private ClassificationState determineState(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        // Check all 6 neighbors
        for (int i = 0; i < 6; i++) {
            int nx = x + DX[i];
            int ny = y + DY[i];
            int nz = z + DZ[i];

            // If neighbor is outside zone bounds, this is a boundary block
            if (nx < minX || nx > maxX || ny < minY || ny > maxY || nz < minZ || nz > maxZ) {
                return ClassificationState.PARTIAL;
            }
        }

        // All neighbors are within zone → interior block
        return ClassificationState.INSIDE;
    }
}
