package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

final class SynthesisBoardSession {
    private final List<Placement> placements = new ArrayList<>();
    private final List<Integer> removedPlacementIds = new ArrayList<>();
    private final Set<String> paletteElementFilters = new LinkedHashSet<>();
    private Piece carried;
    private int carriedRotation;
    private Cell carriedCursorCell = new Cell(0, 0);
    private int nextPlacementId = 1;
    private PaletteSource paletteSource = PaletteSource.STORAGE;
    private int paletteScroll;
    private String activeProfileId = "";
    private boolean paletteFiltersOpen;
    private boolean filterNeedsOnly;
    private boolean filterFusionOnly;
    private boolean filterFitsOnly;
    private ShapeFilterMode shapeFilterMode = ShapeFilterMode.ANY;
    private PaletteSortMode paletteSortMode = PaletteSortMode.RELEVANCE;
    private int selectedRequirementFilterIndex = -1;

    List<Placement> placements() {
        return List.copyOf(placements);
    }

    Optional<Piece> carried() {
        return Optional.ofNullable(carried);
    }

    boolean hasCarried() {
        return carried != null;
    }

    int carriedRotation() {
        return carriedRotation;
    }

    Cell carriedCursorCell() {
        return carriedCursorCell;
    }

    List<Integer> removedPlacementIds() {
        return List.copyOf(removedPlacementIds);
    }

    int addDebugPlacement(ReagentStack reagent, int x, int y) {
        int id = nextPlacementId++;
        placements.add(new Placement(id, new Piece(reagent), 0, x, y));
        return id;
    }

    int addPlacement(Piece piece, int rotation, int anchorX, int anchorY) {
        int id = nextPlacementId++;
        placements.add(new Placement(id, piece, rotation & 3, anchorX, anchorY));
        return id;
    }

    void replacePlacement(Placement placement, int rotation, int anchorX, int anchorY) {
        int index = placements.indexOf(placement);
        if (index >= 0) {
            placements.set(index, new Placement(placement.id(), placement.piece(), rotation & 3, anchorX, anchorY));
        }
    }

    void removePlacement(Placement placement) {
        if (placements.remove(placement)) {
            removedPlacementIds.add(placement.id());
        }
    }

    void removePlacementsIf(Predicate<Placement> predicate) {
        ArrayList<Placement> removed = new ArrayList<>();
        placements.removeIf(placement -> {
            if (predicate.test(placement)) {
                removed.add(placement);
                return true;
            }
            return false;
        });
        removed.forEach(placement -> removedPlacementIds.add(placement.id()));
    }

    boolean removePlacementAt(int x, int y, boolean shiftDown) {
        Optional<Placement> existing = placementAt(x, y);
        if (existing.isEmpty()) {
            return false;
        }
        Placement placement = existing.get();
        placements.remove(placement);
        removedPlacementIds.add(placement.id());
        if (shiftDown) {
            clearCarried();
        } else {
            startCarrying(placement.piece(), placement.rotation(), placement.localCellAt(new Cell(x, y)).orElse(defaultCursorCell(placement.piece(), placement.rotation())));
        }
        return true;
    }

    Optional<Placement> placementAt(int x, int y) {
        return placements.stream()
                .filter(placement -> placement.occupies(x, y))
                .findFirst();
    }

    void startCarrying(Piece piece, int rotation) {
        startCarrying(piece, rotation, defaultCursorCell(piece, rotation));
    }

    void startCarrying(Piece piece, int rotation, Cell cursorCell) {
        carried = piece;
        carriedRotation = rotation & 3;
        carriedCursorCell = cursorCell;
    }

    void setCarriedRotation(int rotation, Cell cursorCell) {
        carriedRotation = rotation & 3;
        carriedCursorCell = cursorCell;
    }

    void clearCarried() {
        carried = null;
        carriedRotation = 0;
        carriedCursorCell = new Cell(0, 0);
    }

    void resetAfterSynthesis() {
        placements.clear();
        clearCarried();
        selectedRequirementFilterIndex = -1;
    }

    void syncSelectedPlan(Optional<SynthesisPlan> plan) {
        String profileId = plan.map(value -> value.profile().id()).orElse("");
        if (profileId.equals(activeProfileId)) {
            return;
        }
        activeProfileId = profileId;
        placements.clear();
        clearCarried();
        selectedRequirementFilterIndex = -1;
    }

    PaletteSource paletteSource() {
        return paletteSource;
    }

    void setPaletteSource(PaletteSource paletteSource) {
        this.paletteSource = paletteSource;
        paletteScroll = 0;
    }

    int paletteScroll() {
        return paletteScroll;
    }

    void setPaletteScroll(int paletteScroll) {
        this.paletteScroll = Math.max(0, paletteScroll);
    }

    void adjustPaletteScroll(int step, int maxScroll) {
        paletteScroll = Math.clamp(paletteScroll + step, 0, Math.max(0, maxScroll));
    }

    void clampPaletteScroll(int maxScroll) {
        paletteScroll = Math.clamp(paletteScroll, 0, Math.max(0, maxScroll));
    }

    boolean paletteFiltersOpen() {
        return paletteFiltersOpen;
    }

    void togglePaletteFiltersOpen() {
        paletteFiltersOpen = !paletteFiltersOpen;
    }

    Set<String> paletteElementFilters() {
        return Set.copyOf(paletteElementFilters);
    }

    void togglePaletteElementFilter(String element) {
        if (!paletteElementFilters.add(element)) {
            paletteElementFilters.remove(element);
        }
        paletteScroll = 0;
    }

    boolean filterNeedsOnly() {
        return filterNeedsOnly;
    }

    void toggleFilterNeedsOnly() {
        filterNeedsOnly = !filterNeedsOnly;
        paletteScroll = 0;
    }

    boolean filterFusionOnly() {
        return filterFusionOnly;
    }

    void toggleFilterFusionOnly() {
        filterFusionOnly = !filterFusionOnly;
        paletteScroll = 0;
    }

    boolean filterFitsOnly() {
        return filterFitsOnly;
    }

    void toggleFilterFitsOnly() {
        filterFitsOnly = !filterFitsOnly;
        paletteScroll = 0;
    }

    ShapeFilterMode shapeFilterMode() {
        return shapeFilterMode;
    }

    void setShapeFilterMode(ShapeFilterMode shapeFilterMode) {
        this.shapeFilterMode = shapeFilterMode;
        paletteScroll = 0;
    }

    PaletteSortMode paletteSortMode() {
        return paletteSortMode;
    }

    void advancePaletteSortMode() {
        paletteSortMode = paletteSortMode.next();
        paletteScroll = 0;
    }

    int selectedRequirementFilterIndex() {
        return selectedRequirementFilterIndex;
    }

    void toggleSelectedRequirementFilter(int index) {
        selectedRequirementFilterIndex = selectedRequirementFilterIndex == index ? -1 : index;
        paletteScroll = 0;
    }

    void clearSelectedRequirementFilter() {
        selectedRequirementFilterIndex = -1;
    }

    boolean hasAdvancedFilters() {
        return filterNeedsOnly || filterFusionOnly || filterFitsOnly || shapeFilterMode != ShapeFilterMode.ANY;
    }

    void resetPaletteFilters() {
        paletteElementFilters.clear();
        filterNeedsOnly = false;
        filterFusionOnly = false;
        filterFitsOnly = false;
        shapeFilterMode = ShapeFilterMode.ANY;
        paletteScroll = 0;
    }

    private static Cell defaultCursorCell(Piece piece, int rotation) {
        return piece.rotatedCells(rotation, 0, 0).stream()
                .min(Comparator.comparingInt(Cell::y).thenComparingInt(Cell::x))
                .orElse(new Cell(0, 0));
    }

    record Piece(ReagentStack reagent) {
        String label() {
            return SynthesisStationText.shortLabel(reagent.reagentId());
        }

        ReagentShape shape() {
            return reagent.shape();
        }

        List<Cell> shapeCells() {
            return shape().cells().stream()
                    .map(cell -> new Cell(cell.x(), cell.y()))
                    .toList();
        }

        List<Cell> rotatedCells(int rotation, int anchorX, int anchorY) {
            return shape().rotated(rotation).stream()
                    .map(cell -> new Cell(anchorX + cell.x(), anchorY + cell.y()))
                    .toList();
        }

        boolean hasElement(String element) {
            return reagent.elements().containsKey(element);
        }

        List<String> fusionTraits() {
            if (!reagent.traits().isEmpty()) {
                return reagent.traits();
            }
            if (!reagent.elements().isEmpty()) {
                return reagent.elements().keySet().stream().sorted().toList();
            }
            return List.of(reagent.reagentId().toLowerCase(Locale.ROOT));
        }

        List<String> inheritableTraits() {
            return reagent.traits();
        }
    }

    record Placement(int id, Piece piece, int rotation, int anchorX, int anchorY) {
        List<Cell> cells() {
            return piece.rotatedCells(rotation, anchorX, anchorY);
        }

        boolean occupies(int x, int y) {
            return cells().stream().anyMatch(cell -> cell.x() == x && cell.y() == y);
        }

        Optional<Cell> localCellAt(Cell absoluteCell) {
            return piece.rotatedCells(rotation, 0, 0).stream()
                    .filter(cell -> anchorX + cell.x() == absoluteCell.x() && anchorY + cell.y() == absoluteCell.y())
                    .findFirst();
        }
    }

    record Cell(int x, int y) {
    }

    record PlacementPreview(int anchorX, int anchorY, Cell cursorCell, boolean valid) {
    }

    enum PaletteSource {
        STORAGE,
        INVENTORY
    }

    enum PaletteSortMode {
        RELEVANCE("Rel"),
        TIER("Tier"),
        ELEMENT("Elem");

        private final String label;

        PaletteSortMode(String label) {
            this.label = label;
        }

        PaletteSortMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        String label() {
            return label;
        }
    }

    enum ShapeFilterMode {
        ANY("Any", 0xFF9E8F80) {
            @Override
            boolean matches(ReagentShape shape) {
                return true;
            }
        },
        SINGLE("Single", 0xFFB9C3D0) {
            @Override
            boolean matches(ReagentShape shape) {
                return shape.size() == 1;
            }
        },
        LINE("Line", 0xFF7FB7FF) {
            @Override
            boolean matches(ReagentShape shape) {
                String id = shape.id();
                return id.startsWith("line") || isLine(shape.cells());
            }
        },
        ANGLE("Angle", 0xFFE7A86B) {
            @Override
            boolean matches(ReagentShape shape) {
                String id = shape.id();
                return id.contains("elbow") || id.contains("l_") || hasCorner(shape.cells());
            }
        },
        WIDE("Wide", 0xFF8FD69A) {
            @Override
            boolean matches(ReagentShape shape) {
                return shape.size() >= 4 || shape.id().contains("tee") || shape.id().contains("square");
            }
        };

        static final List<ShapeFilterMode> VALUES = List.of(values());
        private final String label;
        private final int accentColor;

        ShapeFilterMode(String label, int accentColor) {
            this.label = label;
            this.accentColor = accentColor;
        }

        abstract boolean matches(ReagentShape shape);

        String label() {
            return label;
        }

        int accentColor() {
            return accentColor;
        }

        private static boolean isLine(List<ReagentShape.Cell> cells) {
            return cells.stream().map(ReagentShape.Cell::x).distinct().count() == 1
                    || cells.stream().map(ReagentShape.Cell::y).distinct().count() == 1;
        }

        private static boolean hasCorner(List<ReagentShape.Cell> cells) {
            for (ReagentShape.Cell cell : cells) {
                boolean horizontal = cells.stream().anyMatch(other -> other.y() == cell.y() && Math.abs(other.x() - cell.x()) == 1);
                boolean vertical = cells.stream().anyMatch(other -> other.x() == cell.x() && Math.abs(other.y() - cell.y()) == 1);
                if (horizontal && vertical) {
                    return true;
                }
            }
            return false;
        }
    }
}
