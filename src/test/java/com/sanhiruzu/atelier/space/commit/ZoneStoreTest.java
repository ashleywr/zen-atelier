package com.sanhiruzu.atelier.space.commit;

import com.sanhiruzu.atelier.space.analyze.CandidateDecision;
import com.sanhiruzu.atelier.space.analyze.EvidenceScore;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneStoreTest {
    private ZoneStore store;
    private SpaceRegionIndex index;

    @BeforeEach
    void setUp() {
        store = new ZoneStore();
        index = new SpaceRegionIndex();
    }

    private static final EvidenceScore TEST_SCORE = new EvidenceScore(2.0, 2.5, 1.2, 2.0, 2.0, 1.5, 0, 0, "test");

    private CommittedZone zone(UUID id, ChunkPos... chunks) {
        Set<ChunkPos> chunkSet = new LinkedHashSet<>(Arrays.asList(chunks));
        return new CommittedZone(id, CandidateDecision.ACCEPT_INDOOR, 12345L,
                TEST_SCORE, Set.of(1L), chunkSet, new long[]{},
                0, 64, 0, 15, 70, 15, null);
    }

    @Test
    void committedZoneIsRetrievable() {
        UUID id = UUID.randomUUID();
        var z = zone(id, new ChunkPos(0, 0));
        store.commit(z);
        index.register(z);
        assertThat(store.get(id)).isEqualTo(z);
    }

    @Test
    void removedZoneIsGone() {
        UUID id = UUID.randomUUID();
        var z = zone(id, new ChunkPos(0, 0));
        store.commit(z);
        index.register(z);
        store.remove(id);
        index.remove(id, z.chunkPositions());
        assertThat(store.get(id)).isNull();
        assertThat(index.getZoneIds(new ChunkPos(0, 0))).doesNotContain(id);
    }

    @Test
    void indexReturnsZoneUUIDsForChunk() {
        UUID id1 = UUID.randomUUID(), id2 = UUID.randomUUID();
        var z1 = zone(id1, new ChunkPos(0, 0));
        var z2 = zone(id2, new ChunkPos(0, 0), new ChunkPos(1, 0));
        store.commit(z1); index.register(z1);
        store.commit(z2); index.register(z2);
        assertThat(index.getZoneIds(new ChunkPos(0, 0))).containsExactlyInAnyOrder(id1, id2);
    }

    @Test
    void customNameSurvivesUpdate() {
        UUID id = UUID.randomUUID();
        var z = zone(id, new ChunkPos(0, 0));
        store.commit(z);
        store.setCustomName(id, "My Room");
        assertThat(store.getCustomName(id)).isEqualTo("My Room");
    }

    @Test
    void removeAlsoClearsCustomName() {
        UUID id = UUID.randomUUID();
        var z = zone(id, new ChunkPos(0, 0));
        store.commit(z);
        store.setCustomName(id, "My Room");
        store.remove(id);
        assertThat(store.getCustomName(id)).isNull();
    }

    @Test
    void customNamePreservedOnRecommit() {
        UUID id = UUID.randomUUID();
        var z = zone(id, new ChunkPos(0, 0));
        store.commit(z);
        store.setCustomName(id, "Library");
        // Re-commit same zone without customName — should preserve "Library"
        store.commit(z); // z has customName=null
        assertThat(store.getCustomName(id)).isEqualTo("Library");
    }

    @Test
    void transferCustomNameMovesName() {
        UUID from = UUID.randomUUID(), to = UUID.randomUUID();
        var z1 = zone(from, new ChunkPos(0, 0));
        var z2 = zone(to, new ChunkPos(0, 0));
        store.commit(z1); store.commit(z2);
        store.setCustomName(from, "Smithy");
        store.transferCustomName(from, to);
        assertThat(store.getCustomName(from)).isNull();
        assertThat(store.getCustomName(to)).isEqualTo("Smithy");
    }
}
