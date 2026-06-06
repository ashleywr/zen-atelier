package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;

import java.util.List;

public record SynthesisBoardPlacement(
        String id,
        ReagentStack reagent,
        int x,
        int y,
        int rotation
) {
    public SynthesisBoardPlacement {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("placement id must not be blank");
        }
        if (reagent == null) {
            throw new IllegalArgumentException("reagent must not be null");
        }
        rotation = rotation & 3;
    }

    public List<Cell> cells() {
        return reagent.shape().rotated(rotation).stream()
                .map(cell -> new Cell(x + cell.x(), y + cell.y()))
                .toList();
    }

    public record Cell(int x, int y) {
        static Cell from(ReagentShape.Cell cell) {
            return new Cell(cell.x(), cell.y());
        }
    }
}
