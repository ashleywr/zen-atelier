package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.engine.RequirementStatus;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisBoard;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlanner;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class SynthesisSpatialPrototype {
    private static final int MAX_BOARD_SIZE = 5;
    private static final int CELL_SIZE = 18;
    private static final int LEFT_BUTTON = 0;
    private static final int RIGHT_BUTTON = 1;
    private static final ScreenRect BOARD_AREA = new ScreenRect(164, 66, MAX_BOARD_SIZE * CELL_SIZE, MAX_BOARD_SIZE * CELL_SIZE);
    private static final ScreenRect READOUT = new ScreenRect(278, 58, 180, 153);
    private static final ScreenRect PALETTE = new ScreenRect(8, 226, 464, 93);
    private static final ScreenRect PALETTE_STORAGE_TAB = new ScreenRect(18, 233, 62, 12);
    private static final ScreenRect PALETTE_INVENTORY_TAB = new ScreenRect(82, 233, 70, 12);
    private static final int PALETTE_COLUMNS = 11;
    private static final int PALETTE_ROWS = 3;
    private static final int PALETTE_VISIBLE = PALETTE_COLUMNS * PALETTE_ROWS;
    private static final int PALETTE_TILE_WIDTH = 39;
    private static final int PALETTE_TILE_HEIGHT = 22;

    private final List<Placement> placements = new ArrayList<>();
    private PaletteSource paletteSource = PaletteSource.STORAGE;
    private int paletteScroll;
    private Piece carried;
    private int carriedRotation;
    private Cell carriedCursorCell = new Cell(0, 0);
    private int nextPlacementId = 1;
    private String activeProfileId = "";

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
                renderPalette(graphics, font, palettePieces(storageReagents, inventoryReagents), origin, mouseX, mouseY)
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
        Optional<Integer> paletteIndex = hoveredPaletteIndex(localX, localY, palettePieces(storageReagents, inventoryReagents));
        if (paletteIndex.isPresent()) {
            Piece selected = palettePieces(storageReagents, inventoryReagents).get(paletteIndex.get());
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
        List<Piece> pieces = palettePieces(storageReagents, inventoryReagents);
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
        Optional<Integer> paletteIndex = hoveredPaletteIndex(localX, localY, palettePieces(storageReagents, inventoryReagents));
        if (paletteIndex.isPresent()) {
            Piece piece = palettePieces(storageReagents, inventoryReagents).get(paletteIndex.get());
            graphics.renderComponentTooltip(font, pieceTooltip(piece), mouseX, mouseY);
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
            graphics.renderComponentTooltip(font, pieceTooltip(placement.get().piece()), mouseX, mouseY);
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
        for (Placement placement : placements) {
            for (Cell cell : placement.cells()) {
                ScreenRect rect = cellRect(origin, board, cell.x(), cell.y());
                int color = pieceColor(placement.piece());
                graphics.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2, color);
                SynthesisStationDrawing.frame(graphics, rect.inset(2), 0xFF11100E);
            }
            Cell anchor = placement.cells().getFirst();
            ScreenRect rect = cellRect(origin, board, anchor.x(), anchor.y());
            ItemStack stack = ReagentItem.createStack(placement.piece().reagent());
            graphics.renderFakeItem(stack, rect.x() + 1, rect.y() + 1);
            graphics.drawString(font, Integer.toString(placement.piece().shape().size()), rect.x() + 11, rect.y() + 9, SynthesisScreenTheme.TEXT, false);
        }
    }

    private void renderPalette(GuiGraphics graphics, Font font, List<Piece> pieces, ScreenRect origin, int mouseX, int mouseY) {
        ScreenRect palette = PALETTE.offset(origin.x(), origin.y());
        graphics.fill(palette.x(), palette.y(), palette.right(), palette.bottom(), 0xFF171411);
        SynthesisStationDrawing.frame(graphics, palette, 0xFF4F453C);
        renderPaletteTab(graphics, font, PALETTE_STORAGE_TAB.offset(origin.x(), origin.y()), "Storage", paletteSource == PaletteSource.STORAGE);
        renderPaletteTab(graphics, font, PALETTE_INVENTORY_TAB.offset(origin.x(), origin.y()), "Inventory", paletteSource == PaletteSource.INVENTORY);
        if (pieces.isEmpty()) {
            graphics.drawString(font, "No reagents", palette.x() + 7, palette.y() + 28, SynthesisScreenTheme.MUTED, false);
            return;
        }
        int maxScroll = Math.max(0, pieces.size() - PALETTE_VISIBLE);
        paletteScroll = Math.clamp(paletteScroll, 0, maxScroll);
        int limit = Math.min(PALETTE_VISIBLE, pieces.size() - paletteScroll);
        for (int i = 0; i < limit; i++) {
            int pieceIndex = paletteScroll + i;
            ScreenRect tile = paletteTile(origin, i);
            Piece piece = pieces.get(pieceIndex);
            boolean hovered = tile.contains(mouseX, mouseY);
            graphics.fill(tile.x(), tile.y(), tile.right(), tile.bottom(), hovered ? 0xFF3B332D : 0xFF211D1A);
            SynthesisStationDrawing.frame(graphics, tile, samePiece(carried, piece) ? SynthesisScreenTheme.ACCENT : 0xFF4A4037);
            graphics.renderFakeItem(ReagentItem.createStack(piece.reagent()), tile.x() + 2, tile.y() + 2);
            drawMiniShape(graphics, piece, tile.x() + 22, tile.y() + 4);
            graphics.drawString(font, Integer.toString(piece.shape().size()), tile.right() - 8, tile.bottom() - 9, SynthesisScreenTheme.TEXT, false);
        }
        if (pieces.size() > PALETTE_VISIBLE) {
            String range = (paletteScroll + 1) + "-" + (paletteScroll + limit) + "/" + pieces.size();
            graphics.drawString(font, range, palette.right() - font.width(range) - 5, palette.y() + 6, SynthesisScreenTheme.MUTED, false);
        }
    }

    private void renderPaletteTab(GuiGraphics graphics, Font font, ScreenRect tab, String label, boolean active) {
        graphics.fill(tab.x(), tab.y(), tab.right(), tab.bottom(), active ? 0xFF3B332D : 0xFF211D1A);
        SynthesisStationDrawing.frame(graphics, tab, active ? SynthesisScreenTheme.ACCENT : 0xFF4A4037);
        SynthesisStationText.drawCenteredFit(graphics, font, Component.literal(label), tab.inset(2), active ? SynthesisScreenTheme.TEXT : SynthesisScreenTheme.MUTED);
    }

    private void renderReadout(GuiGraphics graphics, Font font, Optional<SynthesisPlan> plan, SynthesisBoard board, ScreenRect origin) {
        ScreenRect panel = READOUT.offset(origin.x(), origin.y());
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xEE171411);
        SynthesisStationDrawing.frame(graphics, panel, 0xFF4F453C);

        int y = panel.y() + 5;
        if (plan.isEmpty()) {
            SynthesisStationText.drawFit(graphics, font, "Select recipe", new ScreenRect(panel.x() + 5, y, panel.width() - 10, 8), SynthesisScreenTheme.MUTED);
            return;
        }

        SynthesisPlan current = plan.get();
        SynthesisPlan placedPlan = new SynthesisPlanner().plan(current.profile(), placedReagentContainer(), 0);
        SpatialEvaluation spatial = evaluate(board);
        renderReadoutBar(graphics, font, "Success", current.preview().successProbability(), panel.x() + 5, y, panel.width() - 10, placedPlan.canSynthesize() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.MUTED);
        y += 17;
        renderReadoutBar(graphics, font, "Perfect", current.preview().probabilityOf(OutcomeClass.PERFECT_SUCCESS), panel.x() + 5, y, panel.width() - 10, SynthesisScreenTheme.ACCENT);
        y += 17;

        int occupied = occupiedCellCount();
        int totalCells = board.size() * board.size();
        SynthesisStationText.drawFit(graphics, font, "Fill " + occupied + "/" + totalCells, new ScreenRect(panel.x() + 5, y, 70, 8), SynthesisScreenTheme.TEXT);
        SynthesisStationText.drawFit(graphics, font, "Empty " + (totalCells - occupied), new ScreenRect(panel.x() + 80, y, 70, 8), occupied == totalCells ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.MUTED);
        y += 12;

        SynthesisStationText.drawFit(graphics, font, "Needs", new ScreenRect(panel.x() + 5, y, panel.width() - 10, 8), placedPlan.canSynthesize() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.BAD);
        y += 10;
        int requirementLimit = Math.min(5, placedPlan.requirements().size());
        for (RequirementStatus status : placedPlan.requirements().stream().limit(requirementLimit).toList()) {
            int color = status.satisfied() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.BAD;
            Component text = requirementLine(status);
            SynthesisStationText.drawRichFit(graphics, font, text, new ScreenRect(panel.x() + 5, y, panel.width() - 10, 8), color);
            y += 9;
        }
        if (placedPlan.requirements().size() > requirementLimit) {
            SynthesisStationText.drawFit(graphics, font, "+" + (placedPlan.requirements().size() - requirementLimit) + " more needs", new ScreenRect(panel.x() + 5, y, panel.width() - 10, 8), SynthesisScreenTheme.MUTED);
            y += 9;
        }

        y += 2;
        SynthesisStationText.drawFit(graphics, font, "Traits", new ScreenRect(panel.x() + 5, y, panel.width() - 10, 8), spatial.expectedTraits().isEmpty() ? SynthesisScreenTheme.MUTED : SynthesisScreenTheme.GOOD);
        y += 10;
        if (spatial.expectedTraits().isEmpty()) {
            SynthesisStationText.drawFit(graphics, font, "None placed", new ScreenRect(panel.x() + 5, y, panel.width() - 10, 8), SynthesisScreenTheme.MUTED);
            return;
        }
        for (Map.Entry<String, Integer> trait : spatial.expectedTraits().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(4)
                .toList()) {
            Component text = SynthesisNoun.line(SynthesisNoun.component(trait.getKey()), " +", trait.getValue());
            SynthesisStationText.drawRichFit(graphics, font, text, new ScreenRect(panel.x() + 5, y, panel.width() - 10, 8), SynthesisScreenTheme.TEXT);
            y += 9;
        }
    }

    private void renderReadoutBar(GuiGraphics graphics, Font font, String label, double amount, int x, int y, int width, int color) {
        SynthesisStationText.drawFit(graphics, font, label + " " + SynthesisStationText.percent(amount), new ScreenRect(x, y, width, 8), color);
        ScreenRect bar = new ScreenRect(x, y + 9, width, 5);
        graphics.fill(bar.x(), bar.y(), bar.right(), bar.bottom(), SynthesisScreenTheme.PANEL_LIGHT);
        graphics.fill(bar.x(), bar.y(), bar.x() + (int) Math.round(bar.width() * amount), bar.bottom(), color);
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
                        Set<String> common = new HashSet<>(first.piece().fusionTraits());
                        common.retainAll(second.piece().fusionTraits());
                        if (common.isEmpty()) {
                            continue;
                        }
                        links.add(new TraitLink(a, b));
                        for (String trait : common) {
                            expectedTraits.merge(trait, 1, Integer::sum);
                            fusedTraits.merge(trait, 1, Integer::sum);
                        }
                    }
                }
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
        return new SpatialEvaluation(links, expectedTraits, fusedTraits, qualityBonus, morphed);
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

    private List<Piece> palettePieces(List<ReagentStack> storageReagents, List<ReagentStack> inventoryReagents) {
        List<ReagentStack> reagents = paletteSource == PaletteSource.STORAGE
                ? storageReagents.isEmpty() ? PrototypeReagentStock.eraOneStorageStock() : storageReagents
                : inventoryReagents;
        return reagents.stream()
                .sorted(Comparator.comparing(ReagentStack::tier).reversed().thenComparing(ReagentStack::reagentId))
                .map(Piece::new)
                .toList();
    }

    private Optional<Integer> hoveredPaletteIndex(int localX, int localY, List<Piece> pieces) {
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

    private static List<Component> pieceTooltip(Piece piece) {
        ArrayList<Component> lines = new ArrayList<>();
        lines.add(Component.literal(piece.label()));
        lines.add(Component.literal("Shape: " + piece.shape().id()));
        lines.add(Component.literal("Tier " + piece.reagent().tier() + "  Quality " + piece.reagent().quality()));
        lines.add(elementTooltipLine(piece.reagent()));
        lines.add(nounListLine("Fusion: ", piece.fusionTraits()));
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

    private record TraitLink(Cell from, Cell to) {
    }

    private record SpatialEvaluation(List<TraitLink> links, Map<String, Integer> expectedTraits, Map<String, Integer> fusedTraits, int qualityBonus, boolean morphed) {
    }

}
