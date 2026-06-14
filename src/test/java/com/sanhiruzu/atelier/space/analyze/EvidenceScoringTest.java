package com.sanhiruzu.atelier.space.analyze;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class EvidenceScoringTest {

    private static ZoneCandidate candidate(int walkable, int furniture,
                                            double shelter, double natural,
                                            double playerBuilt, boolean hasPortal) {
        return new ZoneCandidate(1L, Set.of(1L), Set.of(new ChunkPos(0, 0)),
                walkable, furniture, shelter, natural, playerBuilt, hasPortal,
                0, 64, 0, 10, 70, 10);
    }

    @Test
    void smallShelterRoomWithDoorIsAcceptedIndoor() {
        // 50 walkable, 3 furniture, 90% shelter, 10% natural walls, 90% player-built, has portal
        var c = candidate(50, 3, 0.9, 0.1, 0.9, true);
        var score = EvidenceScorer.score(c);
        var decision = CandidateResolver.resolve(score, c);
        assertThat(decision).isEqualTo(CandidateDecision.ACCEPT_INDOOR);
    }

    @Test
    void openFieldWithNoDoorIsRejected() {
        // 5000 walkable, no shelter, no player-built, no portal
        var c = candidate(5000, 0, 0.0, 0.0, 0.0, false);
        var score = EvidenceScorer.score(c);
        var decision = CandidateResolver.resolve(score, c);
        assertThat(decision).isEqualTo(CandidateDecision.REJECT_TOO_LARGE_OPEN_AIR);
    }

    @Test
    void smallRoomWithoutDoorButWithFurnitureIsPending() {
        // Good shelter + player-built, some furniture, but NO portal access
        var c = candidate(20, 5, 0.85, 0.1, 0.9, false);
        var score = EvidenceScorer.score(c);
        var decision = CandidateResolver.resolve(score, c);
        assertThat(decision).isEqualTo(CandidateDecision.PENDING_STABILITY);
    }

    @Test
    void caveWithNoPlayerModificationIsRejected() {
        // High shelter (enclosed), all natural, no portal, no furniture
        var c = candidate(30, 0, 0.95, 1.0, 0.0, false);
        var score = EvidenceScorer.score(c);
        var decision = CandidateResolver.resolve(score, c);
        assertThat(decision).isEqualTo(CandidateDecision.REJECT_LOW_CONFIDENCE);
    }

    @Test
    void evidenceScoreHasPositiveTotalForValidRoom() {
        var c = candidate(30, 2, 0.9, 0.1, 0.9, true);
        var score = EvidenceScorer.score(c);
        assertThat(score.total()).isGreaterThan(0.0);
    }

    @Test
    void primaryReasonIsNotEmpty() {
        var c = candidate(30, 2, 0.9, 0.1, 0.9, true);
        var score = EvidenceScorer.score(c);
        assertThat(score.primaryReason()).isNotBlank();
    }
}
