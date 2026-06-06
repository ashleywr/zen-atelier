package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisBoardEvaluatorTest {
    private final SynthesisBoardEvaluator evaluator = new SynthesisBoardEvaluator();

    @Test
    void evaluatesOccupiedEmptyAndActivatedNodes() {
        SynthesisBoard board = new SynthesisBoard(
                3,
                1,
                2,
                List.of(new SynthesisBoard.Node(1, 0, "element", Optional.of("fire"), 2, Optional.empty(), 5, 0))
        );
        ReagentStack reagent = reagent("zen_atelier:ember_line", ReagentShape.LINE_TWO, Map.of("fire", 2));

        SynthesisBoardEvaluation evaluation = evaluator.evaluate(board, List.of(
                new SynthesisBoardPlacement("ember", reagent, 0, 0, 0)
        ));

        assertThat(evaluation.valid()).isTrue();
        assertThat(evaluation.occupiedCellCount()).isEqualTo(2);
        assertThat(evaluation.emptyCells()).isEqualTo(7);
        assertThat(evaluation.emptyCellSuccessPenalty()).isEqualTo(7);
        assertThat(evaluation.emptyCellPerfectPenalty()).isEqualTo(14);
        assertThat(evaluation.activatedNodes()).singleElement()
                .extracting(node -> node.placement().id())
                .isEqualTo("ember");
    }

    @Test
    void blocksOverlappingPlacements() {
        ReagentStack first = reagent("zen_atelier:first", ReagentShape.SQUARE_TWO, Map.of());
        ReagentStack second = reagent("zen_atelier:second", ReagentShape.LINE_TWO, Map.of());

        SynthesisBoardEvaluation evaluation = evaluator.evaluate(SynthesisBoard.CRUDE_3X3, List.of(
                new SynthesisBoardPlacement("first", first, 0, 0, 0),
                new SynthesisBoardPlacement("second", second, 1, 0, 0)
        ));

        assertThat(evaluation.valid()).isFalse();
        assertThat(evaluation.errors()).singleElement().asString().contains("overlaps");
        assertThat(evaluation.activatedNodes()).isEmpty();
    }

    @Test
    void blocksOutOfBoundsPlacements() {
        ReagentStack reagent = reagent("zen_atelier:long", ReagentShape.LINE_THREE, Map.of());

        SynthesisBoardEvaluation evaluation = evaluator.evaluate(SynthesisBoard.CRUDE_3X3, List.of(
                new SynthesisBoardPlacement("long", reagent, 1, 2, 1)
        ));

        assertThat(evaluation.valid()).isFalse();
        assertThat(evaluation.errors()).isNotEmpty();
        assertThat(evaluation.errors().getFirst()).contains("outside");
    }

    @Test
    void doesNotActivateNodeWhenElementValueIsTooLow() {
        SynthesisBoard board = new SynthesisBoard(
                3,
                0,
                0,
                List.of(new SynthesisBoard.Node(0, 0, "element", Optional.of("fire"), 3, Optional.empty(), 5, 0))
        );
        ReagentStack reagent = reagent("zen_atelier:weak_ember", ReagentShape.SINGLE, Map.of("fire", 2));

        SynthesisBoardEvaluation evaluation = evaluator.evaluate(board, List.of(
                new SynthesisBoardPlacement("weak", reagent, 0, 0, 0)
        ));

        assertThat(evaluation.valid()).isTrue();
        assertThat(evaluation.activatedNodes()).isEmpty();
    }

    private static ReagentStack reagent(String id, ReagentShape shape, Map<String, Integer> elements) {
        return new ReagentStack(
                id,
                Set.of("zen_atelier:test"),
                1,
                1,
                50,
                50,
                0,
                elements,
                List.of(),
                shape,
                Set.of()
        );
    }
}
