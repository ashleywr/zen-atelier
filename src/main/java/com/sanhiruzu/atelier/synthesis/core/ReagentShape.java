package com.sanhiruzu.atelier.synthesis.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record ReagentShape(String id, List<Cell> cells) {
    private static final int MAX_CELLS = 9;
    public static final ReagentShape SINGLE = new ReagentShape("single", List.of(new Cell(0, 0)));
    public static final ReagentShape LINE_TWO = new ReagentShape("line_2", List.of(new Cell(0, 0), new Cell(1, 0)));
    public static final ReagentShape LINE_THREE = new ReagentShape("line_3", List.of(new Cell(0, 0), new Cell(1, 0), new Cell(2, 0)));
    public static final ReagentShape SQUARE_TWO = new ReagentShape("square_2", List.of(new Cell(0, 0), new Cell(1, 0), new Cell(0, 1), new Cell(1, 1)));
    public static final ReagentShape ELBOW = new ReagentShape("elbow", List.of(new Cell(0, 0), new Cell(0, 1), new Cell(1, 1)));
    public static final ReagentShape TEE = new ReagentShape("tee", List.of(new Cell(0, 0), new Cell(1, 0), new Cell(2, 0), new Cell(1, 1)));

    private static final Codec<ReagentShape> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(ReagentShape::id),
            Cell.CODEC.listOf().fieldOf("cells").forGetter(ReagentShape::cells)
    ).apply(instance, ReagentShape::new));
    public static final Codec<ReagentShape> CODEC = RAW_CODEC.flatXmap(ReagentShape::validate, DataResult::success);

    public ReagentShape {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("shape id must not be blank");
        }
        if (cells == null || cells.isEmpty()) {
            throw new IllegalArgumentException("shape must contain at least one cell");
        }
        if (cells.size() > MAX_CELLS) {
            throw new IllegalArgumentException("shape must not exceed " + MAX_CELLS + " cells");
        }
        cells = normalize(cells);
    }

    public int size() {
        return cells.size();
    }

    public List<Cell> rotated(int rotation) {
        List<Cell> rotated = cells;
        for (int i = 0; i < (rotation & 3); i++) {
            rotated = rotateClockwise(rotated);
        }
        return normalize(rotated);
    }

    private static DataResult<ReagentShape> validate(ReagentShape shape) {
        try {
            return DataResult.success(new ReagentShape(shape.id, shape.cells));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private static List<Cell> rotateClockwise(List<Cell> cells) {
        ArrayList<Cell> rotated = new ArrayList<>();
        for (Cell cell : cells) {
            rotated.add(new Cell(cell.y(), -cell.x()));
        }
        return rotated;
    }

    private static List<Cell> normalize(List<Cell> cells) {
        int minX = cells.stream().mapToInt(Cell::x).min().orElse(0);
        int minY = cells.stream().mapToInt(Cell::y).min().orElse(0);
        ArrayList<Cell> normalized = new ArrayList<>();
        for (Cell cell : cells) {
            normalized.add(new Cell(cell.x() - minX, cell.y() - minY));
        }
        normalized.sort(Comparator.comparingInt(Cell::y).thenComparingInt(Cell::x));
        for (int i = 1; i < normalized.size(); i++) {
            if (normalized.get(i).equals(normalized.get(i - 1))) {
                throw new IllegalArgumentException("shape cells must not overlap");
            }
        }
        return List.copyOf(normalized);
    }

    public record Cell(int x, int y) {
        private static final Codec<Cell> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(Cell::x),
                Codec.INT.fieldOf("y").forGetter(Cell::y)
        ).apply(instance, Cell::new));
    }
}
