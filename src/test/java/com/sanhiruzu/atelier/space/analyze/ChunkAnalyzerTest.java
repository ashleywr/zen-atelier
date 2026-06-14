package com.sanhiruzu.atelier.space.analyze;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ChunkAnalyzerTest {

    /** 3×3 room at y=64 (floor solid at y=63, air at 64 and 65). */
    private static ChunkSnapshotBundle roomBundle() {
        byte[] cells = new byte[ChunkSnapshotBundle.SIZE];
        for (int x = 5; x <= 7; x++) {
            for (int z = 5; z <= 7; z++) {
                cells[ChunkSnapshotBundle.indexOf(x, 64, z)] = ChunkSnapshotBundle.FLAG_AIR;
                cells[ChunkSnapshotBundle.indexOf(x, 65, z)] = ChunkSnapshotBundle.FLAG_AIR;
                // y=63 stays 0 (solid floor)
            }
        }
        return ChunkSnapshotBundle.forTest(cells, 0, 0, 1L);
    }

    @Test
    void singleRoomProducesOneMicroRegion() {
        List<MicroRegion> regions = ChunkAnalyzer.analyze(roomBundle());
        assertThat(regions).hasSize(1);
        assertThat(regions.get(0).walkableCount()).isEqualTo(9); // 3×3
    }

    @Test
    void emptyChunkProducesNoMicroRegions() {
        byte[] cells = new byte[ChunkSnapshotBundle.SIZE];
        var bundle = ChunkSnapshotBundle.forTest(cells, 0, 0, 1L);
        assertThat(ChunkAnalyzer.analyze(bundle)).isEmpty();
    }

    @Test
    void doorSeparatesTwoRoomsIntoTwoMicroRegions() {
        byte[] cells = new byte[ChunkSnapshotBundle.SIZE];
        // Room A: x=3..4, z=5
        for (int x = 3; x <= 4; x++) {
            cells[ChunkSnapshotBundle.indexOf(x, 64, 5)] = ChunkSnapshotBundle.FLAG_AIR;
            cells[ChunkSnapshotBundle.indexOf(x, 65, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        }
        // Door at x=5, z=5
        cells[ChunkSnapshotBundle.indexOf(5, 64, 5)] = ChunkSnapshotBundle.FLAG_ENTRY;
        // Room B: x=6..7, z=5
        for (int x = 6; x <= 7; x++) {
            cells[ChunkSnapshotBundle.indexOf(x, 64, 5)] = ChunkSnapshotBundle.FLAG_AIR;
            cells[ChunkSnapshotBundle.indexOf(x, 65, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        }
        var bundle = ChunkSnapshotBundle.forTest(cells, 0, 0, 1L);
        List<MicroRegion> regions = ChunkAnalyzer.analyze(bundle);
        assertThat(regions).hasSize(2);
    }

    @Test
    void walkableCellsOnChunkEdgeProduceBoundaryContacts() {
        byte[] cells = new byte[ChunkSnapshotBundle.SIZE];
        // Walkable at x=0 (WEST edge), y=64, z=5
        cells[ChunkSnapshotBundle.indexOf(0, 64, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        cells[ChunkSnapshotBundle.indexOf(0, 65, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        // y=63 solid (default 0)
        var bundle = ChunkSnapshotBundle.forTest(cells, 0, 0, 1L);
        List<MicroRegion> regions = ChunkAnalyzer.analyze(bundle);
        assertThat(regions).hasSize(1);
        assertThat(regions.get(0).boundaryContacts())
            .anyMatch(c -> c.face() == BoundaryContact.Face.WEST && c.y() == 64);
    }

    @Test
    void portalAdjacentBoundaryContactIsMarkedPortal() {
        byte[] cells = new byte[ChunkSnapshotBundle.SIZE];
        // Door at x=14; walkable at x=15 (EAST edge), y=64
        cells[ChunkSnapshotBundle.indexOf(14, 64, 5)] = ChunkSnapshotBundle.FLAG_ENTRY;
        cells[ChunkSnapshotBundle.indexOf(15, 64, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        cells[ChunkSnapshotBundle.indexOf(15, 65, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        var bundle = ChunkSnapshotBundle.forTest(cells, 0, 0, 1L);
        List<MicroRegion> regions = ChunkAnalyzer.analyze(bundle);
        assertThat(regions).hasSize(1);
        assertThat(regions.get(0).boundaryContacts())
            .anyMatch(c -> c.face() == BoundaryContact.Face.EAST && c.isPortal());
    }

    @Test
    void furnitureCellsAreCountedInRegion() {
        byte[] cells = new byte[ChunkSnapshotBundle.SIZE];
        // Walkable at x=5, y=64
        cells[ChunkSnapshotBundle.indexOf(5, 64, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        cells[ChunkSnapshotBundle.indexOf(5, 65, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        // Furniture adjacent at x=6, y=64
        cells[ChunkSnapshotBundle.indexOf(6, 64, 5)] = ChunkSnapshotBundle.FLAG_FURNITURE;
        var bundle = ChunkSnapshotBundle.forTest(cells, 0, 0, 1L);
        List<MicroRegion> regions = ChunkAnalyzer.analyze(bundle);
        assertThat(regions).hasSize(1);
        assertThat(regions.get(0).furnitureCount()).isEqualTo(1);
    }
}
