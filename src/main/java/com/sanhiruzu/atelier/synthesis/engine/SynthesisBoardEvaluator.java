package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.data.TraitFusionRegistry;
import com.sanhiruzu.atelier.synthesis.data.TraitFusionRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SynthesisBoardEvaluator {
    private static final int[][] NEIGHBOR_DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

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
        List<SynthesisBoardEvaluation.ActiveFusion> activeFusions = detectFusions(occupied);
        Set<String> resonantIds = resonantPlacementIds(activeFusions);

        return new SynthesisBoardEvaluation(
                board,
                placements,
                errors.isEmpty(),
                errors,
                occupied,
                emptyCells,
                activatedNodes,
                activeFusions,
                resonantIds
        );
    }

    private static List<SynthesisBoardEvaluation.ActiveFusion> detectFusions(
            Map<SynthesisBoardPlacement.Cell, SynthesisBoardPlacement> occupied
    ) {
        List<SynthesisBoardEvaluation.ActiveFusion> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Map.Entry<SynthesisBoardPlacement.Cell, SynthesisBoardPlacement> entry : occupied.entrySet()) {
            SynthesisBoardPlacement.Cell cell = entry.getKey();
            SynthesisBoardPlacement placement = entry.getValue();

            for (int[] dir : NEIGHBOR_DIRS) {
                SynthesisBoardPlacement.Cell neighbor = new SynthesisBoardPlacement.Cell(
                        cell.x() + dir[0], cell.y() + dir[1]);
                SynthesisBoardPlacement neighborPlacement = occupied.get(neighbor);
                if (neighborPlacement == null || neighborPlacement == placement) {
                    continue;
                }

                for (String traitA : placement.reagent().traits()) {
                    for (String traitB : neighborPlacement.reagent().traits()) {
                        Optional<TraitFusionRule> rule = TraitFusionRegistry.find(traitA, traitB);
                        if (rule.isEmpty()) {
                            continue;
                        }
                        // Deduplicate: use sorted placement IDs + rule ID so each fusion is only counted once
                        String idMin = placement.id().compareTo(neighborPlacement.id()) <= 0
                                ? placement.id() : neighborPlacement.id();
                        String idMax = idMin.equals(placement.id()) ? neighborPlacement.id() : placement.id();
                        String key = idMin + "|" + idMax + "|" + rule.get().id();
                        if (seen.add(key)) {
                            result.add(new SynthesisBoardEvaluation.ActiveFusion(
                                    placement, neighborPlacement, traitA, traitB, rule.get()));
                        }
                    }
                }
            }
        }
        return result;
    }

    private static Set<String> resonantPlacementIds(List<SynthesisBoardEvaluation.ActiveFusion> fusions) {
        Map<String, Integer> fusionCount = new HashMap<>();
        for (SynthesisBoardEvaluation.ActiveFusion fusion : fusions) {
            fusionCount.merge(fusion.placementA().id(), 1, Integer::sum);
            fusionCount.merge(fusion.placementB().id(), 1, Integer::sum);
        }
        Set<String> resonant = new HashSet<>();
        for (Map.Entry<String, Integer> entry : fusionCount.entrySet()) {
            if (entry.getValue() >= 2) {
                resonant.add(entry.getKey());
            }
        }
        return resonant;
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
