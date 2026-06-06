package com.sanhiruzu.atelier.synthesis.engine;

import java.util.List;
import java.util.Optional;

public record SynthesisBoard(
        int size,
        int emptyCellSuccessPenalty,
        int emptyCellPerfectPenalty,
        List<Node> nodes
) {
    public static final SynthesisBoard CRUDE_3X3 = new SynthesisBoard(3, 0, 0, List.of());

    public SynthesisBoard {
        if (size < 3 || size > 7) {
            throw new IllegalArgumentException("board size must be between 3 and 7");
        }
        emptyCellSuccessPenalty = Math.max(0, emptyCellSuccessPenalty);
        emptyCellPerfectPenalty = Math.max(0, emptyCellPerfectPenalty);
        nodes = List.copyOf(nodes);
        for (Node node : nodes) {
            if (node.x() < 0 || node.x() >= size || node.y() < 0 || node.y() >= size) {
                throw new IllegalArgumentException("board node must be inside board bounds");
            }
        }
    }

    public record Node(
            int x,
            int y,
            String type,
            Optional<String> requiredElement,
            int requiredElementValue,
            Optional<String> morphTarget,
            int qualityBonus,
            int perfectBonus
    ) {
        public Node {
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("node type must not be blank");
            }
            requiredElement = requiredElement == null ? Optional.empty() : requiredElement;
            morphTarget = morphTarget == null ? Optional.empty() : morphTarget;
            requiredElementValue = Math.max(0, requiredElementValue);
            qualityBonus = Math.max(0, qualityBonus);
            perfectBonus = Math.max(0, perfectBonus);
        }
    }
}
