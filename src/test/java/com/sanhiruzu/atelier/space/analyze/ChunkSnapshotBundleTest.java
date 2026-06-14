package com.sanhiruzu.atelier.space.analyze;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChunkSnapshotBundleTest {

    private static ChunkSnapshotBundle withCell(int x, int y, int z, byte flags) {
        byte[] cells = new byte[ChunkSnapshotBundle.SIZE];
        cells[ChunkSnapshotBundle.indexOf(x, y, z)] = flags;
        return ChunkSnapshotBundle.forTest(cells, 0, 0, 1L);
    }

    @Test
    void airCellIsAir() {
        var bundle = withCell(5, 64, 5, ChunkSnapshotBundle.FLAG_AIR);
        assertThat(bundle.isAir(5, 64, 5)).isTrue();
        assertThat(bundle.isAir(5, 63, 5)).isFalse();
    }

    @Test
    void walkablePositionRequiresAirAboveAndSolidBelow() {
        byte[] cells = new byte[ChunkSnapshotBundle.SIZE];
        // y=64 is the walkable position: air here and above (y=65), solid below (y=63 = 0 = solid)
        cells[ChunkSnapshotBundle.indexOf(5, 64, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        cells[ChunkSnapshotBundle.indexOf(5, 65, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        var bundle = ChunkSnapshotBundle.forTest(cells, 0, 0, 1L);
        assertThat(bundle.isWalkablePosition(5, 64, 5)).isTrue();
    }

    @Test
    void notWalkableWhenNoHeadroom() {
        byte[] cells = new byte[ChunkSnapshotBundle.SIZE];
        // y=64 is air but y=65 is solid (0) — no headroom
        cells[ChunkSnapshotBundle.indexOf(5, 64, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        var bundle = ChunkSnapshotBundle.forTest(cells, 0, 0, 1L);
        assertThat(bundle.isWalkablePosition(5, 64, 5)).isFalse();
    }

    @Test
    void entryConnectorFlagIsReadable() {
        var bundle = withCell(5, 64, 5, ChunkSnapshotBundle.FLAG_ENTRY);
        assertThat(bundle.isEntryConnector(5, 64, 5)).isTrue();
    }

    @Test
    void furnitureSignalFlagIsReadable() {
        var bundle = withCell(5, 64, 5, ChunkSnapshotBundle.FLAG_FURNITURE);
        assertThat(bundle.isFurnitureSignal(5, 64, 5)).isTrue();
    }

    @Test
    void hasShelterAboveWhenSolidWithinRange() {
        byte[] cells = new byte[ChunkSnapshotBundle.SIZE];
        // walkable position at y=64 (air at 64, 65; solid at 63)
        cells[ChunkSnapshotBundle.indexOf(5, 64, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        cells[ChunkSnapshotBundle.indexOf(5, 65, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        // y=70 is solid (value 0, default) — within 8 above y=64
        var bundle = ChunkSnapshotBundle.forTest(cells, 0, 0, 1L);
        assertThat(bundle.hasShelterAbove(5, 64, 5, 8)).isTrue();
    }

    @Test
    void noShelterWhenOpenSkyWithinRange() {
        byte[] cells = new byte[ChunkSnapshotBundle.SIZE];
        // all cells from 64..79 are air — no solid cover within 8 above y=64
        for (int y = 64; y < 80; y++) {
            cells[ChunkSnapshotBundle.indexOf(5, y, 5)] = ChunkSnapshotBundle.FLAG_AIR;
        }
        var bundle = ChunkSnapshotBundle.forTest(cells, 0, 0, 1L);
        assertThat(bundle.hasShelterAbove(5, 64, 5, 8)).isFalse();
    }

    @Test
    void naturalBlockFlagIsReadable() {
        var bundle = withCell(5, 64, 5, ChunkSnapshotBundle.FLAG_NATURAL);
        assertThat(bundle.isNaturalBlock(5, 64, 5)).isTrue();
    }

    @Test
    void indexRoundTrip() {
        int idx = ChunkSnapshotBundle.indexOf(3, 100, 7);
        assertThat(ChunkSnapshotBundle.decodeX(idx)).isEqualTo(3);
        assertThat(ChunkSnapshotBundle.decodeY(idx)).isEqualTo(100);
        assertThat(ChunkSnapshotBundle.decodeZ(idx)).isEqualTo(7);
    }
}
