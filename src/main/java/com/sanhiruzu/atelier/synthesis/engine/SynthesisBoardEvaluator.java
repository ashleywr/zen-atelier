package com.sanhiruzu.atelier.synthesis.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SynthesisBoardEvaluator {
    public SynthesisBoardEvaluation evaluate(SynthesisBoard board, List<SynthesisBoardPlacement> placements) {
        Map<SynthesisBoardPlacement.Cell, SynthesisBoardPlacement> occupied = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        for (SynthesisBoardPlacement placement : placements) {
            for (SynthesisBoardPlacement.Cell cell : placement.cells()) {
                if (!inside(board, cell)) {
                    errors.add("placement " + placement.id() + " is outside the board at " + cell.x() + "," + cell.y());
                    continue;
                }
                SynthesisBoardPlacement existing = occupied.putIfAbsent(cell, placement);
                if (existing != null) {
                    errors.add("placement " + placement.id() + " overlaps " + existing.id() + " at " + cell.x() + "," + cell.y());
                }
            }
        }

        int totalCells = board.size() * board.size();
        int emptyCells = Math.max(0, totalCells - occupied.size());
        List<SynthesisBoardEvaluation.ActivatedNode> activatedNodes = errors.isEmpty()
                ? activatedNodes(board, occupied)
                : List.of();

        return new SynthesisBoardEvaluation(
                board,
                placements,
                errors.isEmpty(),
                errors,
                occupied,
                emptyCells,
                activatedNodes
        );
    }

    private static List<SynthesisBoardEvaluation.ActivatedNode> activatedNodes(
            SynthesisBoard board,
            Map<SynthesisBoardPlacement.Cell, SynthesisBoardPlacement> occupied
    ) {
        List<SynthesisBoardEvaluation.ActivatedNode> activated = new ArrayList<>();
        for (SynthesisBoard.Node node : board.nodes()) {
            SynthesisBoardPlacement.Cell nodeCell = new SynthesisBoardPlacement.Cell(node.x(), node.y());
            SynthesisBoardPlacement placement = occupied.get(nodeCell);
            if (placement == null) {
                continue;
            }
            if (node.requiredElement().isEmpty()) {
                activated.add(new SynthesisBoardEvaluation.ActivatedNode(node, placement, Optional.empty()));
                continue;
            }
            String element = node.requiredElement().get();
            if (placement.reagent().elements().getOrDefault(element, 0) >= node.requiredElementValue()) {
                activated.add(new SynthesisBoardEvaluation.ActivatedNode(node, placement, Optional.of(element)));
            }
        }
        return List.copyOf(activated);
    }

    private static boolean inside(SynthesisBoard board, SynthesisBoardPlacement.Cell cell) {
        return cell.x() >= 0 && cell.x() < board.size() && cell.y() >= 0 && cell.y() < board.size();
    }
}
