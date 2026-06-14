package com.sanhiruzu.atelier.space.analyze;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class MicroRegionTest {

    @Test
    void walkableCellCountMatchesPositions() {
        long[] positions = {1L, 2L, 3L};
        var region = new MicroRegion(42L, 0, 0, positions,
                List.of(), 0, 0, 0, 0, 0, 0, 0, 15, 10, 15);
        assertThat(region.walkableCount()).isEqualTo(3);
    }

    @Test
    void boundaryContactIsPortalWhenFlagged() {
        var contact = new BoundaryContact(BoundaryContact.Face.EAST, 8, 64, true, 42L);
        assertThat(contact.isPortal()).isTrue();
        assertThat(contact.face()).isEqualTo(BoundaryContact.Face.EAST);
    }

    @Test
    void analysisResultHoldsChunkCoords() {
        var result = new AnalysisResult(3, -2, 7L, List.of());
        assertThat(result.chunkX()).isEqualTo(3);
        assertThat(result.chunkZ()).isEqualTo(-2);
        assertThat(result.snapshotVersion()).isEqualTo(7L);
    }
}
