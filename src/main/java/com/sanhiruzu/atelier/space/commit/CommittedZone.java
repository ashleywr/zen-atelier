package com.sanhiruzu.atelier.space.commit;

import com.sanhiruzu.atelier.space.analyze.CandidateDecision;
import com.sanhiruzu.atelier.space.analyze.EvidenceScore;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

public record CommittedZone(
        UUID uuid,
        CandidateDecision kind,
        long candidateHash,
        EvidenceScore score,
        Set<Long> memberKeys,
        Set<ChunkPos> chunkPositions,
        long[] walkablePositions,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ,
        @Nullable String customName
) {
    /** Quality in [0,1] derived from evidence total, capped at practical max of 13. */
    public float quality() {
        return (float) Math.min(1.0, score.total() / 13.0);
    }

    /** Enclosure proxy in [0,1] derived from the shelter fraction. */
    public float enclosureScore() {
        return (float) Math.min(1.0, score.shelterScore() / 3.0);
    }
}
