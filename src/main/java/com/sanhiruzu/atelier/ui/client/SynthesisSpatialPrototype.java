package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.data.TraitFusionRegistry;
import com.sanhiruzu.atelier.synthesis.data.TraitFusionRule;
import com.sanhiruzu.atelier.synthesis.engine.ResolvedFusionData;
import com.sanhiruzu.atelier.synthesis.engine.OutcomePreview;
import com.sanhiruzu.atelier.synthesis.engine.RequirementStatus;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisBoard;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirementMatcher;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import com.sanhiruzu.atelier.ui.client.SynthesisBoardSession.Cell;
import com.sanhiruzu.atelier.ui.client.SynthesisBoardSession.PaletteSortMode;
import com.sanhiruzu.atelier.ui.client.SynthesisBoardSession.PaletteSource;
import com.sanhiruzu.atelier.ui.client.SynthesisBoardSession.Piece;
import com.sanhiruzu.atelier.ui.client.SynthesisBoardSession.Placement;
import com.sanhiruzu.atelier.ui.client.SynthesisBoardSession.PlacementPreview;
import com.sanhiruzu.atelier.ui.client.SynthesisBoardSession.ShapeFilterMode;
import com.sanhiruzu.atelier.ui.network.SynthesisBoardFusionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    // Inventory slot rows sit at y=220, 6px above the palette. This constant is used to
    // extend click and cover regions upward so that gap doesn't leak to vanilla slot handling.
    private static final int SLOTS_TOP = 220;
    private static final ScreenRect CHIP_FILTER = new ScreenRect(8, 233, 40, 12);
    private static final ScreenRect PALETTE_STORAGE_TAB = new ScreenRect(58, 233, 62, 12);
    private static final ScreenRect PALETTE_INVENTORY_TAB = new ScreenRect(122, 233, 70, 12);
    private static final ScreenRect CHIP_FIRE  = new ScreenRect(200, 233, 40, 12);
    private static final ScreenRect CHIP_WATER = new ScreenRect(242, 233, 40, 12);
    private static final ScreenRect CHIP_EARTH = new ScreenRect(284, 233, 40, 12);
    private static final ScreenRect CHIP_WIND  = new ScreenRect(326, 233, 40, 12);
    private static final ScreenRect CHIP_SORT   = new ScreenRect(370, 233, 36, 12);
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
    private static final int READOUT_BAR_LABEL_GAP = 9;
    private static final int REQUIREMENT_BAR_HEIGHT = 5;
    private static final int REQUIREMENT_ROW_HEIGHT = 17;
    private static final int READOUT_ROW_GAP = 9;
    private static final int READOUT_SECTION_GAP = 10;
    private static final int READOUT_SECTION_PAD = 2;
    private static final int READOUT_FILL_VALUE_WIDTH = 70;
    private static final int READOUT_FILL_SECONDARY_X = 80;
    private static final int BOARD_MODE_OUTER_PADDING = 16;
    private static final int BOARD_MODE_PLACEMENT_INSET = 6;
    private static final int BOARD_MODE_PROGRESS_FOOTER_HEIGHT = 68;
    private static final int BOARD_MODE_PALETTE_COLUMNS = 3;
    private static final int BOARD_MODE_PALETTE_TILE_SIZE = 32;
    private static final int BOARD_MODE_PALETTE_GAP = 4;

    private final SynthesisBoardSession session = new SynthesisBoardSession();
    private SpatialEvaluation lastEvaluation = null;

    void render(
            GuiGraphics graphics,
            Font font,
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents,
            ScreenRect origin
    ) {
        syncSelectedBoard(plan);
        SynthesisBoard board = currentBoard(plan);
        renderBoard(graphics, font, board, origin);
        renderPlacements(graphics, font, board, origin);
        renderReadout(graphics, font, plan, storageReagents, inventoryReagents, board, origin);
    }

    void renderBoardMode(
            GuiGraphics graphics,
            Font font,
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents,
            ScreenRect origin,
            ScreenRect boardArea,
            ScreenRect palettePanel,
            ScreenRect progressPanel
    ) {
        syncSelectedBoard(plan);
        SynthesisBoard board = currentBoard(plan);
        ScreenRect absoluteBoardArea = boardArea.offset(origin.x(), origin.y());
        ScreenRect boardRect = boardRectForArea(absoluteBoardArea, board);
        renderBoardInRect(graphics, font, board, boardRect);
        renderPlacementsInRect(graphics, font, board, boardRect);
        renderBoardModePalette(graphics, font, plan, storageReagents, inventoryReagents, palettePanel.offset(origin.x(), origin.y()));
        renderBoardModeProgress(graphics, font, plan, storageReagents, inventoryReagents, board, progressPanel.offset(origin.x(), origin.y()));
    }

    void renderOverlay(GuiGraphics graphics, Font font, Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents, ScreenRect origin, int mouseX, int mouseY) {
        graphics.flush();
        UiLayer.ABOVE_ITEMS.run(graphics, () ->
            renderPalette(graphics, font, paletteView(plan, storageReagents, inventoryReagents), origin, mouseX, mouseY)
        );
        UiLayer.CARRIED.run(graphics, () ->
                renderCarriedGhost(graphics, font, currentBoard(plan), origin, mouseX, mouseY)
        );
        graphics.flush();
    }

    void renderBoardModeOverlay(GuiGraphics graphics, Font font, Optional<SynthesisPlan> plan, ScreenRect origin, ScreenRect boardArea, int mouseX, int mouseY) {
        UiLayer.CARRIED.run(graphics, () ->
                renderCarriedGhostInRect(graphics, font, currentBoard(plan), boardRectForArea(boardArea.offset(origin.x(), origin.y()), currentBoard(plan)), mouseX, mouseY)
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
        if (session.hasCarried() && boardCell.isEmpty()) {
            clearCarried();
            return true;
        }
        Optional<Integer> requirementFilter = clickedRequirementFilterIndex(plan, board, localX, localY, storageReagents, inventoryReagents);
        if (requirementFilter.isPresent()) {
            session.toggleSelectedRequirementFilter(requirementFilter.get());
            return true;
        }
        if (PALETTE_STORAGE_TAB.contains(localX, localY)) {
            session.setPaletteSource(PaletteSource.STORAGE);
            return true;
        }
        if (PALETTE_INVENTORY_TAB.contains(localX, localY)) {
            session.setPaletteSource(PaletteSource.INVENTORY);
            return true;
        }
        for (int i = 0; i < CHIP_ELEMENTS.size(); i++) {
            if (CHIP_ELEMENT_RECTS.get(i).contains(localX, localY)) {
                session.togglePaletteElementFilter(CHIP_ELEMENTS.get(i));
                return true;
            }
        }
        if (CHIP_SORT.contains(localX, localY)) {
            session.advancePaletteSortMode();
            return true;
        }
        if (CHIP_FILTER.contains(localX, localY)) {
            session.togglePaletteFiltersOpen();
            return true;
        }
        if (session.paletteFiltersOpen()) {
            ScreenRect drawer = filterDrawerLocal(origin);
            if (drawerControlRect(FILTER_NEEDS, drawer).contains(localX, localY)) {
                session.toggleFilterNeedsOnly();
                return true;
            }
            if (drawerControlRect(FILTER_FUSION, drawer).contains(localX, localY)) {
                session.toggleFilterFusionOnly();
                return true;
            }
            if (drawerControlRect(FILTER_FITS, drawer).contains(localX, localY)) {
                session.toggleFilterFitsOnly();
                return true;
            }
            for (int i = 0; i < ShapeFilterMode.VALUES.size(); i++) {
                if (drawerControlRect(FILTER_SHAPE_RECTS.get(i), drawer).contains(localX, localY)) {
                    session.setShapeFilterMode(ShapeFilterMode.VALUES.get(i));
                    return true;
                }
            }
            if (drawerControlRect(FILTER_RESET, drawer).contains(localX, localY)) {
                session.resetPaletteFilters();
                return true;
            }
            // Drawer is open — consume any click inside it so widgets behind it can't fire.
            if (drawer.contains(localX, localY)) {
                return true;
            }
        }
        PaletteView palette = paletteView(plan, storageReagents, inventoryReagents);
        Optional<Integer> paletteIndex = hoveredPaletteIndex(localX, localY, palette.entries());
        if (paletteIndex.isPresent()) {
            PaletteEntry selectedEntry = palette.entries().get(paletteIndex.get());
            if (selectedEntry.fullyPlaced()) {
                return true;
            }
            Piece selected = selectedEntry.piece();
            if (shiftDown) {
                if (!autoPlace(board, selected)) {
                    startCarrying(selected, 0);
                }
            } else {
                startCarrying(selected, 0);
            }
            return true;
        }
        if (boardCell.isEmpty()) {
            // Extend the catch-all upward to SLOTS_TOP so the 6px strip above the palette
            // (where inventory row tops sit) doesn't leak clicks to vanilla slot handling.
            return localX >= PALETTE.x() && localX < PALETTE.right()
                    && localY >= SLOTS_TOP && localY < PALETTE.bottom();
        }
        if (session.hasCarried()) {
            Piece carried = session.carried().orElseThrow();
            int carriedRotation = session.carriedRotation();
            if (shiftDown) {
                Optional<PlacementPreview> preview = overwritePreview(board, carried, carriedRotation, boardCell.get());
                if (preview.isPresent()) {
                    removeOverlappedPlacements(carried, carriedRotation, preview.get());
                    session.addPlacement(carried, carriedRotation, preview.get().anchorX(), preview.get().anchorY());
                    clearCarried();
                }
                return true;
            }
            PlacementPreview preview = placementPreview(board, carried, carriedRotation, boardCell.get());
            if (preview.valid()) {
                session.addPlacement(carried, carriedRotation, preview.anchorX(), preview.anchorY());
                clearCarried();
            }
            return true;
        }
        Optional<Placement> existing = placementAt(boardCell.get().x(), boardCell.get().y());
        if (existing.isPresent()) {
            Placement placement = existing.get();
            session.removePlacement(placement);
            startCarrying(
                    placement.piece(),
                    placement.rotation(),
                    placement.localCellAt(boardCell.get()).orElse(defaultCursorCell(placement.piece(), placement.rotation()))
            );
            return true;
        }
        return true;
    }

    boolean mouseClickedBoardMode(
            double mouseX,
            double mouseY,
            int button,
            boolean shiftDown,
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents,
            ScreenRect origin,
            ScreenRect boardArea,
            ScreenRect palettePanel
    ) {
        syncSelectedBoard(plan);
        if (button != LEFT_BUTTON && button != RIGHT_BUTTON) {
            return false;
        }
        SynthesisBoard board = currentBoard(plan);
        ScreenRect absoluteBoardArea = boardArea.offset(origin.x(), origin.y());
        Optional<Cell> boardCell = boardModeCellAt(absoluteBoardArea, board, (int) mouseX, (int) mouseY);
        if (boardCell.isPresent()) {
            return button == RIGHT_BUTTON
                    ? rotateAtCell(board, boardCell.get())
                    : handleBoardCellClick(board, boardCell.get(), shiftDown);
        }
        if (button != LEFT_BUTTON) {
            return false;
        }
        ScreenRect absolutePalette = palettePanel.offset(origin.x(), origin.y());
        if (handleBoardModePaletteControlClick(absolutePalette, (int) mouseX, (int) mouseY)) {
            return true;
        }
        PaletteView palette = paletteView(plan, storageReagents, inventoryReagents);
        Optional<Integer> paletteIndex = hoveredBoardModePaletteIndex(absolutePalette, (int) mouseX, (int) mouseY, palette.entries());
        if (paletteIndex.isPresent()) {
            PaletteEntry selectedEntry = palette.entries().get(paletteIndex.get());
            if (selectedEntry.fullyPlaced()) {
                return true;
            }
            Piece selected = selectedEntry.piece();
            if (shiftDown) {
                if (!autoPlace(board, selected)) {
                    startCarrying(selected, 0);
                }
            } else {
                startCarrying(selected, 0);
            }
            return true;
        }
        if (session.hasCarried()) {
            clearCarried();
            return true;
        }
        return absolutePalette.contains((int) mouseX, (int) mouseY);
    }

    boolean mouseScrolledBoardMode(
            double mouseX,
            double mouseY,
            double scrollY,
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents,
            ScreenRect origin,
            ScreenRect palettePanel
    ) {
        ScreenRect absolutePalette = palettePanel.offset(origin.x(), origin.y());
        if (!absolutePalette.contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        List<PaletteEntry> pieces = paletteView(plan, storageReagents, inventoryReagents).entries();
        int maxScroll = Math.max(0, pieces.size() - boardModePaletteVisibleCount(absolutePalette));
        if (maxScroll <= 0) {
            return true;
        }
        int step = scrollY < 0.0D ? BOARD_MODE_PALETTE_COLUMNS : -BOARD_MODE_PALETTE_COLUMNS;
        session.adjustPaletteScroll(step, maxScroll);
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
        session.adjustPaletteScroll(step, maxScroll);
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
        if (!session.hasCarried()) {
            return false;
        }
        Piece carried = session.carried().orElseThrow();
        int carriedRotation = session.carriedRotation();
        if (hover.isPresent()) {
            Cell target = hover.get();
            for (int offset = 1; offset < 4; offset++) {
                int candidateRotation = (carriedRotation + offset) & 3;
                PlacementPreview preview = placementPreview(board, carried, candidateRotation, target);
                if (preview.valid()) {
                    session.setCarriedRotation(candidateRotation, preview.cursorCell());
                    return true;
                }
            }
        }
        int nextRotation = (carriedRotation + 1) & 3;
        Cell nextCursorCell = hover
                .map(cell -> placementPreview(board, carried, nextRotation, cell).cursorCell())
                .orElseGet(() -> defaultCursorCell(carried, nextRotation));
        session.setCarriedRotation(nextRotation, nextCursorCell);
        return true;
    }

    boolean renderTooltip(GuiGraphics graphics, Font font, Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents, ScreenRect origin, int mouseX, int mouseY) {
        SynthesisBoard board = currentBoard(plan);
        int localX = mouseX - origin.x();
        int localY = mouseY - origin.y();
        if (session.paletteFiltersOpen() && filterDrawerLocal(origin).contains(localX, localY)) {
            return true;
        }
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
            graphics.renderComponentTooltip(font, synthesisTooltip(placement.get().piece(), Optional.empty(), plan, true), mouseX, mouseY);
            return true;
        }
        return false;
    }

    boolean renderBoardModeTooltip(
            GuiGraphics graphics,
            Font font,
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents,
            ScreenRect origin,
            ScreenRect boardArea,
            ScreenRect palettePanel,
            int mouseX,
            int mouseY
    ) {
        Optional<List<Component>> tooltip = boardModeTooltip(plan, storageReagents, inventoryReagents, origin, boardArea, palettePanel, mouseX, mouseY);
        if (tooltip.isEmpty()) {
            return false;
        }
        graphics.renderComponentTooltip(font, tooltip.get(), mouseX, mouseY);
        return true;
    }

    List<String> debugBoardModeTooltipLines(
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents,
            ScreenRect origin,
            ScreenRect boardArea,
            ScreenRect palettePanel,
            int mouseX,
            int mouseY
    ) {
        return boardModeTooltip(plan, storageReagents, inventoryReagents, origin, boardArea, palettePanel, mouseX, mouseY)
                .orElse(List.of())
                .stream()
                .map(Component::getString)
                .toList();
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
        for (Placement placement : session.placements()) {
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
        for (Placement placement : session.placements()) {
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

    private void renderBoardInRect(GuiGraphics graphics, Font font, SynthesisBoard synthesisBoard, ScreenRect boardRect) {
        graphics.fill(boardRect.x() - 1, boardRect.y() - 1, boardRect.right() + 1, boardRect.bottom() + 1, 0xFF0F0D0B);
        int cellSize = boardRect.width() / Math.max(1, synthesisBoard.size());
        for (int y = 0; y < synthesisBoard.size(); y++) {
            for (int x = 0; x < synthesisBoard.size(); x++) {
                ScreenRect cell = cellRectInBoard(boardRect, synthesisBoard, x, y);
                graphics.fill(cell.x(), cell.y(), cell.right(), cell.bottom(), 0xFF191613);
                SynthesisStationDrawing.frame(graphics, cell, 0xFF40372F);
            }
        }
        for (SynthesisBoard.Node node : synthesisBoard.nodes()) {
            int color = nodeColor(node);
            ScreenRect cell = cellRectInBoard(boardRect, synthesisBoard, node.x(), node.y()).inset(Math.max(3, cellSize / 5));
            graphics.fill(cell.x(), cell.y(), cell.right(), cell.bottom(), 0x66000000 | (color & 0x00FFFFFF));
            SynthesisStationDrawing.frame(graphics, cell, color);
            graphics.drawCenteredString(font, nodeLabel(node), cell.x() + cell.width() / 2, cell.y() + Math.max(2, cell.height() / 2 - 4), color);
        }
    }

    private void renderPlacementsInRect(GuiGraphics graphics, Font font, SynthesisBoard board, ScreenRect boardRect) {
        SpatialEvaluation evaluation = evaluate(board);
        for (TraitLink link : evaluation.links()) {
            ScreenRect a = cellRectInBoard(boardRect, board, link.from().x(), link.from().y());
            ScreenRect b = cellRectInBoard(boardRect, board, link.to().x(), link.to().y());
            int ax = a.x() + a.width() / 2;
            int ay = a.y() + a.height() / 2;
            int bx = b.x() + b.width() / 2;
            int by = b.y() + b.height() / 2;
            graphics.fill(Math.min(ax, bx) - 1, Math.min(ay, by) - 1, Math.max(ax, bx) + 1, Math.max(ay, by) + 1, 0xFFB6F08C);
        }
        for (FusionResult fusion : evaluation.fusionResults()) {
            ScreenRect a = cellRectInBoard(boardRect, board, fusion.from().x(), fusion.from().y());
            ScreenRect b = cellRectInBoard(boardRect, board, fusion.to().x(), fusion.to().y());
            int ax = a.x() + a.width() / 2;
            int ay = a.y() + a.height() / 2;
            int bx = b.x() + b.width() / 2;
            int by = b.y() + b.height() / 2;
            int color = fusion.rule().color() | 0xFF000000;
            graphics.fill(Math.min(ax, bx) - 1, Math.min(ay, by) - 1, Math.max(ax, bx) + 1, Math.max(ay, by) + 1, color);
        }
        for (Placement placement : session.placements()) {
            boolean resonant = evaluation.resonantPlacementIds().contains(placement.id());
            for (Cell cell : placement.cells()) {
                if (!within(board, cell.x(), cell.y())) {
                    continue;
                }
                ScreenRect rect = cellRectInBoard(boardRect, board, cell.x(), cell.y());
                int fill = resonant ? 0xFFE6B66A : pieceColor(placement.piece());
                int inset = Math.max(BOARD_MODE_PLACEMENT_INSET, rect.width() / 7);
                graphics.fill(rect.x() + inset, rect.y() + inset, rect.right() - inset, rect.bottom() - inset, fill);
                SynthesisStationDrawing.frame(graphics, rect.inset(Math.max(3, inset - 2)), resonant ? 0xFFFFE0A0 : 0xAA7FD889);
            }
            ScreenRect bounds = placementBoundsInBoard(boardRect, board, placement);
            ItemStack stack = ReagentItem.createStack(placement.piece().reagent());
            graphics.renderFakeItem(stack, bounds.x() + (bounds.width() - 16) / 2, bounds.y() + (bounds.height() - 16) / 2);
        }
    }

    private void renderBoardModePalette(
            GuiGraphics graphics,
            Font font,
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents,
            ScreenRect panel
    ) {
        SynthesisStationDrawing.smallIcon(graphics, panel.x() + 5, panel.y() + 5, 0xFF8FC9FF);
        SynthesisStationText.drawFit(graphics, font, "Materials", new ScreenRect(panel.x() + 18, panel.y() + 5, panel.width() - 61, 9), SynthesisScreenTheme.ACCENT);
        renderBoardModePaletteControls(graphics, font, panel);
        List<PaletteEntry> entries = paletteView(plan, storageReagents, inventoryReagents).entries();
        if (entries.isEmpty()) {
            graphics.drawString(font, "No matching reagents", panel.x() + 6, panel.y() + 44, SynthesisScreenTheme.MUTED, false);
            return;
        }
        int visibleCount = boardModePaletteVisibleCount(panel);
        int maxScroll = Math.max(0, entries.size() - visibleCount);
        session.clampPaletteScroll(maxScroll);
        int paletteScroll = session.paletteScroll();
        int limit = Math.min(visibleCount, entries.size() - paletteScroll);
        for (int i = 0; i < limit; i++) {
            int entryIndex = paletteScroll + i;
            ScreenRect tile = boardModePaletteTile(panel, i);
            PaletteEntry entry = entries.get(entryIndex);
            Piece piece = entry.piece();
            int face = entry.fullyPlaced() ? 0xFF151311
                    : samePiece(session.carried().orElse(null), piece) ? 0xFF3B332D
                    : 0xFF1C1917;
            graphics.fill(tile.x(), tile.y(), tile.right(), tile.bottom(), face);
            int borderColor = entry.fullyPlaced() ? 0xFF5A524A
                    : entry.matchesUnsatisfiedRequirement() ? SynthesisScreenTheme.GOOD
                    : entry.hasFusionPotential() ? 0xFFFFB454
                    : 0xFF4A4037;
            SynthesisStationDrawing.frame(graphics, tile, borderColor);
            graphics.renderFakeItem(ReagentItem.createStack(piece.reagent()), tile.x() + (tile.width() - 16) / 2, tile.y() + 6);
            renderPaletteReasonPips(graphics, tile, entry);
            renderPaletteShapeBadge(graphics, tile, piece);
            renderBoardModePotencyBadge(graphics, font, tile, entry);
        }
        if (entries.size() > visibleCount) {
            String range = (paletteScroll + 1) + "-" + (paletteScroll + limit) + "/" + entries.size();
            graphics.drawString(font, range, panel.right() - font.width(range) - 6, panel.y() + 7, SynthesisScreenTheme.MUTED, false);
        }
    }

    private void renderBoardModeProgress(
            GuiGraphics graphics,
            Font font,
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents,
            SynthesisBoard board,
            ScreenRect panel
    ) {
        SynthesisStationDrawing.smallIcon(graphics, panel.x() + 6, panel.y() + 5, 0xFF8FD69A);
        SynthesisStationText.drawFit(graphics, font, "Requirements", new ScreenRect(panel.x() + 18, panel.y() + 5, panel.width() - 24, 9), SynthesisScreenTheme.ACCENT);
        if (plan.isEmpty()) {
            graphics.drawString(font, "No recipe", panel.x() + 6, panel.y() + 24, SynthesisScreenTheme.MUTED, false);
            return;
        }
        SynthesisBoardProjection projection = projection(plan, storageReagents, inventoryReagents);
        SynthesisPlan placedPlan = projection.placedPlan().orElse(plan.get());
        SpatialEvaluation spatial = evaluate(board);
        SynthesisDisplayModel display = SynthesisDisplayModel.from(
                placedPlan,
                projection.placedReagents().entries(),
                traitTextLines(spatial),
                resonanceTextLines(spatial)
        );
        BoardModeProgressLayout layout = buildBoardModeProgressLayout(panel, display);
        SynthesisStationText.drawFit(graphics, font, "Essences", layout.essencesHeader(), SynthesisScreenTheme.ACCENT);
        for (int i = 0; i < layout.essenceLines().size(); i++) {
            SynthesisDisplayModel.Line line = display.essences().get(i);
            renderDisplayProgress(graphics, font, line, layout.essenceLines().get(i), line.satisfied() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.BAD);
        }
        layout.elementsHeader().ifPresent(rect ->
                SynthesisStationText.drawFit(graphics, font, "Elements", rect, display.elements().stream().allMatch(SynthesisDisplayModel.Line::satisfied) ? SynthesisScreenTheme.ACCENT : SynthesisScreenTheme.BAD));
        for (int i = 0; i < layout.elementLines().size(); i++) {
            SynthesisDisplayModel.Line line = display.elements().get(i);
            renderDisplayProgress(graphics, font, line, layout.elementLines().get(i), line.satisfied() ? SynthesisScreenTheme.GOOD : SynthesisNoun.color(line.label().toLowerCase(Locale.ROOT), SynthesisScreenTheme.BAD));
        }
        graphics.fill(layout.divider().x(), layout.divider().y(), layout.divider().right(), layout.divider().bottom(), 0x885A4C40);
        SynthesisStationText.drawFit(graphics, font, "Traits", layout.traitsHeader(), display.traits().stream().anyMatch(SynthesisDisplayModel.TextLine::active) ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.MUTED);
        for (int i = 0; i < layout.traitLines().size(); i++) {
            SynthesisDisplayModel.TextLine line = display.traits().get(i);
            SynthesisStationText.drawFit(graphics, font, line.text(), layout.traitLines().get(i), line.active() ? SynthesisScreenTheme.TEXT : SynthesisScreenTheme.MUTED);
        }
        SynthesisStationText.drawFit(graphics, font, "Resonance", layout.resonanceHeader(), display.resonance().stream().anyMatch(SynthesisDisplayModel.TextLine::active) ? 0xFFFFB454 : SynthesisScreenTheme.MUTED);
        for (int i = 0; i < layout.resonanceLines().size(); i++) {
            SynthesisDisplayModel.TextLine line = display.resonance().get(i);
            SynthesisStationText.drawFit(graphics, font, line.text(), layout.resonanceLines().get(i), line.active() ? 0xFFFFB454 : SynthesisScreenTheme.MUTED);
        }
        int occupied = occupiedCellCount();
        graphics.drawString(font,
                SynthesisStationText.fitWidth(font, footerOutcomeText(placedPlan.preview(), occupied > 0), panel.width() - 12),
                panel.x() + 6,
                layout.footerTop() + 4,
                placedPlan.canSynthesize() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.MUTED,
                false);
        graphics.drawString(font,
                SynthesisStationText.fitWidth(font, buildEmptyText(board.size() * board.size() - occupied, board), panel.width() - 12),
                panel.x() + 6,
                layout.footerTop() + 16,
                SynthesisScreenTheme.MUTED,
                false);
        if (spatial.qualityBonus() > 0 || spatial.perfectBonus() > 0) {
            graphics.drawString(font,
                    SynthesisStationText.fitWidth(font, buildNodeBonusText(spatial.qualityBonus(), spatial.perfectBonus()), panel.width() - 12),
                    panel.x() + 6,
                    layout.footerTop() + 28,
                    SynthesisScreenTheme.ACCENT,
                    false);
        }
    }

    private void renderBoardModePaletteControls(GuiGraphics graphics, Font font, ScreenRect panel) {
        renderPaletteChip(graphics, font, boardModeControlRect(panel, 0), "Need", session.filterNeedsOnly(), SynthesisScreenTheme.GOOD);
        renderPaletteChip(graphics, font, boardModeControlRect(panel, 1), "Fit", session.filterFitsOnly(), 0xFF7FB7FF);
        renderPaletteChip(graphics, font, boardModeControlRect(panel, 2), "Fuse", session.filterFusionOnly(), 0xFFFFB454);
        renderPaletteChip(graphics, font, boardModeControlRect(panel, 3), session.paletteSortMode().label(), true, SynthesisScreenTheme.ACCENT_DIM);
    }

    private void renderPalette(GuiGraphics graphics, Font font, PaletteView paletteView, ScreenRect origin, int mouseX, int mouseY) {
        ScreenRect palette = PALETTE.offset(origin.x(), origin.y());
        graphics.fill(palette.x(), palette.y(), palette.right(), palette.bottom(), 0xFF171411);
        SynthesisStationDrawing.frame(graphics, palette, 0xFF4F453C);
        renderPaletteTab(graphics, font, PALETTE_STORAGE_TAB.offset(origin.x(), origin.y()), "Storage", session.paletteSource() == PaletteSource.STORAGE);
        renderPaletteTab(graphics, font, PALETTE_INVENTORY_TAB.offset(origin.x(), origin.y()), "Inventory", session.paletteSource() == PaletteSource.INVENTORY);
        for (int i = 0; i < CHIP_ELEMENTS.size(); i++) {
            String element = CHIP_ELEMENTS.get(i);
            renderPaletteChip(graphics, font, CHIP_ELEMENT_RECTS.get(i).offset(origin.x(), origin.y()),
                    capitalize(element), session.paletteElementFilters().contains(element), SynthesisNoun.color(element, SynthesisScreenTheme.ACCENT));
        }
        renderPaletteChip(graphics, font, CHIP_FILTER.offset(origin.x(), origin.y()),
                "Filter", session.paletteFiltersOpen() || hasAdvancedFilters(), 0xFFCFAE6A);
        renderPaletteChip(graphics, font, CHIP_SORT.offset(origin.x(), origin.y()),
                session.paletteSortMode().label(), true, SynthesisScreenTheme.ACCENT_DIM);
        if (session.paletteFiltersOpen()) {
            renderFilterDrawer(graphics, font, origin);
        }
        List<PaletteEntry> pieces = paletteView.entries();
        if (pieces.isEmpty()) {
            graphics.drawString(font, paletteView.emptyMessage(), palette.x() + 7, palette.y() + 28, SynthesisScreenTheme.MUTED, false);
            return;
        }
        int maxScroll = Math.max(0, pieces.size() - PALETTE_VISIBLE);
        session.clampPaletteScroll(maxScroll);
        int paletteScroll = session.paletteScroll();
        int limit = Math.min(PALETTE_VISIBLE, pieces.size() - paletteScroll);
        for (int i = 0; i < limit; i++) {
            int pieceIndex = paletteScroll + i;
            ScreenRect tile = paletteTile(origin, i);
            PaletteEntry entry = pieces.get(pieceIndex);
            Piece piece = entry.piece();
            boolean hovered = tile.contains(mouseX, mouseY);
            graphics.fill(tile.x(), tile.y(), tile.right(), tile.bottom(), hovered ? 0xFF3B332D : 0xFF211D1A);
            int borderColor = samePiece(session.carried().orElse(null), piece) ? SynthesisScreenTheme.ACCENT
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

    private boolean handleBoardModePaletteControlClick(ScreenRect panel, int mouseX, int mouseY) {
        if (boardModeControlRect(panel, 0).contains(mouseX, mouseY)) {
            session.toggleFilterNeedsOnly();
            return true;
        }
        if (boardModeControlRect(panel, 1).contains(mouseX, mouseY)) {
            session.toggleFilterFitsOnly();
            return true;
        }
        if (boardModeControlRect(panel, 2).contains(mouseX, mouseY)) {
            session.toggleFilterFusionOnly();
            return true;
        }
        if (boardModeControlRect(panel, 3).contains(mouseX, mouseY)) {
            session.advancePaletteSortMode();
            return true;
        }
        return false;
    }

    private static ScreenRect boardModeControlRect(ScreenRect panel, int index) {
        int column = index % 2;
        int row = index / 2;
        return new ScreenRect(panel.x() + 6 + column * 55, panel.y() + 18 + row * 12, 51, 10);
    }

    private static void renderBoardModePotencyBadge(GuiGraphics graphics, Font font, ScreenRect tile, PaletteEntry entry) {
        String text = "E" + entry.remainingPotency();
        int width = Math.min(tile.width() - 4, 8 + font.width(text));
        ScreenRect badge = new ScreenRect(tile.x() + 2, tile.y() + 2, width, 9);
        graphics.fill(badge.x(), badge.y(), badge.right(), badge.bottom(), 0xCC080706);
        SynthesisStationText.drawCenteredFit(graphics, font, Component.literal(text), badge, entry.fullyPlaced() ? SynthesisScreenTheme.MUTED : SynthesisScreenTheme.TEXT);
        if (entry.placedCopies() > 0) {
            String placed = "x" + entry.placedCopies();
            int placedWidth = Math.min(tile.width() - 4, 8 + font.width(placed));
            ScreenRect placedBadge = new ScreenRect(tile.right() - placedWidth - 2, tile.bottom() - 11, placedWidth, 9);
            graphics.fill(placedBadge.x(), placedBadge.y(), placedBadge.right(), placedBadge.bottom(), 0xCC1C1208);
            SynthesisStationText.drawCenteredFit(graphics, font, Component.literal(placed), placedBadge, 0xFFFFB454);
        }
    }

    private void renderFilterDrawer(GuiGraphics graphics, Font font, ScreenRect origin) {
        ScreenRect drawer = filterDrawerAbsolute(origin);
        graphics.fill(drawer.x(), drawer.y(), drawer.right(), drawer.bottom(), 0xF0191714);
        SynthesisStationDrawing.frame(graphics, drawer, 0xFF5B4F44);
        SynthesisStationText.drawFit(graphics, font, "Palette Filter", new ScreenRect(drawer.x() + UiMetrics.LABEL_PADDING, drawer.y() + UiMetrics.LABEL_PADDING, drawer.width() - UiMetrics.LABEL_PADDING * 2, UiMetrics.TEXT_HEIGHT), SynthesisScreenTheme.ACCENT);
        SynthesisStationText.drawFit(graphics, font, "Narrow for the current recipe and board.", new ScreenRect(drawer.x() + UiMetrics.LABEL_PADDING, drawer.y() + 18, drawer.width() - UiMetrics.LABEL_PADDING * 2, 16), SynthesisScreenTheme.MUTED);
        renderPaletteChip(graphics, font, drawerControlRect(FILTER_NEEDS, drawer), "Need Now", session.filterNeedsOnly(), SynthesisScreenTheme.GOOD);
        renderPaletteChip(graphics, font, drawerControlRect(FILTER_FUSION, drawer), "Fusion Ready", session.filterFusionOnly(), 0xFFFFB454);
        renderPaletteChip(graphics, font, drawerControlRect(FILTER_FITS, drawer), "Fits Board", session.filterFitsOnly(), 0xFF7FB7FF);
        SynthesisStationText.drawFit(graphics, font, "Shape", new ScreenRect(drawer.x() + 8, drawer.y() + 86, drawer.width() - 16, UiMetrics.TEXT_HEIGHT), SynthesisScreenTheme.TEXT);
        for (int i = 0; i < ShapeFilterMode.VALUES.size(); i++) {
            ShapeFilterMode mode = ShapeFilterMode.VALUES.get(i);
            renderPaletteChip(graphics, font, drawerControlRect(FILTER_SHAPE_RECTS.get(i), drawer),
                    mode.label(), session.shapeFilterMode() == mode, mode.accentColor());
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

    private void renderReadout(
            GuiGraphics graphics,
            Font font,
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents,
            SynthesisBoard board,
            ScreenRect origin
    ) {
        ScreenRect panel = READOUT.offset(origin.x(), origin.y());
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xEE171411);
        SynthesisStationDrawing.frame(graphics, panel, 0xFF4F453C);

        if (plan.isEmpty()) {
            SynthesisStationText.drawFit(graphics, font, "Select recipe", readoutLineRect(panel, READOUT_TOP_PADDING), SynthesisScreenTheme.MUTED);
            return;
        }

        SynthesisPlan current = plan.get();
        SynthesisPlan placedPlan = projection(plan, storageReagents, inventoryReagents).placedPlan().orElse(current);
        SpatialEvaluation spatial = evaluate(board);
        int resonanceRisk = spatial.resonantPlacementIds().size() * 15;
        int occupied = occupiedCellCount();
        int totalCells = board.size() * board.size();
        ReadoutLayout layout = buildReadoutLayout(panel, placedPlan, spatial, board, occupied);
        boolean hasPlaced = !session.placements().isEmpty();

        SynthesisStationText.drawFit(graphics, font, "Needs", layout.needsHeader(), placedPlan.canSynthesize() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.BAD);
        List<RequirementStatus> displayedRequirements = displayedRequirements(placedPlan);
        for (int i = 0; i < displayedRequirements.size(); i++) {
            RequirementStatus status = displayedRequirements.get(i);
            int color = status.satisfied() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.BAD;
            ScreenRect lineRect = layout.requirementLines().get(i);
            if (session.selectedRequirementFilterIndex() == i) {
                SynthesisStationDrawing.frame(graphics, lineRect.inset(-1), SynthesisScreenTheme.ACCENT);
            }
            renderRequirementProgress(graphics, font, status, lineRect, color);
        }
        layout.extraNeedsLine().ifPresent(rect ->
                SynthesisStationText.drawFit(graphics, font, "+" + (placedPlan.requirements().size() - displayedRequirements.size()) + " more needs", rect, SynthesisScreenTheme.MUTED));

        String elementBudget = SynthesisStationText.compactElementBudget(placedPlan.requirements().stream()
                .map(status -> status.requirement().query())
                .toList());
        layout.elementsHeader().ifPresent(rect ->
                SynthesisStationText.drawFit(
                        graphics,
                        font,
                        "Elements",
                        rect,
                        placedPlan.elementBudgetSatisfied() ? SynthesisScreenTheme.ACCENT : SynthesisScreenTheme.BAD));
        layout.elementsLine().ifPresent(rect ->
                SynthesisStationText.drawFit(
                        graphics,
                        font,
                        elementBudget,
                        rect,
                        placedPlan.elementBudgetSatisfied() ? SynthesisScreenTheme.TEXT : SynthesisScreenTheme.BAD));

        int outcomeColor = !placedPlan.canSynthesize() ? SynthesisScreenTheme.MUTED
                : resonanceRisk > 0 ? 0xFFFF8040
                : SynthesisScreenTheme.GOOD;
        SynthesisStationText.drawFit(graphics, font, outcomeSummary(current.preview(), hasPlaced), layout.outcomeSummary(), outcomeColor);
        layout.resonanceLine().ifPresent(rect ->
                SynthesisStationText.drawFit(graphics, font, "Resonance  +" + resonanceRisk + " risk", rect, 0xFFFF8040));

        SynthesisStationText.drawFit(graphics, font, "Fill " + occupied + "/" + totalCells, layout.fillLine(), SynthesisScreenTheme.TEXT);
        SynthesisStationText.drawFit(graphics, font, buildEmptyText(totalCells - occupied, board), layout.emptyLine(), occupied == totalCells ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.MUTED);
        layout.nodeBonusLine().ifPresent(rect ->
                SynthesisStationText.drawFit(graphics, font, buildNodeBonusText(spatial.qualityBonus(), spatial.perfectBonus()), rect, SynthesisScreenTheme.GOOD));
        layout.morphLine().ifPresent(rect ->
                SynthesisStationText.drawFit(graphics, font, buildMorphText(spatial.morphTargets()), rect, 0xFFE2A35D));

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
            LinkedHashSet<String> outputAffixes = new LinkedHashSet<>();
            for (FusionResult fr : spatial.fusionResults()) {
                totalQuality += fr.rule().qualityBonus();
                totalSuccess += fr.rule().successWeightBonus();
                fr.rule().outputAffix().ifPresent(outputAffixes::add);
            }
            MutableComponent fusionsHeader = Component.literal("Fusions").withStyle(s -> s.withColor(SynthesisScreenTheme.ACCENT));
            if (!outputAffixes.isEmpty()) {
                fusionsHeader.append(Component.literal("  →").withStyle(ChatFormatting.DARK_GRAY));
                int ai = 0;
                for (String affix : outputAffixes) {
                    if (ai++ > 0) fusionsHeader.append(Component.literal(" ·").withStyle(ChatFormatting.DARK_GRAY));
                    fusionsHeader.append(Component.literal(" "));
                    fusionsHeader.append(Component.translatable(affix.replace(":", ".affix.")).withStyle(ChatFormatting.AQUA));
                }
            }
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

    private void renderRequirementProgress(GuiGraphics graphics, Font font, RequirementStatus status, ScreenRect rowRect, int color) {
        SynthesisStationText.drawRichFit(graphics, font, requirementLine(status), rowRect, color);
        ScreenRect barRect = new ScreenRect(rowRect.x(), rowRect.y() + READOUT_BAR_LABEL_GAP, rowRect.width(), REQUIREMENT_BAR_HEIGHT);
        double amount = Math.clamp(status.availableAmount() / (double) status.requirement().amount(), 0.0D, 1.0D);
        graphics.fill(barRect.x(), barRect.y(), barRect.right(), barRect.bottom(), SynthesisScreenTheme.PANEL_LIGHT);
        graphics.fill(barRect.x(), barRect.y(), barRect.x() + (int) Math.round(barRect.width() * amount), barRect.bottom(), color);
    }

    private void renderDisplayProgress(GuiGraphics graphics, Font font, SynthesisDisplayModel.Line line, ScreenRect rowRect, int color) {
        SynthesisStationText.drawFit(graphics, font, displayLineText(line), rowRect, color);
        ScreenRect barRect = new ScreenRect(rowRect.x(), rowRect.y() + READOUT_BAR_LABEL_GAP, rowRect.width(), REQUIREMENT_BAR_HEIGHT);
        double amount = line.required() <= 0 ? 0.0D : Math.clamp(line.available() / (double) line.required(), 0.0D, 1.0D);
        graphics.fill(barRect.x(), barRect.y(), barRect.right(), barRect.bottom(), SynthesisScreenTheme.PANEL_LIGHT);
        graphics.fill(barRect.x(), barRect.y(), barRect.x() + (int) Math.round(barRect.width() * amount), barRect.bottom(), color);
    }

    private static String displayLineText(SynthesisDisplayModel.Line line) {
        return line.available() + "/" + line.required() + " " + line.label();
    }

    private static List<String> traitTextLines(SpatialEvaluation spatial) {
        return spatial.expectedTraits().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> SynthesisNoun.label(entry.getKey()) + " +" + entry.getValue())
                .toList();
    }

    private static List<String> resonanceTextLines(SpatialEvaluation spatial) {
        if (!spatial.fusionResults().isEmpty()) {
            return spatial.fusionResults().stream()
                    .map(fusion -> SynthesisNoun.label(fusion.traitA()) + " + " + SynthesisNoun.label(fusion.traitB()))
                    .toList();
        }
        int risk = spatial.resonantPlacementIds().size() * 15;
        if (risk > 0) {
            return List.of("Risk +" + risk);
        }
        return List.of();
    }

    private static String outcomeSummary(OutcomePreview preview, boolean hasPlaced) {
        if (!hasPlaced) {
            return "Outcome: place reagents";
        }
        return "Outcome: " + SynthesisStationText.percent(preview.successProbability())
                + " success | " + SynthesisStationText.percent(preview.failureProbability())
                + " fail | " + SynthesisStationText.percent(preview.probabilityOf(OutcomeClass.PERFECT_SUCCESS))
                + " perfect";
    }

    private static String footerOutcomeText(OutcomePreview preview, boolean hasPlaced) {
        if (!hasPlaced) {
            return "Outcome: place";
        }
        return "Success " + SynthesisStationText.percent(preview.successProbability())
                + "  Perfect " + SynthesisStationText.percent(preview.probabilityOf(OutcomeClass.PERFECT_SUCCESS));
    }

    private void renderCarriedGhost(GuiGraphics graphics, Font font, SynthesisBoard board, ScreenRect origin, int mouseX, int mouseY) {
        if (!session.hasCarried()) {
            return;
        }
        Piece carried = session.carried().orElseThrow();
        int carriedRotation = session.carriedRotation();
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

    private void renderCarriedGhostInRect(GuiGraphics graphics, Font font, SynthesisBoard board, ScreenRect boardRect, int mouseX, int mouseY) {
        if (!session.hasCarried()) {
            return;
        }
        Piece carried = session.carried().orElseThrow();
        int carriedRotation = session.carriedRotation();
        Optional<Cell> hover = boardModeCellAt(boardRect, board, mouseX, mouseY);
        if (hover.isEmpty()) {
            drawFloatingShape(graphics, font, carried, carriedRotation, mouseX + 5, mouseY + 5, true);
            return;
        }
        PlacementPreview preview = placementPreview(board, carried, carriedRotation, hover.get());
        for (Cell cell : carried.rotatedCells(carriedRotation, preview.anchorX(), preview.anchorY())) {
            if (!within(board, cell.x(), cell.y())) {
                continue;
            }
            ScreenRect rect = cellRectInBoard(boardRect, board, cell.x(), cell.y());
            graphics.fill(rect.x() + 3, rect.y() + 3, rect.right() - 3, rect.bottom() - 3, preview.valid() ? 0x887FBF89 : 0x88D37A6A);
        }
        ScreenRect cursor = cellRectInBoard(boardRect, board, hover.get().x(), hover.get().y()).inset(5);
        SynthesisStationDrawing.frame(graphics, cursor, preview.valid() ? SynthesisScreenTheme.ACCENT : 0xFFD37A6A);
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
        return rotateAtCell(board, boardCell.get());
    }

    private boolean rotateAtCell(SynthesisBoard board, Cell boardCell) {
        if (rotateCarried(board, Optional.of(boardCell))) {
            return true;
        }
        Optional<Placement> existing = placementAt(boardCell.x(), boardCell.y());
        if (existing.isEmpty()) {
            return true;
        }
        Placement placement = existing.get();
        int nextRotation = (placement.rotation() + 1) & 3;
        if (!canPlace(board, placement.piece(), nextRotation, placement.anchorX(), placement.anchorY(), placement.id())) {
            return true;
        }
        session.replacePlacement(placement, nextRotation, placement.anchorX(), placement.anchorY());
        return true;
    }

    private boolean handleBoardCellClick(SynthesisBoard board, Cell boardCell, boolean shiftDown) {
        if (session.hasCarried()) {
            Piece carried = session.carried().orElseThrow();
            int carriedRotation = session.carriedRotation();
            if (shiftDown) {
                Optional<PlacementPreview> preview = overwritePreview(board, carried, carriedRotation, boardCell);
                if (preview.isPresent()) {
                    removeOverlappedPlacements(carried, carriedRotation, preview.get());
                    session.addPlacement(carried, carriedRotation, preview.get().anchorX(), preview.get().anchorY());
                    clearCarried();
                }
                return true;
            }
            PlacementPreview preview = placementPreview(board, carried, carriedRotation, boardCell);
            if (preview.valid()) {
                session.addPlacement(carried, carriedRotation, preview.anchorX(), preview.anchorY());
                clearCarried();
            }
            return true;
        }
        Optional<Placement> existing = placementAt(boardCell.x(), boardCell.y());
        if (existing.isPresent()) {
            Placement placement = existing.get();
            session.removePlacement(placement);
            if (shiftDown) {
                clearCarried();
                return true;
            }
            startCarrying(
                    placement.piece(),
                    placement.rotation(),
                    placement.localCellAt(boardCell).orElse(defaultCursorCell(placement.piece(), placement.rotation()))
            );
            return true;
        }
        return true;
    }

    private void startCarrying(Piece piece, int rotation) {
        startCarrying(piece, rotation, defaultCursorCell(piece, rotation));
    }

    private void startCarrying(Piece piece, int rotation, Cell cursorCell) {
        session.startCarrying(piece, rotation, cursorCell);
    }

    private void clearCarried() {
        session.clearCarried();
    }

    private boolean autoPlace(SynthesisBoard board, Piece piece) {
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int y = 0; y < board.size(); y++) {
                for (int x = 0; x < board.size(); x++) {
                    if (canPlace(board, piece, rotation, x, y, -1)) {
                        session.addPlacement(piece, rotation, x, y);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private PlacementPreview placementPreview(SynthesisBoard board, Piece piece, int rotation, Cell hover) {
        Optional<PlacementPreview> preferred = previewForCursorCell(board, piece, rotation, hover, session.carriedCursorCell());
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
        Optional<PlacementPreview> preferred = overwritePreviewForCursorCell(board, piece, rotation, hover, session.carriedCursorCell());
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
        session.removePlacementsIf(placement -> placement.cells().stream().anyMatch(targetCells::contains));
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

        for (Placement placement : session.placements()) {
            for (String trait : placement.piece().inheritableTraits()) {
                expectedTraits.merge(trait, 1, Integer::sum);
            }
        }
        for (Placement first : session.placements()) {
            for (Placement second : session.placements()) {
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
        int perfectBonus = 0;
        List<String> morphTargets = new ArrayList<>();
        for (SynthesisBoard.Node node : board.nodes()) {
            Optional<Placement> cover = placementAt(node.x(), node.y());
            if (cover.isEmpty() || !nodeActive(node, cover.get())) {
                continue;
            }
            qualityBonus += node.qualityBonus();
            perfectBonus += node.perfectBonus();
            node.morphTarget().ifPresent(morphTargets::add);
        }
        SpatialEvaluation result = new SpatialEvaluation(links, expectedTraits, fusedTraits, qualityBonus, perfectBonus, List.copyOf(morphTargets), fusionResults, resonantIds);
        lastEvaluation = result;
        return result;
    }

    ResolvedFusionData currentFusion() {
        if (lastEvaluation == null || lastEvaluation.fusionResults().isEmpty()) {
            return ResolvedFusionData.EMPTY;
        }
        java.util.LinkedHashSet<String> seenIds = new java.util.LinkedHashSet<>();
        java.util.List<TraitFusionRule> rules = new java.util.ArrayList<>();
        for (FusionResult fr : lastEvaluation.fusionResults()) {
            if (seenIds.add(fr.rule().id())) {
                rules.add(fr.rule());
            }
        }
        return ResolvedFusionData.fromRules(rules, lastEvaluation.resonantPlacementIds().size());
    }

    SynthesisBoardFusionPayload buildFusionPayload(int containerId) {
        if (lastEvaluation == null || lastEvaluation.fusionResults().isEmpty()) {
            return new SynthesisBoardFusionPayload(containerId, List.of(), 0, placedReagentsForPayload());
        }
        java.util.LinkedHashSet<String> ruleIds = new java.util.LinkedHashSet<>();
        for (FusionResult fr : lastEvaluation.fusionResults()) {
            ruleIds.add(fr.rule().id());
        }
        return new SynthesisBoardFusionPayload(
                containerId,
                new ArrayList<>(ruleIds),
                lastEvaluation.resonantPlacementIds().size(),
                placedReagentsForPayload()
        );
    }

    SynthesisBoardFusionPayload buildFusionPayload(int containerId, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        List<ReagentStack> payloadReagents = projectedReagents(storageReagents, inventoryReagents);
        if (lastEvaluation == null || lastEvaluation.fusionResults().isEmpty()) {
            return new SynthesisBoardFusionPayload(containerId, List.of(), 0, payloadReagents);
        }
        java.util.LinkedHashSet<String> ruleIds = new java.util.LinkedHashSet<>();
        for (FusionResult fr : lastEvaluation.fusionResults()) {
            ruleIds.add(fr.rule().id());
        }
        return new SynthesisBoardFusionPayload(
                containerId,
                new ArrayList<>(ruleIds),
                lastEvaluation.resonantPlacementIds().size(),
                payloadReagents
        );
    }

    SynthesisBoardFusionPayload buildFusionPayload(
            int containerId,
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents
    ) {
        List<ReagentStack> payloadReagents = projection(plan, storageReagents, inventoryReagents).payloadReagents();
        if (lastEvaluation == null || lastEvaluation.fusionResults().isEmpty()) {
            return new SynthesisBoardFusionPayload(containerId, List.of(), 0, payloadReagents);
        }
        java.util.LinkedHashSet<String> ruleIds = new java.util.LinkedHashSet<>();
        for (FusionResult fr : lastEvaluation.fusionResults()) {
            ruleIds.add(fr.rule().id());
        }
        return new SynthesisBoardFusionPayload(
                containerId,
                new ArrayList<>(ruleIds),
                lastEvaluation.resonantPlacementIds().size(),
                payloadReagents
        );
    }

    void resetAfterSynthesis() {
        session.resetAfterSynthesis();
        lastEvaluation = null;
    }

    boolean canSynthesizePlaced(Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        return projection(plan, storageReagents, inventoryReagents).canSynthesize();
    }

    private List<ReagentStack> placedReagentsForPayload() {
        return session.placements().stream()
                .map(placement -> placement.piece().reagent())
                .toList();
    }

    private Optional<Placement> placementAt(int x, int y) {
        for (Placement placement : session.placements()) {
            if (placement.occupies(x, y)) {
                return Optional.of(placement);
            }
        }
        return Optional.empty();
    }

    private int occupiedCellCount() {
        Set<Cell> occupied = new HashSet<>();
        for (Placement placement : session.placements()) {
            occupied.addAll(placement.cells());
        }
        return occupied.size();
    }

    private ReagentContainer placedReagentContainer() {
        ReagentContainer container = new ReagentContainer();
        for (Placement placement : session.placements()) {
            container.insert(placement.piece().reagent());
        }
        return container;
    }

    private ReagentContainer placedReagentContainer(List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        ArrayList<ReagentStack> available = new ArrayList<>(storageReagents.size() + inventoryReagents.size());
        available.addAll(storageReagents);
        available.addAll(inventoryReagents);

        ReagentContainer bounded = new ReagentContainer();
        ArrayList<ReagentStack> accepted = new ArrayList<>();
        for (Placement placement : session.placements()) {
            ReagentStack reagent = placement.piece().reagent();
            int remainingAvailable = totalAmountForProfile(available, reagent) - totalAmountForProfile(accepted, reagent);
            if (remainingAvailable <= 0) {
                continue;
            }
            ReagentStack capped = reagent.withAmount(Math.min(reagent.amount(), remainingAvailable));
            accepted.add(capped);
            bounded.insert(capped);
        }
        return bounded;
    }

    SynthesisBoardProjection projection(
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents
    ) {
        return SynthesisBoardProjection.fromPlacedReagents(
                plan,
                placedReagentsForProjection(),
                storageReagents,
                inventoryReagents
        );
    }

    private List<ReagentStack> projectedReagents(List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        return SynthesisBoardProjection.payloadReagents(
                placedReagentsForProjection(),
                storageReagents,
                inventoryReagents
        );
    }

    private List<ReagentStack> placedReagentsForProjection() {
        return session.placements().stream()
                .map(placement -> placement.piece().reagent())
                .toList();
    }

    private PaletteView paletteView(Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        List<ReagentStack> reagents = session.paletteSource() == PaletteSource.STORAGE
                ? storageReagents
                : inventoryReagents;
        SynthesisBoardProjection projection = projection(plan, storageReagents, inventoryReagents);
        List<RequirementStatus> unsatisfied = unsatisfiedRequirements(plan, storageReagents, inventoryReagents);
        List<ReagentStack> currentInputs = projection.payloadReagents();
        List<PaletteEntry> entries = reagents.stream()
                .map(Piece::new)
                .filter(this::matchesPaletteFilters)
                .map(piece -> new PaletteEntry(
                        piece,
                        placedAmountFor(piece),
                        placedCopiesFor(piece),
                        matchesNeedNow(piece, plan, unsatisfied, currentInputs),
                        hasFusionPotentialWithPlaced(piece),
                        primaryElementIndex(piece),
                        canFitOnCurrentBoard(piece, plan),
                        piece.shape().size()
                ))
                .filter(entry -> matchesAdvancedFilters(entry, plan, storageReagents, inventoryReagents))
                .sorted(paletteComparator(plan.isPresent()))
                .toList();
        String emptyMessage = reagents.isEmpty() ? "No reagents"
                : entries.isEmpty() ? "No matching reagents"
                : "";
        return new PaletteView(entries, emptyMessage);
    }

    List<String> debugPaletteOrder(Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        return paletteView(plan, storageReagents, inventoryReagents).entries().stream()
                .map(entry -> entry.piece().reagent().reagentId())
                .toList();
    }

    SynthesisState buildState(
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents
    ) {
        if (plan.isEmpty()) {
            return SynthesisState.empty();
        }
        SynthesisBoard board = currentBoard(plan);
        SynthesisBoardProjection projection = projection(plan, storageReagents, inventoryReagents);
        SpatialEvaluation spatial = evaluate(board);
        PaletteView palette = paletteView(plan, storageReagents, inventoryReagents);
        int occupied = occupiedCellCount();
        int totalCells = board.size() * board.size();
        return SynthesisState.fromProjection(
                projection,
                plan.get().profile(),
                new SynthesisState.BoardState(
                        occupied,
                        totalCells,
                        totalCells - occupied,
                        spatial.qualityBonus(),
                        spatial.perfectBonus(),
                        spatial.resonantPlacementIds().size() * 15,
                        projection.successProbability(),
                        projection.perfectProbability(),
                        placedReagentsForState(),
                        spatial.fusionResults().stream()
                                .map(fusion -> fusion.traitA() + "+" + fusion.traitB()
                                        + fusion.rule().outputAffix().map(affix -> "->" + affix).orElse(""))
                                .toList()
                ),
                new SynthesisState.PaletteState(
                        session.paletteSource().name().toLowerCase(Locale.ROOT),
                        session.filterNeedsOnly(),
                        session.filterFusionOnly(),
                        session.filterFitsOnly(),
                        session.shapeFilterMode().name().toLowerCase(Locale.ROOT),
                        palette.entries().stream()
                                .map(this::paletteEntryState)
                                .toList()
                )
        );
    }

    private List<SynthesisState.PlacedReagent> placedReagentsForState() {
        return session.placements().stream()
                .map(placement -> new SynthesisState.PlacedReagent(
                        placement.piece().reagent().reagentId(),
                        placement.piece().reagent().amount(),
                        placement.piece().reagent().tier(),
                        placement.piece().reagent().quality(),
                        placement.piece().reagent().purity(),
                        placement.piece().reagent().instability(),
                        placement.piece().reagent().categories().stream().sorted().toList(),
                        sortedIntMap(placement.piece().reagent().elements()),
                        placement.piece().reagent().traits().stream().sorted().toList(),
                        placement.cells().stream()
                                .map(cell -> cell.x() + "," + cell.y())
                                .sorted()
                                .toList()
                ))
                .toList();
    }

    private SynthesisState.PaletteEntryState paletteEntryState(PaletteEntry entry) {
        ArrayList<String> reasons = new ArrayList<>();
        if (entry.matchesUnsatisfiedRequirement()) {
            reasons.add("need_now");
        }
        if (entry.hasFusionPotential()) {
            reasons.add("fusion_ready");
        }
        if (entry.fitsCurrentBoard()) {
            reasons.add("fits_board");
        }
        return new SynthesisState.PaletteEntryState(
                entry.piece().reagent().reagentId(),
                entry.piece().reagent().amount(),
                entry.placedAmount(),
                entry.remainingAmount(),
                entry.placedCopies(),
                entry.matchesUnsatisfiedRequirement(),
                entry.hasFusionPotential(),
                entry.fitsCurrentBoard(),
                entry.shapeCellCount(),
                reasons
        );
    }

    private static Map<String, Integer> sortedIntMap(Map<String, Integer> source) {
        LinkedHashMap<String, Integer> sorted = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }

    private Optional<Integer> hoveredPaletteIndex(int localX, int localY, List<PaletteEntry> pieces) {
        int paletteScroll = session.paletteScroll();
        int limit = Math.min(PALETTE_VISIBLE, Math.max(0, pieces.size() - paletteScroll));
        for (int i = 0; i < limit; i++) {
            if (paletteTile(new ScreenRect(0, 0, 0, 0), i).contains(localX, localY)) {
                return Optional.of(paletteScroll + i);
            }
        }
        return Optional.empty();
    }

    private Optional<Integer> hoveredBoardModePaletteIndex(ScreenRect panel, int mouseX, int mouseY, List<PaletteEntry> pieces) {
        int visibleCount = boardModePaletteVisibleCount(panel);
        session.clampPaletteScroll(Math.max(0, pieces.size() - visibleCount));
        int paletteScroll = session.paletteScroll();
        int limit = Math.min(visibleCount, pieces.size() - paletteScroll);
        for (int i = 0; i < limit; i++) {
            if (boardModePaletteTile(panel, i).contains(mouseX, mouseY)) {
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

    static BoardModeDebugLayout debugBoardModeLayout(ScreenRect boardArea, SynthesisBoard board) {
        return new BoardModeDebugLayout(SynthesisBoardGeometry.boardRectForArea(boardArea, board));
    }

    static BoardModePaletteDebugLayout debugBoardModePaletteLayout(ScreenRect palettePanel, int itemCount) {
        return new BoardModePaletteDebugLayout(SynthesisBoardGeometry.paletteTiles(palettePanel, itemCount));
    }

    static Optional<DebugCell> debugBoardModeCellAt(ScreenRect boardArea, SynthesisBoard board, int mouseX, int mouseY) {
        return SynthesisBoardGeometry.boardCellAt(boardArea, board, mouseX, mouseY)
                .map(cell -> new DebugCell(cell.x(), cell.y()));
    }

    private static Optional<Cell> boardModeCellAt(ScreenRect boardArea, SynthesisBoard board, int mouseX, int mouseY) {
        return SynthesisBoardGeometry.boardCellAt(boardArea, board, mouseX, mouseY)
                .map(cell -> new Cell(cell.x(), cell.y()));
    }

    private static ScreenRect cellRect(ScreenRect origin, SynthesisBoard board, int x, int y) {
        ScreenRect rect = boardRect(board);
        return new ScreenRect(rect.x() + origin.x() + x * CELL_SIZE, rect.y() + origin.y() + y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
    }

    private static ScreenRect cellRectInBoard(ScreenRect boardRect, SynthesisBoard board, int x, int y) {
        return SynthesisBoardGeometry.cellRectInBoard(boardRect, board, x, y);
    }

    private static ScreenRect placementBoundsInBoard(ScreenRect boardRect, SynthesisBoard board, Placement placement) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Cell cell : placement.cells()) {
            ScreenRect rect = cellRectInBoard(boardRect, board, cell.x(), cell.y());
            minX = Math.min(minX, rect.x());
            minY = Math.min(minY, rect.y());
            maxX = Math.max(maxX, rect.right());
            maxY = Math.max(maxY, rect.bottom());
        }
        return new ScreenRect(minX, minY, maxX - minX, maxY - minY);
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

    private static int boardModePaletteVisibleRows(ScreenRect panel) {
        return SynthesisBoardGeometry.paletteVisibleRows(panel);
    }

    private static int boardModePaletteVisibleCount(ScreenRect panel) {
        return SynthesisBoardGeometry.paletteVisibleCount(panel);
    }

    private static ScreenRect boardModePaletteTile(ScreenRect panel, int index) {
        return SynthesisBoardGeometry.paletteTile(panel, index);
    }

    private static void drawMiniShape(GuiGraphics graphics, Piece piece, int x, int y) {
        int color = pieceColor(piece);
        for (Cell cell : piece.shapeCells()) {
            graphics.fill(x + cell.x() * 4, y + cell.y() * 4, x + cell.x() * 4 + 4, y + cell.y() * 4 + 4, color);
        }
    }

    private static void renderPaletteReasonPips(GuiGraphics graphics, ScreenRect tile, PaletteEntry entry) {
        int x = tile.x() + 24;
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

    private static void renderPaletteShapeBadge(GuiGraphics graphics, ScreenRect row, Piece piece) {
        ScreenRect badge = new ScreenRect(row.right() - 13, row.y() + 3, 9, 9);
        graphics.fill(badge.x(), badge.y(), badge.right(), badge.bottom(), 0xFF2B241E);
        SynthesisStationDrawing.frame(graphics, badge, 0xFF6E5E50);
        int cell = 2;
        int startX = badge.x() + 2;
        int startY = badge.y() + 2;
        for (Cell shapeCell : piece.shapeCells()) {
            graphics.fill(
                    startX + shapeCell.x() * cell,
                    startY + shapeCell.y() * cell,
                    startX + shapeCell.x() * cell + cell,
                    startY + shapeCell.y() * cell + cell,
                    SynthesisScreenTheme.TEXT
            );
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

    private Optional<List<Component>> boardModeTooltip(
            Optional<SynthesisPlan> plan,
            List<ReagentStack> storageReagents,
            List<ReagentStack> inventoryReagents,
            ScreenRect origin,
            ScreenRect boardArea,
            ScreenRect palettePanel,
            int mouseX,
            int mouseY
    ) {
        ScreenRect absolutePalette = palettePanel.offset(origin.x(), origin.y());
        PaletteView palette = paletteView(plan, storageReagents, inventoryReagents);
        Optional<Integer> paletteIndex = hoveredBoardModePaletteIndex(absolutePalette, mouseX, mouseY, palette.entries());
        if (paletteIndex.isPresent()) {
            PaletteEntry entry = palette.entries().get(paletteIndex.get());
            return Optional.of(synthesisTooltip(entry.piece(), Optional.of(entry), plan, false));
        }
        Optional<Cell> boardCell = boardModeCellAt(boardArea.offset(origin.x(), origin.y()), currentBoard(plan), mouseX, mouseY);
        if (boardCell.isEmpty()) {
            return Optional.empty();
        }
        return placementAt(boardCell.get().x(), boardCell.get().y())
                .map(placement -> synthesisTooltip(placement.piece(), Optional.empty(), plan, true));
    }

    private static List<Component> pieceTooltip(PaletteEntry entry) {
        Piece piece = entry.piece();
        ArrayList<Component> lines = new ArrayList<>();
        lines.add(Component.literal(piece.label()));
        lines.add(Component.literal("Shape: " + piece.shape().id()));
        lines.add(Component.literal("Tier " + piece.reagent().tier() + "  Quality " + piece.reagent().quality()));
        if (entry.placedPotency() > 0) {
            lines.add(Component.literal("Potency: " + entry.remainingPotency() + "/" + entry.totalPotency()
                    + " essence available"));
            lines.add(Component.literal("Placed: " + entry.placedCopies() + " reagent"));
        } else {
            lines.add(Component.literal("Potency: " + entry.totalPotency() + " essence"));
        }
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

    private static List<Component> synthesisTooltip(Piece piece, Optional<PaletteEntry> entry, Optional<SynthesisPlan> plan, boolean placed) {
        ArrayList<Component> lines = new ArrayList<>(pieceTooltip(entry.orElseGet(() -> new PaletteEntry(
                piece,
                0,
                0,
                false,
                false,
                primaryElementIndex(piece),
                true,
                piece.shape().size()
        ))));
        lines.add(1, Component.literal(placed ? "Placed in synthesis board" : "Available for synthesis"));
        contributionTooltipLine(piece.reagent(), plan).ifPresent(lines::add);
        return lines;
    }

    private static Optional<Component> contributionTooltipLine(ReagentStack reagent, Optional<SynthesisPlan> plan) {
        if (plan.isEmpty()) {
            return Optional.empty();
        }
        List<String> contributions = plan.get().profile().requirements().stream()
                .map(SynthesisRequirement::query)
                .filter(query -> SynthesisRequirementMatcher.reagentQuery(query).matches(reagent))
                .map(query -> "Contributes " + reagent.amount() + " " + summarizeRequirement(query).getString() + " essence")
                .distinct()
                .toList();
        if (contributions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Component.literal(String.join(", ", contributions)));
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
        if (!query.reagentIds().isEmpty()) {
            return SynthesisNoun.component(query.reagentIds().stream().sorted().findFirst().orElse("reagent"));
        }
        if (!query.requiredCategories().isEmpty()) {
            return SynthesisNoun.component(query.requiredCategories().stream().sorted().findFirst().orElse("category"));
        }
        if (!query.requiredTraits().isEmpty()) {
            return SynthesisNoun.component(query.requiredTraits().stream().sorted().findFirst().orElse("trait"));
        }
        return Component.literal("Any");
    }

    private static boolean samePiece(Piece first, Piece second) {
        return first != null && second != null && first.reagent().reagentId().equals(second.reagent().reagentId());
    }

    private static int totalAmountForProfile(List<ReagentStack> reagents, ReagentStack profile) {
        return reagents.stream()
                .filter(reagent -> sameReagentProfile(reagent, profile))
                .mapToInt(ReagentStack::amount)
                .sum();
    }

    private static boolean sameReagentProfile(ReagentStack left, ReagentStack right) {
        return left.reagentId().equals(right.reagentId())
                && left.categories().equals(right.categories())
                && left.tier() == right.tier()
                && left.quality() == right.quality()
                && left.purity() == right.purity()
                && left.instability() == right.instability()
                && left.elements().equals(right.elements())
                && left.traits().equals(right.traits())
                && left.shape().equals(right.shape())
                && left.sourceHints().equals(right.sourceHints());
    }

    private boolean matchesPaletteFilters(Piece piece) {
        for (String element : session.paletteElementFilters()) {
            if (!piece.hasElement(element)) {
                return false;
            }
        }
        if (session.shapeFilterMode() != ShapeFilterMode.ANY && !session.shapeFilterMode().matches(piece.shape())) {
            return false;
        }
        return true;
    }

    private boolean matchesAdvancedFilters(PaletteEntry entry, Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        if (session.filterNeedsOnly() && !entry.matchesUnsatisfiedRequirement()) {
            return false;
        }
        if (session.filterFusionOnly() && !entry.hasFusionPotential()) {
            return false;
        }
        if (session.filterFitsOnly() && !entry.fitsCurrentBoard()) {
            return false;
        }
        Optional<RequirementStatus> selectedRequirement = selectedRequirementFilter(plan, storageReagents, inventoryReagents);
        if (selectedRequirement.isPresent()
                && !SynthesisRequirementMatcher.reagentQuery(selectedRequirement.get().requirement().query())
                .matches(entry.piece().reagent())) {
            return false;
        }
        return true;
    }

    private Optional<RequirementStatus> selectedRequirementFilter(Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        List<RequirementStatus> displayed = displayedRequirements(plan, storageReagents, inventoryReagents);
        int selectedRequirementFilterIndex = session.selectedRequirementFilterIndex();
        if (selectedRequirementFilterIndex < 0 || selectedRequirementFilterIndex >= displayed.size()) {
            return Optional.empty();
        }
        return Optional.of(displayed.get(selectedRequirementFilterIndex));
    }

    private Optional<Integer> clickedRequirementFilterIndex(Optional<SynthesisPlan> plan, SynthesisBoard board, int localX, int localY, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        List<ScreenRect> rects = requirementFilterRects(plan, board, storageReagents, inventoryReagents);
        for (int i = 0; i < rects.size(); i++) {
            RequirementStatus status = displayedRequirements(plan, storageReagents, inventoryReagents).get(i);
            if (!status.satisfied() && rects.get(i).contains(localX, localY)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private List<RequirementStatus> displayedRequirements(Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        if (plan.isEmpty()) {
            return List.of();
        }
        return displayedRequirements(projection(plan, storageReagents, inventoryReagents).placedPlan().orElse(plan.get()));
    }

    private List<ScreenRect> requirementFilterRects(Optional<SynthesisPlan> plan, SynthesisBoard board, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        if (plan.isEmpty()) {
            return List.of();
        }
        SynthesisPlan placedPlan = projection(plan, storageReagents, inventoryReagents).placedPlan().orElse(plan.get());
        return buildReadoutLayout(READOUT, placedPlan, evaluate(board), board, occupiedCellCount()).requirementLines();
    }

    private static List<RequirementStatus> displayedRequirements(SynthesisPlan placedPlan) {
        return placedPlan.requirements().stream().limit(5).toList();
    }

    static ReadoutDebugLayout debugReadoutLayout(ScreenRect panel, SynthesisPlan placedPlan, SynthesisBoard board) {
        ReadoutLayout layout = buildReadoutLayout(
                panel,
                placedPlan,
                new SpatialEvaluation(List.of(), Map.of(), Map.of(), 0, 0, List.of(), List.of(), Set.of()),
                board,
                0
        );
        return new ReadoutDebugLayout(
                layout.requirementLines().isEmpty() ? -1 : layout.requirementLines().getFirst().y(),
                layout.outcomeSummary().y()
        );
    }

    static BoardModeProgressDebugLayout debugBoardModeProgressLayout(ScreenRect panel, SynthesisDisplayModel model) {
        BoardModeProgressLayout layout = buildBoardModeProgressLayout(panel, model);
        return new BoardModeProgressDebugLayout(
                layout.essencesHeader().y(),
                layout.essenceLines().isEmpty() ? -1 : layout.essenceLines().getFirst().y(),
                layout.elementsHeader().map(ScreenRect::y).orElse(-1),
                layout.elementLines().isEmpty() ? -1 : layout.elementLines().getFirst().y(),
                layout.divider().y(),
                layout.traitsHeader().y(),
                layout.resonanceHeader().y()
        );
    }

    private static BoardModeProgressLayout buildBoardModeProgressLayout(ScreenRect panel, SynthesisDisplayModel model) {
        int footerTop = panel.bottom() - BOARD_MODE_PROGRESS_FOOTER_HEIGHT;
        int y = panel.y() + 22;
        ScreenRect essencesHeader = new ScreenRect(panel.x() + 6, y, panel.width() - 12, READOUT_TEXT_HEIGHT);
        y += 12;
        ArrayList<ScreenRect> essenceLines = new ArrayList<>();
        int visibleEssences = Math.min(4, model.essences().size());
        for (int i = 0; i < visibleEssences && y + 13 <= footerTop - 62; i++) {
            essenceLines.add(new ScreenRect(panel.x() + 6, y, panel.width() - 12, 13));
            y += 16;
        }

        y += 2;
        Optional<ScreenRect> elementsHeader = Optional.empty();
        ArrayList<ScreenRect> elementLines = new ArrayList<>();
        if (!model.elements().isEmpty() && y + 25 <= footerTop - 38) {
            elementsHeader = Optional.of(new ScreenRect(panel.x() + 6, y, panel.width() - 12, READOUT_TEXT_HEIGHT));
            y += 12;
            int visibleElements = Math.min(3, model.elements().size());
            for (int i = 0; i < visibleElements && y + 13 <= footerTop - 30; i++) {
                elementLines.add(new ScreenRect(panel.x() + 6, y, panel.width() - 12, 13));
                y += 16;
            }
        }

        y += 4;
        ScreenRect divider = new ScreenRect(panel.x() + 6, y, panel.width() - 12, 1);
        y += 8;
        ScreenRect traitsHeader = new ScreenRect(panel.x() + 6, y, panel.width() - 12, READOUT_TEXT_HEIGHT);
        y += 11;
        ArrayList<ScreenRect> traitLines = new ArrayList<>();
        int visibleTraits = Math.min(2, model.traits().size());
        for (int i = 0; i < visibleTraits && y + READOUT_TEXT_HEIGHT <= footerTop - 16; i++) {
            traitLines.add(new ScreenRect(panel.x() + 6, y, panel.width() - 12, READOUT_TEXT_HEIGHT));
            y += 10;
        }
        ScreenRect resonanceHeader = new ScreenRect(panel.x() + 6, y, panel.width() - 12, READOUT_TEXT_HEIGHT);
        y += 11;
        ArrayList<ScreenRect> resonanceLines = new ArrayList<>();
        int visibleResonance = Math.min(1, model.resonance().size());
        for (int i = 0; i < visibleResonance && y + READOUT_TEXT_HEIGHT <= footerTop - 4; i++) {
            resonanceLines.add(new ScreenRect(panel.x() + 6, y, panel.width() - 12, READOUT_TEXT_HEIGHT));
            y += 10;
        }
        return new BoardModeProgressLayout(
                essencesHeader,
                List.copyOf(essenceLines),
                elementsHeader,
                List.copyOf(elementLines),
                divider,
                traitsHeader,
                List.copyOf(traitLines),
                resonanceHeader,
                List.copyOf(resonanceLines),
                footerTop
        );
    }

    private static ScreenRect readoutLineRect(ScreenRect panel, int y) {
        return new ScreenRect(panel.x() + READOUT_INSET_X, y, panel.width() - READOUT_INSET_X * 2, READOUT_TEXT_HEIGHT);
    }

    private static ReadoutLayout buildReadoutLayout(ScreenRect panel, SynthesisPlan placedPlan, SpatialEvaluation spatial, SynthesisBoard board, int occupied) {
        int y = panel.y() + READOUT_TOP_PADDING;
        ScreenRect needsHeader = readoutLineRect(panel, y);
        y += READOUT_SECTION_GAP;
        List<RequirementStatus> displayedRequirements = displayedRequirements(placedPlan);
        ArrayList<ScreenRect> requirementLines = new ArrayList<>(displayedRequirements.size());
        for (int i = 0; i < displayedRequirements.size(); i++) {
            requirementLines.add(readoutLineRect(panel, y));
            y += REQUIREMENT_ROW_HEIGHT;
        }

        Optional<ScreenRect> extraNeedsLine = Optional.empty();
        if (placedPlan.requirements().size() > displayedRequirements.size()) {
            extraNeedsLine = Optional.of(readoutLineRect(panel, y));
            y += READOUT_ROW_GAP;
        }

        y += READOUT_SECTION_PAD;
        Optional<ScreenRect> elementsHeader = Optional.empty();
        Optional<ScreenRect> elementsLine = Optional.empty();
        String elementBudget = SynthesisStationText.compactElementBudget(placedPlan.requirements().stream()
                .map(status -> status.requirement().query())
                .toList());
        if (!"None".equals(elementBudget) && y + READOUT_TEXT_HEIGHT <= panel.bottom()) {
            int labelX = panel.x() + READOUT_INSET_X;
            int valueX = labelX + 57;
            elementsHeader = Optional.of(new ScreenRect(labelX, y, 54, READOUT_TEXT_HEIGHT));
            elementsLine = Optional.of(new ScreenRect(valueX, y, panel.right() - READOUT_INSET_X - valueX, READOUT_TEXT_HEIGHT));
            y += READOUT_ROW_GAP + READOUT_SECTION_PAD;
        }

        ScreenRect outcomeSummary = readoutLineRect(panel, y);
        y += READOUT_ROW_GAP;

        Optional<ScreenRect> resonanceLine = Optional.empty();
        int resonanceRisk = spatial.resonantPlacementIds().size() * 15;
        if (resonanceRisk > 0) {
            resonanceLine = Optional.of(readoutLineRect(panel, y));
            y += READOUT_ROW_GAP;
        }

        ScreenRect fillLine = new ScreenRect(panel.x() + READOUT_INSET_X, y, READOUT_FILL_VALUE_WIDTH, READOUT_TEXT_HEIGHT);
        ScreenRect emptyLine = new ScreenRect(panel.x() + READOUT_FILL_SECONDARY_X, y, READOUT_FILL_VALUE_WIDTH, READOUT_TEXT_HEIGHT);
        y += 12;

        Optional<ScreenRect> nodeBonusLine = Optional.empty();
        if (spatial.qualityBonus() > 0 || spatial.perfectBonus() > 0) {
            nodeBonusLine = Optional.of(readoutLineRect(panel, y));
            y += READOUT_ROW_GAP;
        }

        Optional<ScreenRect> morphLine = Optional.empty();
        if (!spatial.morphTargets().isEmpty()) {
            morphLine = Optional.of(readoutLineRect(panel, y));
            y += READOUT_ROW_GAP;
        }

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
                fillLine,
                emptyLine,
                nodeBonusLine,
                morphLine,
                needsHeader,
                requirementLines,
                extraNeedsLine,
                elementsHeader,
                elementsLine,
                outcomeSummary,
                resonanceLine,
                traitsHeader,
                emptyTraitsLine,
                traitLines,
                fusionsHeader,
                fusionLines
        );
    }

    private List<RequirementStatus> unsatisfiedRequirements(Optional<SynthesisPlan> plan, List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        if (plan.isEmpty()) {
            return List.of();
        }
        return projection(plan, storageReagents, inventoryReagents).placedPlan().orElse(plan.get()).requirements().stream()
                .filter(status -> !status.satisfied())
                .toList();
    }

    private boolean matchesNeedNow(
            Piece piece,
            Optional<SynthesisPlan> plan,
            List<RequirementStatus> unsatisfied,
            List<ReagentStack> currentInputs
    ) {
        if (matchesAnyUnsatisfiedRequirement(piece, unsatisfied)) {
            return true;
        }
        return plan.isPresent() && SynthesisRequirementMatcher.contributesMissingElement(
                piece.reagent(),
                plan.get().profile().requirements(),
                currentInputs
        );
    }

    private boolean matchesAnyUnsatisfiedRequirement(Piece piece, List<RequirementStatus> unsatisfied) {
        for (RequirementStatus status : unsatisfied) {
            if (SynthesisRequirementMatcher.reagentQuery(status.requirement().query()).matches(piece.reagent())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFusionPotentialWithPlaced(Piece piece) {
        for (Placement placement : session.placements()) {
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

    private int placedAmountFor(Piece piece) {
        return session.placements().stream()
                .map(placement -> placement.piece().reagent())
                .filter(reagent -> sameReagentProfile(reagent, piece.reagent()))
                .mapToInt(ReagentStack::amount)
                .sum();
    }

    private int placedCopiesFor(Piece piece) {
        return (int) session.placements().stream()
                .map(placement -> placement.piece().reagent())
                .filter(reagent -> sameReagentProfile(reagent, piece.reagent()))
                .count();
    }

    private Comparator<PaletteEntry> paletteComparator(boolean hasPlan) {
        Comparator<PaletteEntry> tierThenId = Comparator
                .comparingInt((PaletteEntry entry) -> entry.piece().reagent().tier()).reversed()
                .thenComparingInt((PaletteEntry entry) -> entry.piece().reagent().quality()).reversed()
                .thenComparingInt((PaletteEntry entry) -> entry.fitsCurrentBoard() ? 0 : 1)
                .thenComparingInt((PaletteEntry entry) -> entry.shapeCellCount())
                .thenComparing(entry -> entry.piece().reagent().reagentId());
        return switch (session.paletteSortMode()) {
            case TIER -> tierThenId;
            case ELEMENT -> Comparator
                    .comparingInt(PaletteEntry::primaryElementIndex)
                    .thenComparing(tierThenId);
            case RELEVANCE -> hasPlan
                    ? Comparator
                    .comparingInt(PaletteEntry::usefulnessScore)
                    .reversed()
                    .thenComparing(entry -> entry.piece().label())
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
        return session.hasAdvancedFilters();
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

    private static ScreenRect boardRectForArea(ScreenRect area, SynthesisBoard board) {
        return SynthesisBoardGeometry.boardRectForArea(area, board);
    }

    private static boolean within(SynthesisBoard board, int x, int y) {
        return x >= 0 && x < board.size() && y >= 0 && y < board.size();
    }

    private void syncSelectedBoard(Optional<SynthesisPlan> plan) {
        session.syncSelectedPlan(plan);
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

    private record PaletteEntry(
            Piece piece,
            int placedAmount,
            int placedCopies,
            boolean matchesUnsatisfiedRequirement,
            boolean hasFusionPotential,
            int primaryElementIndex,
            boolean fitsCurrentBoard,
            int shapeCellCount
    ) {
        int remainingAmount() {
            return Math.max(0, piece.reagent().amount() - placedAmount);
        }

        int totalPotency() {
            return piece.reagent().amount();
        }

        int placedPotency() {
            return placedAmount;
        }

        int remainingPotency() {
            return remainingAmount();
        }

        boolean fullyPlaced() {
            return remainingAmount() <= 0;
        }

        int usefulnessScore() {
            int score = 0;
            if (matchesUnsatisfiedRequirement) {
                score += 1000;
            }
            if (primaryElementIndex >= 0 && primaryElementIndex < CHIP_ELEMENTS.size()) {
                score += 500;
            }
            if (fitsCurrentBoard) {
                score += 100;
            }
            if (hasFusionPotential) {
                score += 50;
            }
            score += piece.reagent().quality();
            score += piece.reagent().tier() * 5;
            score += piece.reagent().purity() / 5;
            return score;
        }
    }

    private record PaletteView(List<PaletteEntry> entries, String emptyMessage) {
    }

    private record TraitLink(Cell from, Cell to) {
    }

    private record FusionResult(Cell from, Cell to, String traitA, String traitB, TraitFusionRule rule) {
    }

    private record SpatialEvaluation(List<TraitLink> links, Map<String, Integer> expectedTraits, Map<String, Integer> fusedTraits, int qualityBonus, int perfectBonus, List<String> morphTargets, List<FusionResult> fusionResults, Set<Integer> resonantPlacementIds) {
    }

    private record ReadoutLayout(
            ScreenRect fillLine,
            ScreenRect emptyLine,
            Optional<ScreenRect> nodeBonusLine,
            Optional<ScreenRect> morphLine,
            ScreenRect needsHeader,
            List<ScreenRect> requirementLines,
            Optional<ScreenRect> extraNeedsLine,
            Optional<ScreenRect> elementsHeader,
            Optional<ScreenRect> elementsLine,
            ScreenRect outcomeSummary,
            Optional<ScreenRect> resonanceLine,
            ScreenRect traitsHeader,
            Optional<ScreenRect> emptyTraitsLine,
            List<ScreenRect> traitLines,
            Optional<ScreenRect> fusionsHeader,
            List<ScreenRect> fusionLines
    ) {
    }

    record ReadoutDebugLayout(int firstRequirementY, int outcomeSummaryY) {
    }

    private record BoardModeProgressLayout(
            ScreenRect essencesHeader,
            List<ScreenRect> essenceLines,
            Optional<ScreenRect> elementsHeader,
            List<ScreenRect> elementLines,
            ScreenRect divider,
            ScreenRect traitsHeader,
            List<ScreenRect> traitLines,
            ScreenRect resonanceHeader,
            List<ScreenRect> resonanceLines,
            int footerTop
    ) {
    }

    record BoardModeProgressDebugLayout(
            int essencesHeaderY,
            int firstEssenceLineY,
            int elementsHeaderY,
            int firstElementLineY,
            int dividerY,
            int traitsHeaderY,
            int resonanceHeaderY
    ) {
    }

    record BoardModeDebugLayout(ScreenRect boardRect) {
    }

    record BoardModePaletteDebugLayout(List<ScreenRect> tiles) {
    }

    record DebugCell(int x, int y) {
    }

    private static String buildEmptyText(int empty, SynthesisBoard board) {
        if (empty <= 0 || (board.emptyCellSuccessPenalty() == 0 && board.emptyCellPerfectPenalty() == 0)) {
            return "Empty " + empty;
        }
        StringBuilder sb = new StringBuilder("Empty ").append(empty);
        if (board.emptyCellSuccessPenalty() > 0) {
            sb.append("  S-").append(empty * board.emptyCellSuccessPenalty());
        }
        if (board.emptyCellPerfectPenalty() > 0) {
            sb.append("  P-").append(empty * board.emptyCellPerfectPenalty());
        }
        return sb.toString();
    }

    private static String buildNodeBonusText(int qualityBonus, int perfectBonus) {
        if (qualityBonus > 0 && perfectBonus > 0) {
            return "Node  +" + qualityBonus + " quality  +" + perfectBonus + " perfect";
        }
        if (qualityBonus > 0) {
            return "Node  +" + qualityBonus + " quality";
        }
        return "Node  +" + perfectBonus + " perfect";
    }

    private static String buildMorphText(List<String> morphTargets) {
        return "Morph → " + morphTargets.stream()
                .map(SynthesisStationText::shortLabel)
                .collect(java.util.stream.Collectors.joining(", "));
    }

}
