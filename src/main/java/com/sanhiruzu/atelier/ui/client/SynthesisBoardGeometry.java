package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.engine.SynthesisBoard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class SynthesisBoardGeometry {
    private static final int BOARD_MODE_OUTER_PADDING = 16;
    private static final int BOARD_MODE_PALETTE_COLUMNS = 3;
    private static final int BOARD_MODE_PALETTE_TILE_SIZE = 32;
    private static final int BOARD_MODE_PALETTE_GAP = 4;
    private static final int BOARD_MODE_PALETTE_TOP = 43;

    private SynthesisBoardGeometry() {
    }

    static ScreenRect boardRectForArea(ScreenRect area, SynthesisBoard board) {
        int usable = Math.max(54, Math.min(area.width(), area.height()) - BOARD_MODE_OUTER_PADDING * 2);
        int cellSize = Math.max(18, usable / Math.max(1, board.size()));
        int size = cellSize * board.size();
        return new ScreenRect(
                area.x() + (area.width() - size) / 2,
                area.y() + (area.height() - size) / 2,
                size,
                size
        );
    }

    static Optional<BoardCell> boardCellAt(ScreenRect boardArea, SynthesisBoard board, int mouseX, int mouseY) {
        ScreenRect boardRect = boardRectForArea(boardArea, board);
        if (!boardRect.contains(mouseX, mouseY)) {
            return Optional.empty();
        }
        int cellSize = boardRect.width() / Math.max(1, board.size());
        return Optional.of(new BoardCell(
                Math.clamp((mouseX - boardRect.x()) / cellSize, 0, board.size() - 1),
                Math.clamp((mouseY - boardRect.y()) / cellSize, 0, board.size() - 1)
        ));
    }

    static ScreenRect cellRectInBoard(ScreenRect boardRect, SynthesisBoard board, int x, int y) {
        int cellSize = boardRect.width() / Math.max(1, board.size());
        return new ScreenRect(boardRect.x() + x * cellSize, boardRect.y() + y * cellSize, cellSize, cellSize);
    }

    static int paletteVisibleRows(ScreenRect panel) {
        return Math.max(0, (panel.height() - BOARD_MODE_PALETTE_TOP + BOARD_MODE_PALETTE_GAP) / (BOARD_MODE_PALETTE_TILE_SIZE + BOARD_MODE_PALETTE_GAP));
    }

    static int paletteVisibleCount(ScreenRect panel) {
        return paletteVisibleRows(panel) * BOARD_MODE_PALETTE_COLUMNS;
    }

    static ScreenRect paletteTile(ScreenRect panel, int index) {
        int column = index % BOARD_MODE_PALETTE_COLUMNS;
        int row = index / BOARD_MODE_PALETTE_COLUMNS;
        return new ScreenRect(
                panel.x() + 5 + column * (BOARD_MODE_PALETTE_TILE_SIZE + BOARD_MODE_PALETTE_GAP),
                panel.y() + BOARD_MODE_PALETTE_TOP + row * (BOARD_MODE_PALETTE_TILE_SIZE + BOARD_MODE_PALETTE_GAP),
                BOARD_MODE_PALETTE_TILE_SIZE,
                BOARD_MODE_PALETTE_TILE_SIZE
        );
    }

    static List<ScreenRect> paletteTiles(ScreenRect panel, int count) {
        ArrayList<ScreenRect> tiles = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            tiles.add(paletteTile(panel, i));
        }
        return List.copyOf(tiles);
    }

    record BoardCell(int x, int y) {
    }
}
