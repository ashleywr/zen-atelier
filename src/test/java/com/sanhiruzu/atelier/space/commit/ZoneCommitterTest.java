package com.sanhiruzu.atelier.space.commit;

import com.sanhiruzu.atelier.space.ChunkClassificationData;
import com.sanhiruzu.atelier.space.ClassificationState;
import com.sanhiruzu.atelier.space.analyze.CandidateDecision;
import com.sanhiruzu.atelier.space.analyze.ZoneCandidate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneCommitterTest {

    private ZoneStore store;
    private SpaceRegionIndex index;
    private List<ChunkPos> synced;
    private ZoneCommitter committer;

    @BeforeEach
    void setUp() {
        store = new ZoneStore();
        index = new SpaceRegionIndex();
        synced = new ArrayList<>();
        committer = new ZoneCommitter(store, index, (pos, data) -> synced.add(pos));
    }

    /** A minimal ZoneCandidate covering chunk (0,0). Walkable position at world (5, 64, 5). */
    private ZoneCandidate candidate(long hash) {
        long packed = BlockPos.asLong(5, 64, 5);
        return new ZoneCandidate(
                hash,
                Set.of(packed),
                Set.of(new ChunkPos(0, 0)),
                1, 0,
                0.9, 0.1, 0.8,
                false,
                0, 64, 0, 15, 70, 15
        );
    }

    /** Build a walkable positions array for world pos (5, 64, 5). */
    private long[] walkableAt5_64_5() {
        return new long[]{ BlockPos.asLong(5, 64, 5) };
    }

    /** Build a CommittedZone already in the store with the given UUID and candidateHash. */
    private void preCommit(UUID id, long hash) {
        CommittedZone existing = new CommittedZone(
                id, CandidateDecision.ACCEPT_INDOOR, hash,
                Set.of(BlockPos.asLong(5, 64, 5)),
                Set.of(new ChunkPos(0, 0)),
                walkableAt5_64_5(),
                0, 64, 0, 15, 70, 15,
                "My Room"
        );
        store.commit(existing);
        index.register(existing);
    }

    @Test
    void commitAccepted_rejectLowConfidence_returnsNull() {
        ZoneCandidate c = candidate(1L);
        Map<ChunkPos, ChunkClassificationData> chunkData = new HashMap<>();
        chunkData.put(new ChunkPos(0, 0), new ChunkClassificationData());

        UUID result = committer.commitAccepted(c, CandidateDecision.REJECT_LOW_CONFIDENCE, null, walkableAt5_64_5(), chunkData);

        assertThat(result).isNull();
        assertThat(store.all()).isEmpty();
        assertThat(synced).isEmpty();
    }

    @Test
    void commitAccepted_rejectTooLargeOpenAir_returnsNull() {
        ZoneCandidate c = candidate(1L);
        Map<ChunkPos, ChunkClassificationData> chunkData = new HashMap<>();
        chunkData.put(new ChunkPos(0, 0), new ChunkClassificationData());

        UUID result = committer.commitAccepted(c, CandidateDecision.REJECT_TOO_LARGE_OPEN_AIR, null, walkableAt5_64_5(), chunkData);

        assertThat(result).isNull();
        assertThat(store.all()).isEmpty();
        assertThat(synced).isEmpty();
    }

    @Test
    void commitAccepted_newZone_createsAndMarksInside() {
        ZoneCandidate c = candidate(42L);
        ChunkClassificationData data = new ChunkClassificationData();
        Map<ChunkPos, ChunkClassificationData> chunkData = new HashMap<>();
        chunkData.put(new ChunkPos(0, 0), data);

        UUID result = committer.commitAccepted(c, CandidateDecision.ACCEPT_INDOOR, null, walkableAt5_64_5(), chunkData);

        assertThat(result).isNotNull();
        assertThat(store.get(result)).isNotNull();
        assertThat(store.get(result).candidateHash()).isEqualTo(42L);

        // Block (5,64,5) -> local (5,64,5) in chunk (0,0)
        assertThat(data.getBlockState(5, 64, 5)).isEqualTo(ClassificationState.INSIDE);

        assertThat(synced).containsExactly(new ChunkPos(0, 0));
        assertThat(index.getZoneIds(new ChunkPos(0, 0))).contains(result);
    }

    @Test
    void commitAccepted_sameHash_preservesUUID() {
        UUID existingId = UUID.randomUUID();
        preCommit(existingId, 99L);

        ZoneCandidate c = candidate(99L); // same hash
        ChunkClassificationData data = new ChunkClassificationData();
        Map<ChunkPos, ChunkClassificationData> chunkData = new HashMap<>();
        chunkData.put(new ChunkPos(0, 0), data);

        synced.clear(); // reset after preCommit

        UUID result = committer.commitAccepted(c, CandidateDecision.ACCEPT_INDOOR, existingId, walkableAt5_64_5(), chunkData);

        assertThat(result).isEqualTo(existingId);
        assertThat(store.get(existingId)).isNotNull();
        // Verify custom name is preserved
        assertThat(store.get(result).customName()).isEqualTo("My Room");
    }

    @Test
    void commitAccepted_differentHash_newUUID() {
        UUID existingId = UUID.randomUUID();
        preCommit(existingId, 10L);

        ZoneCandidate c = candidate(99L); // different hash
        ChunkClassificationData data = new ChunkClassificationData();
        Map<ChunkPos, ChunkClassificationData> chunkData = new HashMap<>();
        chunkData.put(new ChunkPos(0, 0), data);

        synced.clear();

        UUID result = committer.commitAccepted(c, CandidateDecision.ACCEPT_INDOOR, existingId, walkableAt5_64_5(), chunkData);

        assertThat(result).isNotNull();
        assertThat(result).isNotEqualTo(existingId);
        assertThat(store.get(result)).isNotNull();
    }

    @Test
    void dissolve_removesZoneAndClearsCells() {
        // First commit a zone
        ZoneCandidate c = candidate(7L);
        ChunkClassificationData data = new ChunkClassificationData();
        Map<ChunkPos, ChunkClassificationData> chunkData = new HashMap<>();
        chunkData.put(new ChunkPos(0, 0), data);

        UUID id = committer.commitAccepted(c, CandidateDecision.ACCEPT_INDOOR, null, walkableAt5_64_5(), chunkData);
        assertThat(id).isNotNull();
        assertThat(data.getBlockState(5, 64, 5)).isEqualTo(ClassificationState.INSIDE);

        synced.clear();

        committer.dissolve(id, Set.of(new ChunkPos(0, 0)), chunkData);

        assertThat(store.get(id)).isNull();
        assertThat(index.getZoneIds(new ChunkPos(0, 0))).doesNotContain(id);
        // Cell cleared back to SOLID
        assertThat(data.getBlockState(5, 64, 5)).isEqualTo(ClassificationState.SOLID);
        assertThat(synced).containsExactly(new ChunkPos(0, 0));
    }

    @Test
    void dissolve_nullZone_doesNothing() {
        UUID unknownId = UUID.randomUUID();
        Map<ChunkPos, ChunkClassificationData> chunkData = new HashMap<>();
        chunkData.put(new ChunkPos(0, 0), new ChunkClassificationData());

        // Should not throw
        committer.dissolve(unknownId, Set.of(new ChunkPos(0, 0)), chunkData);

        assertThat(synced).isEmpty();
    }
}
