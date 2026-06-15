package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.world.ReagentDumpLogic.Click;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReagentDumpLogicTest {
    @Test
    void doubleClickWithinWindowSamePos() {
        Click previous = new Click(new BlockPos(1, 2, 3), 100L);
        assertThat(ReagentDumpLogic.isDoubleClick(previous, new BlockPos(1, 2, 3), 105L)).isTrue();
    }

    @Test
    void doubleClickAtExactWindowBoundaryIsInclusive() {
        Click previous = new Click(new BlockPos(1, 2, 3), 100L);
        assertThat(ReagentDumpLogic.isDoubleClick(previous, new BlockPos(1, 2, 3), 110L)).isTrue();
    }

    @Test
    void notDoubleClickPastWindow() {
        Click previous = new Click(new BlockPos(1, 2, 3), 100L);
        assertThat(ReagentDumpLogic.isDoubleClick(previous, new BlockPos(1, 2, 3), 111L)).isFalse();
    }

    @Test
    void notDoubleClickDifferentPos() {
        Click previous = new Click(new BlockPos(1, 2, 3), 100L);
        assertThat(ReagentDumpLogic.isDoubleClick(previous, new BlockPos(9, 9, 9), 101L)).isFalse();
    }

    @Test
    void notDoubleClickWhenNoPreviousRecord() {
        assertThat(ReagentDumpLogic.isDoubleClick(null, new BlockPos(1, 2, 3), 100L)).isFalse();
    }

    @Test
    void summarizeEmptyIsZero() {
        ReagentDumpLogic.DumpSummary summary = ReagentDumpLogic.summarize(List.of());
        assertThat(summary.stacks()).isZero();
        assertThat(summary.units()).isZero();
    }

    @Test
    void summarizeCountsStacksAndUnits() {
        ReagentDumpLogic.DumpSummary summary = ReagentDumpLogic.summarize(
                List.of(reagent(40), reagent(60), reagent(5)));
        assertThat(summary.stacks()).isEqualTo(3);
        assertThat(summary.units()).isEqualTo(105);
    }

    private static ReagentStack reagent(int amount) {
        return new ReagentStack(
                "zen_atelier:fire_reagent",
                amount,
                1,
                0,
                0,
                0,
                Map.of(),
                List.of(),
                Set.of());
    }
}
