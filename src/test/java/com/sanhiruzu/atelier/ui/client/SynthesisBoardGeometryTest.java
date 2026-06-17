package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.engine.SynthesisBoard;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisBoardGeometryTest {
    @Test
    void compactPaletteTilesAdvanceAcrossColumnsBeforeRows() {
        ScreenRect panel = new ScreenRect(12, 49, 122, 166);

        List<ScreenRect> tiles = SynthesisBoardGeometry.paletteTiles(panel, 6);

        assertThat(tiles).hasSize(6);
        assertThat(tiles.get(0).width()).isEqualTo(tiles.get(0).height());
        assertThat(tiles.get(1).y()).isEqualTo(tiles.get(0).y());
        assertThat(tiles.get(1).x()).isGreaterThan(tiles.get(0).x());
        assertThat(tiles.get(3).x()).isEqualTo(tiles.get(0).x());
        assertThat(tiles.get(3).y()).isGreaterThan(tiles.get(0).y());
    }

    @Test
    void boardCellHitTestingUsesCenteredBoardRect() {
        ScreenRect area = new ScreenRect(146, 48, 166, 166);
        ScreenRect boardRect = SynthesisBoardGeometry.boardRectForArea(area, SynthesisBoard.CRUDE_3X3);

        Optional<SynthesisBoardGeometry.BoardCell> cell = SynthesisBoardGeometry.boardCellAt(
                area,
                SynthesisBoard.CRUDE_3X3,
                boardRect.x() + boardRect.width() / 2,
                boardRect.y() + boardRect.height() / 2
        );

        assertThat(cell).contains(new SynthesisBoardGeometry.BoardCell(1, 1));
    }

    @Test
    void paletteVisibleCountUsesFullRows() {
        ScreenRect panel = new ScreenRect(12, 49, 122, 166);

        assertThat(SynthesisBoardGeometry.paletteVisibleCount(panel)).isEqualTo(9);
    }

    @Test
    void tallerPaletteUsesMoreFullRows() {
        ScreenRect panel = new ScreenRect(12, 49, 122, 254);

        assertThat(SynthesisBoardGeometry.paletteVisibleCount(panel)).isGreaterThan(12);
    }
}
