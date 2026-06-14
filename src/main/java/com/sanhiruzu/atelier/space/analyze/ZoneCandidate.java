package com.sanhiruzu.atelier.space.analyze;

import net.minecraft.world.level.ChunkPos;
import java.util.Set;

public record ZoneCandidate(
        long candidateHash,
        Set<Long> memberKeys,
        Set<ChunkPos> chunkPositions,
        int totalWalkableCells,
        int totalFurnitureCount,
        double shelterFraction,
        double naturalFraction,
        double playerBuiltFraction,
        boolean hasPortalAccess,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ
) {}
