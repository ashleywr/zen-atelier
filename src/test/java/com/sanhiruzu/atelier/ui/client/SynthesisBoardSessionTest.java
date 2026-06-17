package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisBoardSessionTest {
    @Test
    void shiftRemoveClearsPlacementWithoutCarryingIt() {
        SynthesisBoardSession session = new SynthesisBoardSession();
        ReagentStack reagent = ReagentStack.simple("zen_atelier:test_reagent", 10, 1);

        int placementId = session.addDebugPlacement(reagent, 1, 1);

        assertThat(session.removePlacementAt(1, 1, true)).isTrue();
        assertThat(session.placements()).isEmpty();
        assertThat(session.carried()).isEmpty();
        assertThat(session.removedPlacementIds()).containsExactly(placementId);
    }

    @Test
    void normalRemovePicksPlacementUpForMoving() {
        SynthesisBoardSession session = new SynthesisBoardSession();
        ReagentStack reagent = new ReagentStack(
                "zen_atelier:organic_reagent",
                Set.of("zen_atelier:organic"),
                31,
                1,
                20,
                20,
                0,
                Map.of("life", 1),
                List.of("zen_atelier:organic"),
                ReagentShape.SINGLE,
                Set.of()
        );

        session.addDebugPlacement(reagent, 0, 0);

        assertThat(session.removePlacementAt(0, 0, false)).isTrue();
        assertThat(session.placements()).isEmpty();
        assertThat(session.carried()).isPresent();
        assertThat(session.carried().get().reagent()).isEqualTo(reagent);
    }
}
