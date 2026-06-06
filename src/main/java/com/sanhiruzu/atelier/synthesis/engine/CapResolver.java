package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.RollTrace;

import java.util.List;

public final class CapResolver {
    private CapResolver() {
    }

    public static int resolve(CapContext context, RollTrace.Builder trace) {
        int cap = List.of(
                        context.sourceCap(),
                        context.reagentCap(),
                        context.apparatusCap(),
                        context.roomCap(),
                        context.recipeCap(),
                        context.configCap()
                ).stream()
                .mapToInt(Integer::intValue)
                .min()
                .orElse(CapContext.UNBOUNDED);
        int resolved = cap == CapContext.UNBOUNDED ? 6 : Math.clamp(cap, 1, 6);
        trace.add("tier cap resolved to " + resolved);
        return resolved;
    }
}
