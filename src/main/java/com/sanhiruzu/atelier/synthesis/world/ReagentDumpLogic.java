package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import net.minecraft.core.BlockPos;

import java.util.List;

/** Pure decision helpers for the reagent cabinet's storage-drawer dump-all gesture. */
public final class ReagentDumpLogic {
    private ReagentDumpLogic() {
    }

    /** A second click within this many game ticks of the first counts as a double-click (~0.5s). */
    public static final long WINDOW_TICKS = 10L;

    /** The most recent click a player made on a cabinet. */
    public record Click(BlockPos pos, long tick) {
    }

    /** Result of a dump: how many reagent stacks and total units were deposited. */
    public record DumpSummary(int stacks, int units) {
    }

    public static boolean isDoubleClick(Click previous, BlockPos pos, long currentTick) {
        return previous != null
                && previous.pos().equals(pos)
                && currentTick - previous.tick() <= WINDOW_TICKS;
    }

    public static DumpSummary summarize(List<ReagentStack> reagents) {
        int units = 0;
        for (ReagentStack reagent : reagents) {
            units += reagent.amount();
        }
        return new DumpSummary(reagents.size(), units);
    }
}
