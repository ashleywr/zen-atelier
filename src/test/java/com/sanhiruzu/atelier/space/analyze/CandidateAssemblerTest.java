package com.sanhiruzu.atelier.space.analyze;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class CandidateAssemblerTest {

    /** Build a MicroRegion with the given boundary contacts and walkableCount. */
    private static MicroRegion region(int chunkX, int chunkZ, List<BoundaryContact> contacts, int walkable) {
        long key = ChunkAnalyzer.regionKeyForTest(chunkX, chunkZ, 5, 64, 5);
        return new MicroRegion(key, chunkX, chunkZ, new long[walkable],
                contacts, 0, walkable, 0, 0,
                chunkX * 16 + 5, 64, chunkZ * 16 + 5,
                chunkX * 16 + 7, 66, chunkZ * 16 + 7);
    }

    @Test
    void singleRegionBecomesOneCandidate() {
        var r = region(0, 0, List.of(), 9);
        var input = Map.of(new ChunkPos(0, 0), List.of(r));
        var candidates = CandidateAssembler.assemble(input);
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).totalWalkableCells()).isEqualTo(9);
    }

    @Test
    void twoRegionsWithOpenBoundaryMergeIntoOneCandidate() {
        // chunk(0,0) has EAST contact at (axisCoord=5, y=64, isPortal=false)
        // chunk(1,0) has WEST contact at (axisCoord=5, y=64, isPortal=false)
        var contactA = new BoundaryContact(BoundaryContact.Face.EAST, 5, 64, false,
                ChunkAnalyzer.regionKeyForTest(0, 0, 5, 64, 5));
        var contactB = new BoundaryContact(BoundaryContact.Face.WEST, 5, 64, false,
                ChunkAnalyzer.regionKeyForTest(1, 0, 5, 64, 5));
        var rA = region(0, 0, List.of(contactA), 9);
        var rB = region(1, 0, List.of(contactB), 6);
        var input = new LinkedHashMap<ChunkPos, List<MicroRegion>>();
        input.put(new ChunkPos(0, 0), List.of(rA));
        input.put(new ChunkPos(1, 0), List.of(rB));
        var candidates = CandidateAssembler.assemble(input);
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).totalWalkableCells()).isEqualTo(15);
    }

    @Test
    void twoRegionsWithPortalBoundaryStayAsTwoCandidates() {
        var contactA = new BoundaryContact(BoundaryContact.Face.EAST, 5, 64, true, // portal!
                ChunkAnalyzer.regionKeyForTest(0, 0, 5, 64, 5));
        var contactB = new BoundaryContact(BoundaryContact.Face.WEST, 5, 64, false,
                ChunkAnalyzer.regionKeyForTest(1, 0, 5, 64, 5));
        var rA = region(0, 0, List.of(contactA), 9);
        var rB = region(1, 0, List.of(contactB), 6);
        var input = new LinkedHashMap<ChunkPos, List<MicroRegion>>();
        input.put(new ChunkPos(0, 0), List.of(rA));
        input.put(new ChunkPos(1, 0), List.of(rB));
        var candidates = CandidateAssembler.assemble(input);
        assertThat(candidates).hasSize(2);
    }

    @Test
    void candidateHashIsStableForSameMemberKeys() {
        var r = region(0, 0, List.of(), 9);
        var input = Map.of(new ChunkPos(0, 0), List.of(r));
        long hash1 = CandidateAssembler.assemble(input).get(0).candidateHash();
        long hash2 = CandidateAssembler.assemble(input).get(0).candidateHash();
        assertThat(hash1).isEqualTo(hash2);
    }
}
