package com.sanhiruzu.atelier.space.analyze;

import java.util.List;

/**
 * A connected walkable surface within one chunk.
 * walkablePositions entries are BlockPos.asLong() values.
 * key is derived from (chunkX, chunkZ, minX, minY, minZ) — stable across re-analyses of the same space.
 */
public record MicroRegion(
        long key,
        int chunkX, int chunkZ,
        long[] walkablePositions,
        List<BoundaryContact> boundaryContacts,
        int naturalBlockCount,
        int playerBuiltCount,
        int shelterCount,
        int furnitureCount,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ
) {
    public int walkableCount() { return walkablePositions.length; }
}
