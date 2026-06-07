package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.data.TraitFusionRegistry;
import com.sanhiruzu.atelier.synthesis.data.TraitFusionRule;
import com.sanhiruzu.atelier.synthesis.engine.RequirementStatus;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisBoard;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlanner;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import com.sanhiruzu.atelier.ui.network.SynthesisBoardFusionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class SynthesisSpatialPrototype {
    private static final UiSkin SKIN = UiSkins.active();
    private static final int MAX_BOARD_SIZE = 5;
    private static final int CELL_SIZE = 18;
    private static final int LEFT_BUTTON = 0;
    private static final int RIGHT_BUTTON = 1;
    private static final ScreenRect BOARD_AREA = new ScreenRect(164, 66, MAX_BOARD_SIZE * CELL_SIZE, MAX_BOARD_SIZE * CELL_SIZE);
    private static final ScreenRect READOUT = new ScreenRect(278, 58, 180, 168);
    private static final ScreenRect PALETTE = new ScreenRect(8, 226, 464, 93);
    private static final ScreenRect PALETTE_STORAGE_TAB = new ScreenRect(18, 233, 62, 12);
    private static final ScreenRect PALETTE_INVENTORY_TAB = new ScreenRect(82, 233, 70, 12);
    private static final ScreenRect CHIP_FIRE  = new ScreenRect(162, 233, 40, 12);
    private static final ScreenRect CHIP_WATER = new ScreenRect(204, 233, 40, 12);
    private static final ScreenRect CHIP_EARTH = new ScreenRect(246, 233, 40, 12);
    private static final ScreenRect CHIP_WIND  = new ScreenRect(288, 233, 40, 12);
    private static final ScreenRect CHIP_FILTER = new ScreenRect(332, 233, 40, 12);
    private static final ScreenRect CHIP_SORT  = new ScreenRect(380, 233, 36, 12);
    private static final ScreenRect FILTER_DRAWER = new ScreenRect(8, 58, 146, 154);
    private static final ScreenRect FILTER_NEEDS = new ScreenRect(16, 92, 130, 14);
    private static final ScreenRect FILTER_FUSION = new ScreenRect(16, 110, 130, 14);
    private static final ScreenRect FILTER_FITS = new ScreenRect(16, 128, 130, 14);
    private static final ScreenRect FILTER_RESET = new ScreenRect(92, 194, 54, 12);
    private static final List<ScreenRect> FILTER_SHAPE_RECTS = List.of(
            new ScreenRect(16, 154, 60, 12),
            new ScreenRect(84, 154, 60, 12),
            new ScreenRect(16, 170, 60, 12),
            new ScreenRect(84, 170, 60, 12),
            new ScreenRect(16, 186, 60, 12)
    );
    private static final List<String>     CHIP_ELEMENTS      = List.of("fire", "water", "earth", "wind");
    private static final List<ScreenRect> CHIP_ELEMENT_RECTS = List.of(CHIP_FIRE, CHIP_WATER, CHIP_EARTH, CHIP_WIND);
    private static final int PALETTE_COLUMNS = 11;
    private static final int PALETTE_ROWS = 3;
    private static final int PALETTE_VISIBLE = PALETTE_COLUMNS * PALETTE_ROWS;
    private static final int PALETTE_TILE_WIDTH = 39;
    private static final int PALETTE_TILE_HEIGHT = 22;
    private static final int READOUT_INSET_X = UiMetrics.PANEL_PADDING;
    private static final int READOUT_TOP_PADDING = 5;
    private static final int READOUT_TEXT_HEIGHT = UiMetrics.TEXT_HEIGHT;
    private static final int READOUT_BAR_HEIGHT = 5;
    private static final int READOUT_BAR_LABEL_GAP = 9;
    private static final int READOUT_BAR_BLOCK_HEIGHT = 17;
    private static final int READOUT_ROW_GAP = 9;
    private static final int READOUT_SECTION_GAP = 10;
    private static final int READOUT_SECTION_PAD = 2;
    private static final int READOUT_FILL_VALUE_WIDTH = 70;
    private static final int READOUT_FILL_SECONDARY_X = 80;

    private final List<Placement> placements = new ArrayList<>();
    private PaletteSource paletteSource = PaletteSource.STORAGE;
    private int paletteScroll;
    private Piece carried;
    private int carriedRotation;
    private Cell carriedCursorCell = new Cell(0, 0);
    private int nextPlacementId = 1;
    private String activeProfileId = "";
    private final Set<String> paletteElementFilters = new LinkedHashSet<>();
    private boolean paletteFiltersOpen;
    private boolean filterNeedsOnly;
    private boolean filterFusionOnly;
    private boolean filterFitsOnly;
    private ShapeFilterMode shapeFilterMode = ShapeFilterMode.ANY;
    private PaletteSortMode paletteSortMode = PaletteSortMode.RELEVANCE;
    private int selectedRequirementFilterIndex = -1;
    private SpatialEvaluation lastEvaluation = null;

    void render(GuiGraphics graphics, Font font, Optional<SynthesisPlan> plan, ScreenRect origin) {
        syncSelectedBoard(plan);
        SynthesisBoard board = currentBoard(plan);
        renderBoard(graphics, font, board, origin);
        renderPlacements(graphics, font, board, origin);
        renderReadout(graphics, font, plan, board, origin);
    }

    void renderOverlay(GuiGraphics graphics, Font font, Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents, ScreenRect origin, int mouseX, int mouseY) {
        graphics.flush();
        SynthesisUiLayer.ABOVE_VANILLA_SLOTS.run(graphics, () ->
                renderPalette(graphics, font, paletteView(plan, storageReagents, inventoryReagents), origin, mouseX, mouseY)
        );
        SynthesisUiLayer.CARRIED_REAGENT.run(graphics, () ->
                renderCarriedGhost(graphics, font, currentBoard(plan), origin, mouseX, mouseY)
        );
        graphics.flush();
    }

    void renderLabels(GuiGraphics graphics, Font font) {
    }

    boolean mouseClicked(double mouseX, double mouseY, int button, boolean shiftDown, Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents, ScreenRect origin) {
        syncSelectedBoard(plan);
        SynthesisBoard board = currentBoard(plan);
        int localX = (int) mouseX - origin.x();
        int localY = (int) mouseY - origin.y();
        if (button == RIGHT_BUTTON) {
            return rotateAt(board, localX, localY);
        }
        if (button != LEFT_BUTTON) {
            return false;
        }
        Optional<Cell> boardCell = boardCell(board, localX, localY);
        if (carried != null && boardCell.isEmpty()) {
            clearCarried();
            return true;
        }
        Optional<Integer> requirementFilter = clickedRequirementFilterIndex(plan, board, localX, localY);
        if (requirementFilter.isPresent()) {
            selectedRequirementFilterIndex = selectedRequirementFilterIndex == requirementFilter.get()
                    ? -1
                    : requirementFilter.get();
            paletteScroll = 0;
            return true;
        }
        if (PALETTE_STORAGE_TAB.contains(localX, localY)) {
            paletteSource = PaletteSource.STORAGE;
            paletteScroll = 0;
            return true;
        }
        if (PALETTE_INVENTORY_TAB.contains(localX, localY)) {
            paletteSource = PaletteSource.INVENTORY;
            paletteScroll = 0;
            return true;
        }
        for (int i = 0; i < CHIP_ELEMENTS.size(); i++) {
            if (CHIP_ELEMENT_RECTS.get(i).contains(localX, localY)) {
                togglePaletteElementFilter(CHIP_ELEMENTS.get(i));
                paletteScroll = 0;
                return true;
            }
        }
        if (CHIP_SORT.contains(localX, localY)) {
            paletteSortMode = paletteSortMode.next();
            paletteScroll = 0;
            return true;
        }
        if (CHIP_FILTER.contains(localX, localY)) {
            paletteFiltersOpen = !paletteFiltersOpen;
            return true;
        }
        if (paletteFiltersOpen) {
            ScreenRect drawer = filterDrawerLocal(origin);
            if (drawerControlRect(FILTER_NEEDS, drawer).contains(localX, localY)) {
                filterNeedsOnly = !filterNeedsOnly;
                paletteScroll = 0;
                return true;
            }
            if (drawerControlRect(FILTER_FUSION, drawer).contains(localX, localY)) {
                filterFusionOnly = !filterFusionOnly;
                paletteScroll = 0;
                return true;
            }
            if (drawerControlRect(FILTER_FITS, drawer).contains(localX, localY)) {
                filterFitsOnly = !filterFitsOnly;
                paletteScroll = 0;
                return true;
            }
            for (int i = 0; i < ShapeFilterMode.VALUES.size(); i++) {
                if (drawerControlRect(FILTER_SHAPE_RECTS.get(i), drawer).contains(localX, localY)) {
                    shapeFilterMode = ShapeFilterMode.VALUES.get(i);
                    paletteScroll = 0;
                    return true;
                }
            }
            if (drawerControlRect(FILTER_RESET, drawer).contains(localX, localY)) {
                resetPaletteFilters();
                paletteScroll = 0;
                return true;
            }
        }
        PaletteView palette = paletteView(plan, storageReagents, inventoryReagents);
        Optional<Integer> paletteIndex = hoveredPaletteIndex(localX, localY, palette.entries());
        if (paletteIndex.isPresent()) {
            Piece selected = palette.entries().get(paletteIndex.get()).piece();
            startCarrying(selected, 0);
            return true;
        }
        if (boardCell.isEmpty()) {
            return PALETTE.contains(localX, localY);
        }
        if (carried != null) {
            if (shiftDown) {
                Optional<PlacementPreview> preview = overwritePreview(board, carried, carriedRotation, boardCell.get());
                if (preview.isPresent()) {
                    removeOverlappedPlacements(carried, carriedRotation, preview.get());
                    placements.add(new Placement(nextPlacementId++, carried, carriedRotation, preview.get().anchorX(), preview.get().anchorY()));
                    clearCarried();
                }
                return true;
            }
            PlacementPreview preview = placementPreview(board, carried, carriedRotation, boardCell.get());
            if (preview.valid()) {
                placements.add(new Placement(nextPlacementId++, carried, carriedRotation, preview.anchorX(), preview.anchorY()));
                clearCarried();
            }
            return true;
        }
        Optional<Placement> existing = placementAt(boardCell.get().x(), boardCell.get().y());
        if (existing.isPresent()) {
            Placement placement = existing.get();
            placements.remove(placement);
            startCarrying(
                    placement.piece(),
                    placement.rotation(),
                    placement.localCellAt(boardCell.get()).orElse(defaultCursorCell(placement.piece(), placement.rotation()))
            );
            return true;
        }
        return true;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents, ScreenRect origin) {
        int localX = (int) mouseX - origin.x();
        int localY = (int) mouseY - origin.y();
        if (!PALETTE.contains(localX, localY)) {
            return false;
        }
        List<PaletteEntry> pieces = paletteView(Optional.empty(), storageReagents, inventoryReagents).entries();
        int maxScroll = Math.max(0, pieces.size() - PALETTE_VISIBLE);
        if (maxScroll <= 0) {
            return true;
        }
        int step = scrollY < 0.0D ? PALETTE_COLUMNS : -PALETTE_COLUMNS;
        paletteScroll = Math.clamp(paletteScroll + step, 0, maxScroll);
        return true;
    }

    boolean rotateCarried() {
        return rotateCarried(SynthesisBoard.CRUDE_3X3, Optional.empty());
    }

    boolean rotateCarriedAt(int mouseX, int mouseY, Optional<SynthesisPlan> plan, ScreenRect origin) {
        SynthesisBoard board = currentBoard(plan);
        int localX = mouseX - origin.x();
        int localY = mouseY - origin.y();
        return rotateCarried(board, boardCell(board, localX, localY));
    }

    private boolean rotateCarried(SynthesisBoard board, Optional<Cell> hover) {
        if (carried == null) {
            return false;
        }
        if (hover.isPresent()) {
            Cell target = hover.get();
            for (int offset = 1; offset < 4; offset++) {
                int candidateRotation = (carriedRotation + offset) & 3;
                PlacementPreview preview = placementPreview(board, carried, candidateRotation, target);
                if (preview.valid()) {
                    carriedRotation = candidateRotation;
                    carriedCursorCell = preview.cursorCell();
                    return true;
                }
            }
        }
        carriedRotation = (carriedRotation + 1) & 3;
        carriedCursorCell = hover
                .map(cell -> placementPreview(board, carried, carriedRotation, cell).cursorCell())
                .orElseGet(() -> defaultCursorCell(carried, carriedRotation));
        return true;
    }

    boolean renderTooltip(GuiGraphics graphics, Font font, Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents, ScreenRect origin, int mouseX, int mouseY) {
        SynthesisBoard board = currentBoard(plan);
        int localX = mouseX - origin.x();
        int localY = mouseY - origin.y();
        PaletteView palette = paletteView(plan, storageReagents, inventoryReagents);
        Optional<Integer> paletteIndex = hoveredPaletteIndex(localX, localY, palette.entries());
        if (paletteIndex.isPresent()) {
            PaletteEntry entry = palette.entries().get(paletteIndex.get());
            graphics.renderComponentTooltip(font, pieceTooltip(entry), mouseX, mouseY);
            return true;
        }
        if (PALETTE.contains(localX, localY)) {
            return true;
        }
        Optional<Cell> boardCell = boardCell(board, localX, localY);
        if (boardCell.isEmpty()) {
            return false;
        }
        Optional<Placement> placement = placementAt(boardCell.get().x(), boardCell.get().y());
        if (placement.isPresent()) {
            graphics.renderComponentTooltip(font, pieceTooltip(new PaletteEntry(
                    placement.get().piece(),
                    false,
                    false,
                    primaryElementIndex(placement.get().piece()),
                    true,
                    placement.get().piece().shape().size()
            )), mouseX, mouseY);
            return true;
        }
        return false;
    }

    private void renderBoard(GuiGraphics graphics, Font font, SynthesisBoard synthesisBoard, ScreenRect origin) {
        ScreenRect board = boardRect(synthesisBoard).offset(origin.x(), origin.y());
        graphics.fill(board.x() - 1, board.y() - 1, board.right() + 1, board.bottom() + 1, 0xFF0F0D0B);
        for (int y = 0; y < synthesisBoard.size(); y++) {
            for (int x = 0; x < synthesisBoard.size(); x++) {
                ScreenRect cell = cellRect(origin, synthesisBoard, x, y);
                graphics.fill(cell.x(), cell.y(), cell.right(), cell.bottom(), 0xFF191613);
                SynthesisStationDrawing.frame(graphics, cell, 0xFF40372F);
            }
        }
        for (SynthesisBoard.Node node : synthesisBoard.nodes()) {
            renderNode(graphics, font, origin, synthesisBoard, node);
        }
    }

    private void renderNode(GuiGraphics graphics, Font font, ScreenRect origin, SynthesisBoard synthesisBoard, SynthesisBoard.Node node) {
        int color = nodeColor(node);
        ScreenRect cell = cellRect(origin, synthesisBoard, node.x(), node.y()).inset(4);
        graphics.fill(cell.x(), cell.y(), cell.right(), cell.bottom(), 0x66000000 | (color & 0x00FFFFFF));
        SynthesisStationDrawing.frame(graphics, cell, color);
        graphics.drawCenteredString(font, nodeLabel(node), cell.x() + cell.width() / 2, cell.y() + 2, color);
    }

    private void renderPlacements(GuiGraphics graphics, Font font, SynthesisBoard board, ScreenRect origin) {
        SpatialEvaluation evaluation = evaluate(board);
        for (TraitLink link : evaluation.links()) {
            ScreenRect a = cellRect(origin, board, link.from().x(), link.from().y());
            ScreenRect b = cellRect(origin, board, link.to().x(), link.to().y());
            int ax = a.x() + CELL_SIZE / 2;
            int ay = a.y() + CELL_SIZE / 2;
            int bx = b.x() + CELL_SIZE / 2;
            int by = b.y() + CELL_SIZE / 2;
            graphics.fill(Math.min(ax, bx) - 1, Math.min(ay, by) - 1, Math.max(ax, bx) + 1, Math.max(ay, by) + 1, 0xFFB6F08C);
        }
        for (FusionResult fusion : evaluation.fusionResults()) {
            ScreenRect a = cellRect(origin, board, fusion.from().x(), fusion.from().y());
            ScreenRect b = cellRect(origin, board, fusion.to().x(), fusion.to().y());
            int ax = a.x() + CELL_SIZE / 2;
            int ay = a.y() + CELL_SIZE / 2;
            int bx = b.x() + CELL_SIZE / 2;
            int by = b.y() + CELL_SIZE / 2;
            int color = fusion.rule().color() | 0xFF000000;
            graphics.fill(Math.min(ax, bx) - 1, Math.min(ay, by) - 1, Math.max(ax, bx) + 1, Math.max(ay, by) + 1, color);
        }
        for (Placement placement : placements) {
            boolean resonant = evaluation.resonantPlacementIds().contains(placement.id());
            for (Cell cell : placement.cells()) {
                ScreenRect rect = cellRect(origin, board, cell.x(), cell.y());
                int color = pieceColor(placement.piece());
                graphics.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2, color);
                SynthesisStationDrawing.frame(graphics, rect.inset(2), 0xFF11100E);
                if (resonant) {
                    SynthesisStationDrawing.frame(graphics, rect.inset(1), 0xFFFF8040);
                }
            }
            Cell anchor = placement.cells().getFirst();
            ScreenRect rect = cellRect(origin, board, anchor.x(), anchor.y());
            ItemStack stack = ReagentItem.createStack(placement.piece().reagent());
            graphics.renderFakeItem(stack, rect.x() + 1, rect.y() + 1);
            graphics.drawString(font, Integer.toString(placement.piece().shape().size()), rect.x() + 11, rect.y() + 9, SynthesisScreenTheme.TEXT, false);
        }
        for (Placement placement : placements) {
            List<String> traits = placement.piece().reagent().traits();
            if (traits.isEmpty()) {
                continue;
            }
            Cell anchor = placement.cells().getFirst();
            ScreenRect rect = cellRect(origin, board, anchor.x(), anchor.y());
            renderTraitDots(graphics, traits, rect);
        }
    }

    private static void renderTraitDots(GuiGraphics graphics, List<String> traits, ScreenRect cell) {
        int count = Math.min(4, traits.size());
        int dotY = cell.bottom() - 4;
        graphics.fill(cell.x() + 2, dotY - 1, cell.x() + 2 + count * 3, dotY + 3, 0xAA000000);
        for (int i = 0; i < count; i++) {
            int traitColor = SynthesisNoun.color(traits.get(i), 0xFFB0A090);
            int dotColor = 0xFF000000 | (traitColor & 0x00FFFFFF);
            int dx = cell.x() + 3 + i * 3;
            graphics.fill(dx, dotY, dx + 2, dotY + 2, dotColor);
        }
    }

    private void renderPalette(GuiGraphics graphics, Font font, PaletteView paletteView, ScreenRect origin, int mouseX, int mouseY) {
        ScreenRect palette = PALETTE.offset(origin.x(), origin.y());
        graphics.fill(palette.x(), palette.y(), palette.right(), palette.bottom(), 0xFF171411);
        SynthesisStationDrawing.frame(graphics, palette, 0xFF4F453C);
        renderPaletteTab(graphics, font, PALETTE_STORAGE_TAB.offset(origin.x(), origin.y()), "Storage", paletteSource == PaletteSource.STORAGE);
        renderPaletteTab(graphics, font, PALETTE_INVENTORY_TAB.offset(origin.x(), origin.y()), "Inventory", paletteSource == PaletteSource.INVENTORY);
        for (int i = 0; i < CHIP_ELEMENTS.size(); i++) {
            String element = CHIP_ELEMENTS.get(i);
            renderPaletteChip(graphics, font, CHIP_ELEMENT_RECTS.get(i).offset(origin.x(), origin.y()),
                    capitalize(element), paletteElementFilters.contains(element), SynthesisNoun.color(element, SynthesisScreenTheme.ACCENT));
        }
        renderPaletteChip(graphics, font, CHIP_FILTER.offset(origin.x(), origin.y()),
                "Filter", paletteFiltersOpen || hasAdvancedFilters(), 0xFFCFAE6A);
        renderPaletteChip(graphics, font, CHIP_SORT.offset(origin.x(), origin.y()),
                paletteSortMode.label(), true, SynthesisScreenTheme.ACCENT_DIM);
        if (paletteFiltersOpen) {
            renderFilterDrawer(graphics, font, origin);
        }
        List<PaletteEntry> pieces = paletteView.entries();
        if (pieces.isEmpty()) {
            graphics.drawString(font, paletteView.emptyMessage(), palette.x() + 7, palette.y() + 28, SynthesisScreenTheme.MUTED, false);
            return;
        }
        int maxScroll = Math.max(0, pieces.size() - PALETTE_VISIBLE);
        paletteScroll = Math.clamp(paletteScroll, 0, maxScroll);
        int limit = Math.min(PALETTE_VISIBLE, pieces.size() - paletteScroll);
        for (int i = 0; i < limit; i++) {
            int pieceIndex = paletteScroll + i;
            ScreenRect tile = paletteTile(origin, i);
            PaletteEntry entry = pieces.get(pieceIndex);
            Piece piece = entry.piece();
            boolean hovered = tile.contains(mouseX, mouseY);
            graphics.fill(tile.x(), tile.y(), tile.right(), tile.bottom(), hovered ? 0xFF3B332D : 0xFF211D1A);
            int borderColor = samePiece(carried, piece) ? SynthesisScreenTheme.ACCENT
                    : entry.matchesUnsatisfiedRequirement() ? SynthesisScreenTheme.GOOD
                    : entry.hasFusionPotential() ? 0xFFFFB454
                    : 0xFF4A4037;
            SynthesisStationDrawing.frame(graphics, tile, borderColor);
            graphics.renderFakeItem(ReagentItem.createStack(piece.reagent()), tile.x() + 2, tile.y() + 2);
            drawMiniShape(graphics, piece, tile.x() + 22, tile.y() + 4);
            renderPaletteReasonPips(graphics, tile, entry);
            graphics.drawString(font, Integer.toString(piece.shape().size()), tile.right() - 8, tile.bottom() - 9, SynthesisScreenTheme.TEXT, false);
        }
        if (pieces.size() > PALETTE_VISIBLE) {
            String range = (paletteScroll + 1) + "-" + (paletteScroll + limit) + "/" + pieces.size();
            graphics.drawString(font, range, palette.right() - font.width(range) - 5, palette.y() + 6, SynthesisScreenTheme.MUTED, false);
        }
    }

    private void renderPaletteTab(GuiGraphics graphics, Font font, ScreenRect tab, String label, boolean active) {
        UiChrome.chipFace(graphics, tab, SKIN, SynthesisScreenTheme.ACCENT, active);
        SynthesisStationText.drawCenteredFit(graphics, font, Component.literal(label), tab.inset(UiMetrics.INSET_SMALL), active ? SynthesisScreenTheme.TEXT : SynthesisScreenTheme.MUTED);
    }

    private void renderPaletteChip(GuiGraphics graphics, Font font, ScreenRect tab, String label, boolean active, int accentColor) {
        UiChrome.chipFace(graphics, tab, SKIN, accentColor, active);
        SynthesisStationText.drawCenteredFit(graphics, font, Component.literal(label), tab.inset(UiMetrics.INSET_SMALL), active ? SynthesisScreenTheme.TEXT : SynthesisScreenTheme.MUTED);
    }

    private void renderFilterDrawer(GuiGraphics graphics, Font font, ScreenRect origin) {
        ScreenRect drawer = filterDrawerAbsolute(origin);
        graphics.fill(drawer.x(), drawer.y(), drawer.right(), drawer.bottom(), 0xF0191714);
        SynthesisStationDrawing.frame(graphics, drawer, 0xFF5B4F44);
        SynthesisStationText.drawFit(graphics, font, "Palette Filter", new ScreenRect(drawer.x() + UiMetrics.LABEL_PADDING, drawer.y() + UiMetrics.LABEL_PADDING, drawer.width() - UiMetrics.LABEL_PADDING * 2, UiMetrics.TEXT_HEIGHT), SynthesisScreenTheme.ACCENT);
        SynthesisStationText.drawFit(graphics, font, "Narrow for the current recipe and board.", new ScreenRect(drawer.x() + UiMetrics.LABEL_PADDING, drawer.y() + 18, drawer.width() - UiMetrics.LABEL_PADDING * 2, 16), SynthesisScreenTheme.MUTED);
        renderPaletteChip(graphics, font, drawerControlRect(FILTER_NEEDS, drawer), "Need Now", filterNeedsOnly, SynthesisScreenTheme.GOOD);
        renderPaletteChip(graphics, font, drawerControlRect(FILTER_FUSION, drawer), "Fusion Ready", filterFusionOnly, 0xFFFFB454);
        renderPaletteChip(graphics, font, drawerControlRect(FILTER_FITS, drawer), "Fits Board", filterFitsOnly, 0xFF7FB7FF);
        SynthesisStationText.drawFit(graphics, font, "Shape", new ScreenRect(drawer.x() + UiMetrics.LABEL_PADDING, drawer.y() + 82, drawer.width() - UiMetrics.LABEL_PADDING * 2, UiMetrics.TEXT_HEIGHT), SynthesisScreenTheme.TEXT);
        for (int i = 0; i < ShapeFilterMode.VALUES.size(); i++) {
            ShapeFilterMode mode = ShapeFilterMode.VALUES.get(i);
            renderPaletteChip(graphics, font, drawerControlRect(FILTER_SHAPE_RECTS.get(i), drawer),
                    mode.label(), shapeFilterMode == mode, mode.accentColor());
        }
        renderPaletteChip(graphics, font, drawerControlRect(FILTER_RESET, drawer), "Reset", false, 0xFF9E7E5E);
    }

    private ScreenRect filterDrawerLocal(ScreenRect origin) {
        Minecraft minecraft = Minecraft.getInstance();
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        int absoluteX = Math.clamp(origin.x() + FILTER_DRAWER.x(), 0, Math.max(0, guiWidth - FILTER_DRAWER.width()));
        int absoluteY = Math.clamp(origin.y() + FILTER_DRAWER.y(), 0, Math.max(0, guiHeight - FILTER_DRAWER.height()));
        return new ScreenRect(absoluteX - origin.x(), absoluteY - origin.y(), FILTER_DRAWER.width(), FILTER_DRAWER.height());
    }

    private ScreenRect filterDrawerAbsolute(ScreenRect origin) {
        ScreenRect local = filterDrawerLocal(origin);
        return local.offset(origin.x(), origin.y());
    }

    private static ScreenRect drawerControlRect(ScreenRect control, ScreenRect drawer) {
        return new ScreenRect(
                drawer.x() + (control.x() - FILTER_DRAWER.x()),
                drawer.y() + (control.y() - FILTER_DRAWER.y()),
                control.width(),
                control.height()
        );
    }

    private void renderReadout(GuiGraphics graphics, Font font, Optional<SynthesisPlan> plan, SynthesisBoard board, ScreenRect origin) {
        ScreenRect panel = READOUT.offset(origin.x(), origin.y());
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xEE171411);
        SynthesisStationDrawing.frame(graphics, panel, 0xFF4F453C);

        if (plan.isEmpty()) {
            SynthesisStationText.drawFit(graphics, font, "Select recipe", readoutLineRect(panel, READOUT_TOP_PADDING), SynthesisScreenTheme.MUTED);
            return;
        }

        SynthesisPlan current = plan.get();
        SynthesisPlan placedPlan = new SynthesisPlanner().plan(current.profile(), placedReagentContainer(), 0);
        SpatialEvaluation spatial = evaluate(board);
        int resonanceRisk = spatial.resonantPlacementIds().size() * 15;
        ReadoutLayout layout = buildReadoutLayout(panel, placedPlan, spatial);
        int successColor = !placedPlan.canSynthesize() ? SynthesisScreenTheme.MUTED
                : resonanceRisk > 0 ? 0xFFFF8040
                : SynthesisScreenTheme.GOOD;
        renderReadoutBar(graphics, font, "Success", current.preview().successProbability(), layout.successLabel(), layout.successBar(), successColor);
        layout.resonanceLine().ifPresent(rect ->
                SynthesisStationText.drawFit(graphics, font, "Resonance  +" + resonanceRisk + " risk", rect, 0xFFFF8040));
        renderReadoutBar(graphics, font, "Perfect", current.preview().probabilityOf(OutcomeClass.PERFECT_SUCCESS), layout.perfectLabel(), layout.perfectBar(), SynthesisScreenTheme.ACCENT);

        int occupied = occupiedCellCount();
        int totalCells = board.size() * board.size();
        SynthesisStationText.drawFit(graphics, font, "Fill " + occupied + "/" + totalCells, layout.fillLine(), SynthesisScreenTheme.TEXT);
        SynthesisStationText.drawFit(graphics, font, "Empty " + (totalCells - occupied), layout.emptyLine(), occupied == totalCells ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.MUTED);

        SynthesisStationText.drawFit(graphics, font, "Needs", layout.needsHeader(), placedPlan.canSynthesize() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.BAD);
        List<RequirementStatus> displayedRequirements = displayedRequirements(placedPlan);
        for (int i = 0; i < displayedRequirements.size(); i++) {
            RequirementStatus status = displayedRequirements.get(i);
            int color = status.satisfied() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.BAD;
            ScreenRect lineRect = layout.requirementLines().get(i);
            if (selectedRequirementFilterIndex == i) {
                SynthesisStationDrawing.frame(graphics, lineRect.inset(-1), SynthesisScreenTheme.ACCENT);
            }
            SynthesisStationText.drawRichFit(graphics, font, requirementLine(status), lineRect, color);
        }
        layout.extraNeedsLine().ifPresent(rect ->
                SynthesisStationText.drawFit(graphics, font, "+" + (placedPlan.requirements().size() - displayedRequirements.size()) + " more needs", rect, SynthesisScreenTheme.MUTED));

        SynthesisStationText.drawFit(graphics, font, "Traits", layout.traitsHeader(), spatial.expectedTraits().isEmpty() ? SynthesisScreenTheme.MUTED : SynthesisScreenTheme.GOOD);
        if (spatial.expectedTraits().isEmpty()) {
            layout.emptyTraitsLine().ifPresent(rect ->
                    SynthesisStationText.drawFit(graphics, font, "None placed", rect, SynthesisScreenTheme.MUTED));
        } else {
            List<ScreenRect> traitLines = layout.traitLines();
            int traitIndex = 0;
            for (Map.Entry<String, Integer> trait : spatial.expectedTraits().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .limit(3)
                    .toList()) {
                Component text = SynthesisNoun.line(SynthesisNoun.component(trait.getKey()), " +", trait.getValue());
                if (traitIndex >= traitLines.size()) {
                    break;
                }
                SynthesisStationText.drawRichFit(graphics, font, text, traitLines.get(traitIndex++), SynthesisScreenTheme.TEXT);
            }
        }

        if (layout.fusionsHeader().isPresent()) {
            int totalQuality = 0;
            int totalSuccess = 0;
            for (FusionResult fr : spatial.fusionResults()) {
                totalQuality += fr.rule().qualityBonus();
                totalSuccess += fr.rule().successWeightBonus();
            }
            MutableComponent fusionsHeader = Component.literal("Fusions").withStyle(s -> s.withColor(SynthesisScreenTheme.ACCENT));
            if (totalQuality > 0 || totalSuccess > 0) {
                fusionsHeader.append(Component.literal("  " + fusionTotals(totalQuality, totalSuccess)).withStyle(s -> s.withColor(SynthesisScreenTheme.GOOD)));
            }
            SynthesisStationText.drawRichFit(graphics, font, fusionsHeader, layout.fusionsHeader().get(), SynthesisScreenTheme.ACCENT);
            for (int i = 0; i < Math.min(spatial.fusionResults().size(), layout.fusionLines().size()); i++) {
                SynthesisStationText.drawRichFit(graphics, font, fusionLine(spatial.fusionResults().get(i)), layout.fusionLines().get(i), SynthesisScreenTheme.TEXT);
            }
        }
    }

    private static String fusionTotals(int quality, int success) {
        if (quality > 0 && success > 0) return "+" + quality + "q +" + success + "s";
        if (quality > 0) return "+" + quality + " quality";
        return "+" + success + " success";
    }

    private static Component fusionLine(FusionResult fusion) {
        TraitFusionRule rule = fusion.rule();
        if (rule.outputAffix().isPresent()) {
            return SynthesisNoun.line(
                    SynthesisNoun.component(fusion.traitA()), " + ",
                    SynthesisNoun.component(fusion.traitB()), " → ",
                    SynthesisNoun.component(rule.outputAffix().get()));
        }
        String bonus = rule.qualityBonus() > 0
                ? "+" + rule.qualityBonus() + " quality"
                : "+" + rule.successWeightBonus() + " success";
        return SynthesisNoun.line(
                SynthesisNoun.component(fusion.traitA()), " + ",
                SynthesisNoun.component(fusion.traitB()), " → ", bonus);
    }

    private void renderReadoutBar(GuiGraphics graphics, Font font, String label, double amount, ScreenRect labelRect, ScreenRect barRect, int color) {
        SynthesisStationText.drawFit(graphics, font, label + " " + SynthesisStationText.percent(amount), labelRect, color);
        graphics.fill(barRect.x(), barRect.y(), barRect.right(), barRect.bottom(), SynthesisScreenTheme.PANEL_LIGHT);
        graphics.fill(barRect.x(), barRect.y(), barRect.x() + (int) Math.round(barRect.width() * amount), barRect.bottom(), color);
    }

    private void renderCarriedGhost(GuiGraphics graphics, Font font, SynthesisBoard board, ScreenRect origin, int mouseX, int mouseY) {
        if (carried == null) {
            return;
        }
        int localX = mouseX - origin.x();
        int localY = mouseY - origin.y();
        Optional<Cell> hover = boardCell(board, localX, localY);
        if (hover.isEmpty()) {
            drawFloatingShape(graphics, font, carried, carriedRotation, mouseX + 5, mouseY + 5, true);
            return;
        }
        PlacementPreview preview = placementPreview(board, carried, carriedRotation, hover.get());
        for (Cell cell : carried.rotatedCells(carriedRotation, preview.anchorX(), preview.anchorY())) {
            if (!within(board, cell.x(), cell.y())) {
                continue;
            }
            ScreenRect rect = cellRect(origin, board, cell.x(), cell.y());
            graphics.fill(rect.x() + 3, rect.y() + 3, rect.right() - 3, rect.bottom() - 3, preview.valid() ? 0x887FBF89 : 0x88D37A6A);
        }
        ScreenRect cursor = cellRect(origin, board, hover.get().x(), hover.get().y()).inset(5);
        SynthesisStationDrawing.frame(graphics, cursor, preview.valid() ? SynthesisScreenTheme.ACCENT : 0xFFD37A6A);
        ScreenRect boardRect = boardRect(board).offset(origin.x(), origin.y());
        graphics.drawString(font, "Right-click/R rotate", boardRect.x(), boardRect.bottom() + 4, SynthesisScreenTheme.MUTED, false);
    }

    private boolean rotateAt(SynthesisBoard board, int localX, int localY) {
        if (rotateCarried(board, boardCell(board, localX, localY))) {
            return true;
        }
        Optional<Cell> boardCell = boardCell(board, localX, localY);
        if (boardCell.isEmpty()) {
            return false;
        }
        Optional<Placement> existing = placementAt(boardCell.get().x(), boardCell.get().y());
        if (existing.isEmpty()) {
            return true;
        }
        Placement placement = existing.get();
        int nextRotation = (placement.rotation() + 1) & 3;
        if (!canPlace(board, placement.piece(), nextRotation, placement.anchorX(), placement.anchorY(), placement.id())) {
            return true;
        }
        int index = placements.indexOf(placement);
        placements.set(index, new Placement(placement.id(), placement.piece(), nextRotation, placement.anchorX(), placement.anchorY()));
        return true;
    }

    private void startCarrying(Piece piece, int rotation) {
        startCarrying(piece, rotation, defaultCursorCell(piece, rotation));
    }

    private void startCarrying(Piece piece, int rotation, Cell cursorCell) {
        carried = piece;
        carriedRotation = rotation & 3;
        carriedCursorCell = cursorCell;
    }

    private void clearCarried() {
        carried = null;
        carriedRotation = 0;
        carriedCursorCell = new Cell(0, 0);
    }

    private PlacementPreview placementPreview(SynthesisBoard board, Piece piece, int rotation, Cell hover) {
        Optional<PlacementPreview> preferred = previewForCursorCell(board, piece, rotation, hover, carriedCursorCell);
        if (preferred.isPresent() && preferred.get().valid()) {
            return preferred.get();
        }
        for (Cell cursorCell : piece.rotatedCells(rotation, 0, 0)) {
            PlacementPreview preview = previewForCursorCell(board, piece, rotation, hover, cursorCell).orElseThrow();
            if (preview.valid()) {
                return preview;
            }
        }
        return preferred.orElseGet(() -> {
            Cell fallback = defaultCursorCell(piece, rotation);
            return previewForCursorCell(board, piece, rotation, hover, fallback).orElseThrow();
        });
    }

    private Optional<PlacementPreview> overwritePreview(SynthesisBoard board, Piece piece, int rotation, Cell hover) {
        Optional<PlacementPreview> preferred = overwritePreviewForCursorCell(board, piece, rotation, hover, carriedCursorCell);
        if (preferred.isPresent() && preferred.get().valid()) {
            return preferred;
        }
        for (Cell cursorCell : piece.rotatedCells(rotation, 0, 0)) {
            Optional<PlacementPreview> preview = overwritePreviewForCursorCell(board, piece, rotation, hover, cursorCell);
            if (preview.isPresent() && preview.get().valid()) {
                return preview;
            }
        }
        return Optional.empty();
    }

    private Optional<PlacementPreview> previewForCursorCell(SynthesisBoard board, Piece piece, int rotation, Cell hover, Cell cursorCell) {
        boolean cursorCellExists = piece.rotatedCells(rotation, 0, 0).stream().anyMatch(cursorCell::equals);
        if (!cursorCellExists) {
            return Optional.empty();
        }
        int anchorX = hover.x() - cursorCell.x();
        int anchorY = hover.y() - cursorCell.y();
        return Optional.of(new PlacementPreview(anchorX, anchorY, cursorCell, canPlace(board, piece, rotation, anchorX, anchorY, -1)));
    }

    private Optional<PlacementPreview> overwritePreviewForCursorCell(SynthesisBoard board, Piece piece, int rotation, Cell hover, Cell cursorCell) {
        boolean cursorCellExists = piece.rotatedCells(rotation, 0, 0).stream().anyMatch(cursorCell::equals);
        if (!cursorCellExists) {
            return Optional.empty();
        }
        int anchorX = hover.x() - cursorCell.x();
        int anchorY = hover.y() - cursorCell.y();
        return Optional.of(new PlacementPreview(anchorX, anchorY, cursorCell, fitsBoard(board, piece, rotation, anchorX, anchorY)));
    }

    private void removeOverlappedPlacements(Piece piece, int rotation, PlacementPreview preview) {
        Set<Cell> targetCells = new HashSet<>(piece.rotatedCells(rotation, preview.anchorX(), preview.anchorY()));
        placements.removeIf(placement -> placement.cells().stream().anyMatch(targetCells::contains));
    }

    private boolean fitsBoard(SynthesisBoard board, Piece piece, int rotation, int anchorX, int anchorY) {
        for (Cell cell : piece.rotatedCells(rotation, anchorX, anchorY)) {
            if (!within(board, cell.x(), cell.y())) {
                return false;
            }
        }
        return true;
    }

    private boolean canPlace(SynthesisBoard board, Piece piece, int rotation, int anchorX, int anchorY, int ignoredPlacementId) {
        for (Cell cell : piece.rotatedCells(rotation, anchorX, anchorY)) {
            if (!within(board, cell.x(), cell.y())) {
                return false;
            }
            Optional<Placement> occupied = placementAt(cell.x(), cell.y());
            if (occupied.isPresent() && occupied.get().id() != ignoredPlacementId) {
                return false;
            }
        }
        return true;
    }

    private SpatialEvaluation evaluate(SynthesisBoard board) {
        List<TraitLink> links = new ArrayList<>();
        java.util.Map<String, Integer> expectedTraits = new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> fusedTraits = new java.util.LinkedHashMap<>();
        List<FusionResult> fusionResults = new ArrayList<>();
        Set<String> seenFusionKeys = new HashSet<>();
        java.util.Map<Integer, Integer> fusionCountPerPlacement = new java.util.HashMap<>();

        for (Placement placement : placements) {
            for (String trait : placement.piece().inheritableTraits()) {
                expectedTraits.merge(trait, 1, Integer::sum);
            }
        }
        for (Placement first : placements) {
            for (Placement second : placements) {
                if (first.id() >= second.id()) {
                    continue;
                }
                for (Cell a : first.cells()) {
                    for (Cell b : second.cells()) {
                        if (manhattan(a, b) != 1) {
                            continue;
                        }
                        // Same-trait adjacency links (existing system)
                        Set<String> common = new HashSet<>(first.piece().fusionTraits());
                        common.retainAll(second.piece().fusionTraits());
                        if (!common.isEmpty()) {
                            links.add(new TraitLink(a, b));
                            for (String trait : common) {
                                expectedTraits.merge(trait, 1, Integer::sum);
                                fusedTraits.merge(trait, 1, Integer::sum);
                            }
                        }
                        // TraitFusionRegistry cross-pair fusions (new system)
                        for (String traitA : first.piece().reagent().traits()) {
                            for (String traitB : second.piece().reagent().traits()) {
                                Optional<TraitFusionRule> rule = TraitFusionRegistry.find(traitA, traitB);
                                if (rule.isEmpty()) {
                                    continue;
                                }
                                int idMin = Math.min(first.id(), second.id());
                                int idMax = Math.max(first.id(), second.id());
                                String key = idMin + "|" + idMax + "|" + rule.get().id();
                                if (seenFusionKeys.add(key)) {
                                    fusionResults.add(new FusionResult(a, b, traitA, traitB, rule.get()));
                                    fusionCountPerPlacement.merge(first.id(), 1, Integer::sum);
                                    fusionCountPerPlacement.merge(second.id(), 1, Integer::sum);
                                }
                            }
                        }
                    }
                }
            }
        }

        Set<Integer> resonantIds = new HashSet<>();
        for (java.util.Map.Entry<Integer, Integer> e : fusionCountPerPlacement.entrySet()) {
            if (e.getValue() >= 2) {
                resonantIds.add(e.getKey());
            }
        }

        int qualityBonus = 0;
        boolean morphed = false;
        for (SynthesisBoard.Node node : board.nodes()) {
            Optional<Placement> cover = placementAt(node.x(), node.y());
            if (cover.isEmpty() || !nodeActive(node, cover.get())) {
                continue;
            }
            qualityBonus += node.qualityBonus();
            morphed = morphed || node.morphTarget().isPresent();
        }
        SpatialEvaluation result = new SpatialEvaluation(links, expectedTraits, fusedTraits, qualityBonus, morphed, fusionResults, resonantIds);
        lastEvaluation = result;
        return result;
    }

    SynthesisBoardFusionPayload buildFusionPayload(int containerId) {
        if (lastEvaluation == null || lastEvaluation.fusionResults().isEmpty()) {
            return new SynthesisBoardFusionPayload(containerId, List.of(), 0);
        }
        java.util.LinkedHashSet<String> ruleIds = new java.util.LinkedHashSet<>();
        for (FusionResult fr : lastEvaluation.fusionResults()) {
            ruleIds.add(fr.rule().id());
        }
        return new SynthesisBoardFusionPayload(
                containerId,
                new ArrayList<>(ruleIds),
                lastEvaluation.resonantPlacementIds().size()
        );
    }

    private Optional<Placement> placementAt(int x, int y) {
        for (Placement placement : placements) {
            if (placement.occupies(x, y)) {
                return Optional.of(placement);
            }
        }
        return Optional.empty();
    }

    private int occupiedCellCount() {
        Set<Cell> occupied = new HashSet<>();
        for (Placement placement : placements) {
            occupied.addAll(placement.cells());
        }
        return occupied.size();
    }

    private ReagentContainer placedReagentContainer() {
        ReagentContainer container = new ReagentContainer();
        for (Placement placement : placements) {
            container.insert(placement.piece().reagent());
        }
        return container;
    }

    private PaletteView paletteView(Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        List<ReagentStack> reagents = paletteSource == PaletteSource.STORAGE
                ? storageReagents.isEmpty() ? PrototypeReagentStock.eraOneStorageStock() : storageReagents
                : inventoryReagents;
        List<RequirementStatus> unsatisfied = unsatisfiedRequirements(plan);
        List<PaletteEntry> entries = reagents.stream()
                .map(Piece::new)
                .filter(this::matchesPaletteFilters)
                .map(piece -> new PaletteEntry(
                        piece,
                        matchesAnyUnsatisfiedRequirement(piece, unsatisfied),
                        hasFusionPotentialWithPlaced(piece),
                        primaryElementIndex(piece),
                        canFitOnCurrentBoard(piece, plan),
                        piece.shape().size()
                ))
                .filter(entry -> matchesAdvancedFilters(entry, plan))
                .sorted(paletteComparator(plan.isPresent()))
                .toList();
        String emptyMessage = reagents.isEmpty() ? "No reagents"
                : entries.isEmpty() ? "No matching reagents"
                : "";
        return new PaletteView(entries, emptyMessage);
    }

    private Optional<Integer> hoveredPaletteIndex(int localX, int localY, List<PaletteEntry> pieces) {
        int limit = Math.min(PALETTE_VISIBLE, Math.max(0, pieces.size() - paletteScroll));
        for (int i = 0; i < limit; i++) {
            if (paletteTile(new ScreenRect(0, 0, 0, 0), i).contains(localX, localY)) {
                return Optional.of(paletteScroll + i);
            }
        }
        return Optional.empty();
    }

    private Optional<Cell> boardCell(SynthesisBoard board, int localX, int localY) {
        ScreenRect rect = boardRect(board);
        if (!rect.contains(localX, localY)) {
            return Optional.empty();
        }
        return Optional.of(new Cell((localX - rect.x()) / CELL_SIZE, (localY - rect.y()) / CELL_SIZE));
    }

    private static ScreenRect cellRect(ScreenRect origin, SynthesisBoard board, int x, int y) {
        ScreenRect rect = boardRect(board);
        return new ScreenRect(rect.x() + origin.x() + x * CELL_SIZE, rect.y() + origin.y() + y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
    }

    private static ScreenRect paletteTile(ScreenRect origin, int index) {
        int column = index % PALETTE_COLUMNS;
        int row = index / PALETTE_COLUMNS;
        return new ScreenRect(
                PALETTE.x() + origin.x() + 5 + column * PALETTE_TILE_WIDTH,
                PALETTE.y() + origin.y() + 19 + row * PALETTE_TILE_HEIGHT,
                PALETTE_TILE_WIDTH - 3,
                PALETTE_TILE_HEIGHT - 2
        );
    }

    private static void drawMiniShape(GuiGraphics graphics, Piece piece, int x, int y) {
        int color = pieceColor(piece);
        for (Cell cell : piece.shapeCells()) {
            graphics.fill(x + cell.x() * 4, y + cell.y() * 4, x + cell.x() * 4 + 4, y + cell.y() * 4 + 4, color);
        }
    }

    private static void renderPaletteReasonPips(GuiGraphics graphics, ScreenRect tile, PaletteEntry entry) {
        int x = tile.x() + 19;
        int y = tile.bottom() - 6;
        if (entry.matchesUnsatisfiedRequirement()) {
            renderReasonPip(graphics, x, y, SynthesisScreenTheme.GOOD);
            x += 5;
        }
        if (entry.hasFusionPotential()) {
            renderReasonPip(graphics, x, y, 0xFFFFB454);
            x += 5;
        }
        if (entry.fitsCurrentBoard()) {
            renderReasonPip(graphics, x, y, 0xFF7FB7FF);
        }
    }

    private static void renderReasonPip(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 4, y + 4, 0xFF11100E);
        graphics.fill(x + 1, y + 1, x + 3, y + 3, color);
    }

    private static void drawFloatingShape(GuiGraphics graphics, Font font, Piece piece, int rotation, int x, int y, boolean includeLabel) {
        for (Cell cell : piece.rotatedCells(rotation, 0, 0)) {
            graphics.fill(x + cell.x() * 12, y + cell.y() * 12, x + cell.x() * 12 + 11, y + cell.y() * 12 + 11, pieceColor(piece));
        }
        if (includeLabel) {
            graphics.drawString(font, piece.label(), x, y + 38, SynthesisScreenTheme.TEXT, false);
        }
    }

    private static int pieceColor(Piece piece) {
        if (piece.hasElement("fire")) {
            return SynthesisNoun.color("fire", 0xFFE37A61);
        }
        if (piece.hasElement("water")) {
            return SynthesisNoun.color("water", 0xFF76B7E8);
        }
        if (piece.hasElement("earth")) {
            return SynthesisNoun.color("earth", 0xFFC5A66D);
        }
        if (piece.hasElement("wind")) {
            return SynthesisNoun.color("wind", 0xFF85D7A0);
        }
        return 0xFFB78AE4;
    }

    private static List<Component> pieceTooltip(PaletteEntry entry) {
        Piece piece = entry.piece();
        ArrayList<Component> lines = new ArrayList<>();
        lines.add(Component.literal(piece.label()));
        lines.add(Component.literal("Shape: " + piece.shape().id()));
        lines.add(Component.literal("Tier " + piece.reagent().tier() + "  Quality " + piece.reagent().quality()));
        lines.add(elementTooltipLine(piece.reagent()));
        lines.add(nounListLine("Fusion: ", piece.fusionTraits()));
        List<String> reasons = new ArrayList<>(3);
        if (entry.matchesUnsatisfiedRequirement()) {
            reasons.add("Need now");
        }
        if (entry.hasFusionPotential()) {
            reasons.add("Fusion ready");
        }
        if (entry.fitsCurrentBoard()) {
            reasons.add("Fits board");
        }
        if (!reasons.isEmpty()) {
            lines.add(Component.literal("Why here: " + String.join(", ", reasons)));
        }
        return lines;
    }

    private static Component elementTooltipLine(ReagentStack reagent) {
        if (reagent.elements().isEmpty()) {
            return Component.literal("Elements: none");
        }
        MutableComponent line = Component.literal("Elements: ");
        int index = 0;
        for (Map.Entry<String, Integer> entry : reagent.elements().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList()) {
            if (index++ > 0) {
                line.append(Component.literal(", "));
            }
            line.append(SynthesisNoun.component(entry.getKey()));
            line.append(Component.literal(" " + entry.getValue()));
        }
        return line;
    }

    private static Component nounListLine(String prefix, List<String> ids) {
        MutableComponent line = Component.literal(prefix);
        if (ids.isEmpty()) {
            return line.append(Component.literal("none"));
        }
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                line.append(Component.literal(", "));
            }
            line.append(SynthesisNoun.component(ids.get(i)));
        }
        return line;
    }

    private static int manhattan(Cell a, Cell b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    private static Component requirementLine(RequirementStatus status) {
        return SynthesisNoun.line(
                status.availableAmount(),
                "/",
                status.requirement().amount(),
                " ",
                summarizeRequirement(status.requirement().query())
        );
    }

    private static Component summarizeRequirement(ReagentQuery query) {
        ArrayList<Component> parts = new ArrayList<>();
        if (!query.reagentIds().isEmpty()) {
            parts.add(SynthesisNoun.component(query.reagentIds().stream().sorted().findFirst().orElse("reagent")));
        }
        if (!query.requiredCategories().isEmpty()) {
            parts.add(SynthesisNoun.component(query.requiredCategories().stream().sorted().findFirst().orElse("category")));
        }
        if (!query.minElements().isEmpty()) {
            parts.add(query.minElements().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .findFirst()
                    .map(entry -> SynthesisNoun.line(SynthesisNoun.component(entry.getKey()), " ", entry.getValue()))
                    .orElseGet(() -> Component.literal("Element")));
        }
        if (!query.requiredTraits().isEmpty()) {
            parts.add(SynthesisNoun.component(query.requiredTraits().stream().sorted().findFirst().orElse("trait")));
        }
        if (parts.isEmpty()) {
            return Component.literal("Any");
        }
        MutableComponent result = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                result.append(Component.literal(" + "));
            }
            result.append(parts.get(i));
        }
        return result;
    }

    private static boolean samePiece(Piece first, Piece second) {
        return first != null && second != null && first.reagent().reagentId().equals(second.reagent().reagentId());
    }

    private void togglePaletteElementFilter(String element) {
        if (!paletteElementFilters.add(element)) {
            paletteElementFilters.remove(element);
        }
    }

    private boolean matchesPaletteFilters(Piece piece) {
        for (String element : paletteElementFilters) {
            if (!piece.hasElement(element)) {
                return false;
            }
        }
        if (shapeFilterMode != ShapeFilterMode.ANY && !shapeFilterMode.matches(piece.shape())) {
            return false;
        }
        return true;
    }

    private boolean matchesAdvancedFilters(PaletteEntry entry, Optional<SynthesisPlan> plan) {
        if (filterNeedsOnly && !entry.matchesUnsatisfiedRequirement()) {
            return false;
        }
        if (filterFusionOnly && !entry.hasFusionPotential()) {
            return false;
        }
        if (filterFitsOnly && !entry.fitsCurrentBoard()) {
            return false;
        }
        Optional<RequirementStatus> selectedRequirement = selectedRequirementFilter(plan);
        if (selectedRequirement.isPresent() && !selectedRequirement.get().requirement().query().matches(entry.piece().reagent())) {
            return false;
        }
        return true;
    }

    private Optional<RequirementStatus> selectedRequirementFilter(Optional<SynthesisPlan> plan) {
        List<RequirementStatus> displayed = displayedRequirements(plan);
        if (selectedRequirementFilterIndex < 0 || selectedRequirementFilterIndex >= displayed.size()) {
            return Optional.empty();
        }
        return Optional.of(displayed.get(selectedRequirementFilterIndex));
    }

    private Optional<Integer> clickedRequirementFilterIndex(Optional<SynthesisPlan> plan, SynthesisBoard board, int localX, int localY) {
        List<ScreenRect> rects = requirementFilterRects(plan, board);
        for (int i = 0; i < rects.size(); i++) {
            RequirementStatus status = displayedRequirements(plan).get(i);
            if (!status.satisfied() && rects.get(i).contains(localX, localY)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private List<RequirementStatus> displayedRequirements(Optional<SynthesisPlan> plan) {
        if (plan.isEmpty()) {
            return List.of();
        }
        return displayedRequirements(new SynthesisPlanner().plan(plan.get().profile(), placedReagentContainer(), 0));
    }

    private List<ScreenRect> requirementFilterRects(Optional<SynthesisPlan> plan, SynthesisBoard board) {
        if (plan.isEmpty()) {
            return List.of();
        }
        SynthesisPlan placedPlan = new SynthesisPlanner().plan(plan.get().profile(), placedReagentContainer(), 0);
        return buildReadoutLayout(READOUT, placedPlan, evaluate(board)).requirementLines();
    }

    private static List<RequirementStatus> displayedRequirements(SynthesisPlan placedPlan) {
        return placedPlan.requirements().stream().limit(5).toList();
    }

    private static ScreenRect readoutLineRect(ScreenRect panel, int y) {
        return new ScreenRect(panel.x() + READOUT_INSET_X, y, panel.width() - READOUT_INSET_X * 2, READOUT_TEXT_HEIGHT);
    }

    private static ReadoutLayout buildReadoutLayout(ScreenRect panel, SynthesisPlan placedPlan, SpatialEvaluation spatial) {
        int y = panel.y() + READOUT_TOP_PADDING;
        ScreenRect successLabel = readoutLineRect(panel, y);
        ScreenRect successBar = new ScreenRect(successLabel.x(), y + READOUT_BAR_LABEL_GAP, successLabel.width(), READOUT_BAR_HEIGHT);
        y += READOUT_BAR_BLOCK_HEIGHT;

        Optional<ScreenRect> resonanceLine = Optional.empty();
        int resonanceRisk = spatial.resonantPlacementIds().size() * 15;
        if (resonanceRisk > 0) {
            resonanceLine = Optional.of(readoutLineRect(panel, y));
            y += READOUT_ROW_GAP;
        }

        ScreenRect perfectLabel = readoutLineRect(panel, y);
        ScreenRect perfectBar = new ScreenRect(perfectLabel.x(), y + READOUT_BAR_LABEL_GAP, perfectLabel.width(), READOUT_BAR_HEIGHT);
        y += READOUT_BAR_BLOCK_HEIGHT;

        ScreenRect fillLine = new ScreenRect(panel.x() + READOUT_INSET_X, y, READOUT_FILL_VALUE_WIDTH, READOUT_TEXT_HEIGHT);
        ScreenRect emptyLine = new ScreenRect(panel.x() + READOUT_FILL_SECONDARY_X, y, READOUT_FILL_VALUE_WIDTH, READOUT_TEXT_HEIGHT);
        y += 12;

        ScreenRect needsHeader = readoutLineRect(panel, y);
        y += READOUT_SECTION_GAP;
        List<RequirementStatus> displayedRequirements = displayedRequirements(placedPlan);
        ArrayList<ScreenRect> requirementLines = new ArrayList<>(displayedRequirements.size());
        for (int i = 0; i < displayedRequirements.size(); i++) {
            requirementLines.add(readoutLineRect(panel, y));
            y += READOUT_ROW_GAP;
        }

        Optional<ScreenRect> extraNeedsLine = Optional.empty();
        if (placedPlan.requirements().size() > displayedRequirements.size()) {
            extraNeedsLine = Optional.of(readoutLineRect(panel, y));
            y += READOUT_ROW_GAP;
        }

        y += READOUT_SECTION_PAD;
        ScreenRect traitsHeader = readoutLineRect(panel, y);
        y += READOUT_SECTION_GAP;

        Optional<ScreenRect> emptyTraitsLine = Optional.empty();
        ArrayList<ScreenRect> traitLines = new ArrayList<>();
        if (spatial.expectedTraits().isEmpty()) {
            emptyTraitsLine = Optional.of(readoutLineRect(panel, y));
            y += READOUT_ROW_GAP;
        } else {
            int traitCount = Math.min(3, spatial.expectedTraits().size());
            for (int i = 0; i < traitCount; i++) {
                traitLines.add(readoutLineRect(panel, y));
                y += READOUT_ROW_GAP;
            }
        }

        Optional<ScreenRect> fusionsHeader = Optional.empty();
        ArrayList<ScreenRect> fusionLines = new ArrayList<>();
        if (!spatial.fusionResults().isEmpty() && y + 11 <= panel.bottom()) {
            y += READOUT_SECTION_PAD;
            fusionsHeader = Optional.of(readoutLineRect(panel, y));
            y += READOUT_SECTION_GAP;
            while (fusionLines.size() < spatial.fusionResults().size() && y + READOUT_TEXT_HEIGHT <= panel.bottom()) {
                fusionLines.add(readoutLineRect(panel, y));
                y += READOUT_ROW_GAP;
            }
        }

        return new ReadoutLayout(
                successLabel,
                successBar,
                resonanceLine,
                perfectLabel,
                perfectBar,
                fillLine,
                emptyLine,
                needsHeader,
                requirementLines,
                extraNeedsLine,
                traitsHeader,
                emptyTraitsLine,
                traitLines,
                fusionsHeader,
                fusionLines
        );
    }

    private List<RequirementStatus> unsatisfiedRequirements(Optional<SynthesisPlan> plan) {
        if (plan.isEmpty()) {
            return List.of();
        }
        return new SynthesisPlanner().plan(plan.get().profile(), placedReagentContainer(), 0).requirements().stream()
                .filter(status -> !status.satisfied())
                .toList();
    }

    private boolean matchesAnyUnsatisfiedRequirement(Piece piece, List<RequirementStatus> unsatisfied) {
        for (RequirementStatus status : unsatisfied) {
            if (status.requirement().query().matches(piece.reagent())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFusionPotentialWithPlaced(Piece piece) {
        for (Placement placement : placements) {
            Set<String> shared = new HashSet<>(piece.fusionTraits());
            shared.retainAll(placement.piece().fusionTraits());
            if (!shared.isEmpty()) {
                return true;
            }
            for (String traitA : piece.reagent().traits()) {
                for (String traitB : placement.piece().reagent().traits()) {
                    if (TraitFusionRegistry.find(traitA, traitB).isPresent()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Comparator<PaletteEntry> paletteComparator(boolean hasPlan) {
        Comparator<PaletteEntry> tierThenId = Comparator
                .comparingInt((PaletteEntry entry) -> entry.piece().reagent().tier()).reversed()
                .thenComparingInt((PaletteEntry entry) -> entry.piece().reagent().quality()).reversed()
                .thenComparingInt((PaletteEntry entry) -> entry.fitsCurrentBoard() ? 0 : 1)
                .thenComparingInt((PaletteEntry entry) -> entry.shapeCellCount())
                .thenComparing(entry -> entry.piece().reagent().reagentId());
        return switch (paletteSortMode) {
            case TIER -> tierThenId;
            case ELEMENT -> Comparator
                    .comparingInt(PaletteEntry::primaryElementIndex)
                    .thenComparing(tierThenId);
            case RELEVANCE -> hasPlan
                    ? Comparator
                    .comparingInt((PaletteEntry entry) -> entry.matchesUnsatisfiedRequirement() ? 0 : entry.hasFusionPotential() ? 1 : 2)
                    .thenComparingInt((PaletteEntry entry) -> entry.fitsCurrentBoard() ? 0 : 1)
                    .thenComparingInt((PaletteEntry entry) -> entry.shapeCellCount())
                    .thenComparing(tierThenId)
                    : tierThenId;
        };
    }

    private static int primaryElementIndex(Piece piece) {
        for (int i = 0; i < CHIP_ELEMENTS.size(); i++) {
            if (piece.hasElement(CHIP_ELEMENTS.get(i))) {
                return i;
            }
        }
        return CHIP_ELEMENTS.size();
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private boolean hasAdvancedFilters() {
        return filterNeedsOnly || filterFusionOnly || filterFitsOnly || shapeFilterMode != ShapeFilterMode.ANY;
    }

    private void resetPaletteFilters() {
        paletteElementFilters.clear();
        filterNeedsOnly = false;
        filterFusionOnly = false;
        filterFitsOnly = false;
        shapeFilterMode = ShapeFilterMode.ANY;
    }

    private boolean canFitOnCurrentBoard(Piece piece, Optional<SynthesisPlan> plan) {
        SynthesisBoard board = currentBoard(plan);
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int y = 0; y < board.size(); y++) {
                for (int x = 0; x < board.size(); x++) {
                    if (canPlace(board, piece, rotation, x, y, -1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Cell defaultCursorCell(Piece piece, int rotation) {
        return piece.rotatedCells(rotation, 0, 0).stream()
                .min(Comparator.comparingInt(Cell::y).thenComparingInt(Cell::x))
                .orElse(new Cell(0, 0));
    }

    private static ScreenRect boardRect(SynthesisBoard board) {
        int width = board.size() * CELL_SIZE;
        int height = board.size() * CELL_SIZE;
        return new ScreenRect(
                BOARD_AREA.x() + (BOARD_AREA.width() - width) / 2,
                BOARD_AREA.y() + (BOARD_AREA.height() - height) / 2,
                width,
                height
        );
    }

    private static boolean within(SynthesisBoard board, int x, int y) {
        return x >= 0 && x < board.size() && y >= 0 && y < board.size();
    }

    private void syncSelectedBoard(Optional<SynthesisPlan> plan) {
        String profileId = plan.map(value -> value.profile().id()).orElse("");
        if (profileId.equals(activeProfileId)) {
            return;
        }
        activeProfileId = profileId;
        placements.clear();
        clearCarried();
        selectedRequirementFilterIndex = -1;
    }

    private static SynthesisBoard currentBoard(Optional<SynthesisPlan> plan) {
        return plan.map(value -> value.profile().board()).orElse(SynthesisBoard.CRUDE_3X3);
    }

    private static boolean nodeActive(SynthesisBoard.Node node, Placement placement) {
        if (node.requiredElement().isEmpty()) {
            return true;
        }
        return placement.piece().reagent().elements().getOrDefault(node.requiredElement().get(), 0) >= node.requiredElementValue();
    }

    private static int nodeColor(SynthesisBoard.Node node) {
        if (node.morphTarget().isPresent() || node.type().toLowerCase(Locale.ROOT).contains("morph")) {
            return 0xFFE2A35D;
        }
        return 0xFF8EDC76;
    }

    private static String nodeLabel(SynthesisBoard.Node node) {
        if (node.morphTarget().isPresent() || node.type().toLowerCase(Locale.ROOT).contains("morph")) {
            return "M";
        }
        return "E";
    }

    private record Piece(ReagentStack reagent) {
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

    private record Placement(int id, Piece piece, int rotation, int anchorX, int anchorY) {
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

    private record Cell(int x, int y) {
    }

    private record PlacementPreview(int anchorX, int anchorY, Cell cursorCell, boolean valid) {
    }

    private enum PaletteSource {
        STORAGE,
        INVENTORY
    }

    private enum PaletteSortMode {
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

    private enum ShapeFilterMode {
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

        private static final List<ShapeFilterMode> VALUES = List.of(values());
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
    }

    private record PaletteEntry(
            Piece piece,
            boolean matchesUnsatisfiedRequirement,
            boolean hasFusionPotential,
            int primaryElementIndex,
            boolean fitsCurrentBoard,
            int shapeCellCount
    ) {
    }

    private record PaletteView(List<PaletteEntry> entries, String emptyMessage) {
    }

    private record TraitLink(Cell from, Cell to) {
    }

    private record FusionResult(Cell from, Cell to, String traitA, String traitB, TraitFusionRule rule) {
    }

    private record SpatialEvaluation(List<TraitLink> links, Map<String, Integer> expectedTraits, Map<String, Integer> fusedTraits, int qualityBonus, boolean morphed, List<FusionResult> fusionResults, Set<Integer> resonantPlacementIds) {
    }

    private record ReadoutLayout(
            ScreenRect successLabel,
            ScreenRect successBar,
            Optional<ScreenRect> resonanceLine,
            ScreenRect perfectLabel,
            ScreenRect perfectBar,
            ScreenRect fillLine,
            ScreenRect emptyLine,
            ScreenRect needsHeader,
            List<ScreenRect> requirementLines,
            Optional<ScreenRect> extraNeedsLine,
            ScreenRect traitsHeader,
            Optional<ScreenRect> emptyTraitsLine,
            List<ScreenRect> traitLines,
            Optional<ScreenRect> fusionsHeader,
            List<ScreenRect> fusionLines
    ) {
    }

    private static boolean isLine(List<ReagentShape.Cell> cells) {
        boolean sameX = cells.stream().mapToInt(ReagentShape.Cell::x).distinct().count() == 1;
        boolean sameY = cells.stream().mapToInt(ReagentShape.Cell::y).distinct().count() == 1;
        return sameX || sameY;
    }

    private static boolean hasCorner(List<ReagentShape.Cell> cells) {
        Set<String> set = new HashSet<>();
        for (ReagentShape.Cell cell : cells) {
            set.add(cell.x() + "," + cell.y());
        }
        for (ReagentShape.Cell cell : cells) {
            boolean right = set.contains((cell.x() + 1) + "," + cell.y());
            boolean left = set.contains((cell.x() - 1) + "," + cell.y());
            boolean up = set.contains(cell.x() + "," + (cell.y() - 1));
            boolean down = set.contains(cell.x() + "," + (cell.y() + 1));
            if ((right || left) && (up || down)) {
                return true;
            }
        }
        return false;
    }

}
