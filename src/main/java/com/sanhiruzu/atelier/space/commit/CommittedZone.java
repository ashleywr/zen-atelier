package com.sanhiruzu.atelier.space.commit;

import com.sanhiruzu.atelier.space.analyze.CandidateDecision;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

public record CommittedZone(
        UUID uuid,
        CandidateDecision kind,
        long candidateHash,
        Set<Long> memberKeys,
        Set<ChunkPos> chunkPositions,
        long[] walkablePositions,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ,
        @Nullable String customName
) {}
