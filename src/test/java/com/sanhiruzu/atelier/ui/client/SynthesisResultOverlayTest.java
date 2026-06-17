package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisResultOverlayTest {
    @Test
    void failedOutcomesUseSmokeImpactDelay() {
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.RECOVERABLE_FAILURE))
                .isEqualTo(SynthesisResultOverlay.FAILURE_IMPACT_TICKS);
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.MESSY_FAILURE))
                .isEqualTo(SynthesisResultOverlay.FAILURE_IMPACT_TICKS);
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.DUD))
                .isEqualTo(SynthesisResultOverlay.FAILURE_IMPACT_TICKS);
    }

    @Test
    void successfulOutcomesShowResultImmediately() {
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.SUCCESS)).isZero();
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.PERFECT_SUCCESS)).isZero();
        assertThat(SynthesisResultOverlay.impactTicksFor(OutcomeClass.UNSTABLE_SUCCESS)).isZero();
    }

    @Test
    void resultPanelStaysInsideStationBounds() {
        SynthesisResultOverlay.Layout layout = SynthesisResultOverlay.layoutFor(
                OutcomeClass.PERFECT_SUCCESS,
                List.of(
                        new SynthesisOutput("minecraft:diamond", 2, 3, 100, List.of("zen_atelier:brilliant")),
                        new SynthesisOutput("minecraft:emerald", 1, 2, 86, List.of())
                ),
                List.of(ReagentStack.simple("zen_atelier:residue", 12, 1)),
                new ScreenRect(20, 30, 0, 0)
        );

        ScreenRect station = new ScreenRect(20, 30, SynthesisStationMetrics.DEFAULT.width(), SynthesisStationMetrics.DEFAULT.height());

        assertThat(layout.panel().x()).isGreaterThanOrEqualTo(station.x() + 24);
        assertThat(layout.panel().right()).isLessThanOrEqualTo(station.right() - 24);
        assertThat(layout.panel().bottom()).isLessThanOrEqualTo(station.bottom() - 24);
        assertThat(layout.outputRows()).hasSize(2);
        assertThat(layout.byproductRows()).hasSize(1);
        assertThat(layout.byproductHeader()).isPresent();
    }

    @Test
    void resultRowsStayInsidePanelContent() {
        SynthesisResultOverlay.Layout layout = SynthesisResultOverlay.layoutFor(
                OutcomeClass.SUCCESS,
                List.of(new SynthesisOutput("minecraft:gold_ingot", 4, 2, 73, List.of())),
                List.of(),
                new ScreenRect(0, 0, 0, 0)
        );

        ScreenRect panel = layout.panel();
        for (ScreenRect row : layout.outputRows()) {
            assertThat(row.x()).isGreaterThan(panel.x());
            assertThat(row.right()).isLessThan(panel.right());
            assertThat(row.bottom()).isLessThan(panel.bottom());
        }
        assertThat(layout.byproductHeader()).isEmpty();
        assertThat(layout.byproductRows()).isEmpty();
    }
}
