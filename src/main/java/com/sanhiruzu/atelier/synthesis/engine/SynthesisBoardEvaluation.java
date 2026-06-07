package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.data.TraitFusionRule;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record SynthesisBoardEvaluation(
        SynthesisBoard board,
        List<SynthesisBoardPlacement> placements,
        boolean valid,
        List<String> errors,
        Map<SynthesisBoardPlacement.Cell, SynthesisBoardPlacement> occupiedCells,
        int emptyCells,
        List<ActivatedNode> activatedNodes,
        List<ActiveFusion> activeFusions,
        Set<String> resonantPlacementIds
) {
    public SynthesisBoardEvaluation {
        placements = List.copyOf(placements);
        errors = List.copyOf(errors);
        occupiedCells = Map.copyOf(occupiedCells);
        activatedNodes = List.copyOf(activatedNodes);
        activeFusions = List.copyOf(activeFusions);
        resonantPlacementIds = Set.copyOf(resonantPlacementIds);
    }

    public int occupiedCellCount() {
        return occupiedCells.size();
    }

    public int emptyCellSuccessPenalty() {
        return emptyCells * board.emptyCellSuccessPenalty();
    }

    public int emptyCellPerfectPenalty() {
        return emptyCells * board.emptyCellPerfectPenalty();
    }

    public record ActivatedNode(
            SynthesisBoard.Node node,
            SynthesisBoardPlacement placement,
            Optional<String> activatedElement
    ) {
        public ActivatedNode {
            activatedElement = activatedElement == null ? Optional.empty() : activatedElement;
        }
    }

    public record ActiveFusion(
            SynthesisBoardPlacement placementA,
            SynthesisBoardPlacement placementB,
            String traitA,
            String traitB,
            TraitFusionRule rule
    ) {
    }
}
