package com.sanhiruzu.atelier.ui.client;

record CodexMetrics(
        int cellSize,
        int cellGap,
        int buttonSize,
        int rootMargin,
        int rootMaxWidth,
        int rootMaxHeight,
        int sectionGap,
        int gridInset,
        int scrollbarInset
) {
    static final CodexMetrics DEFAULT = new CodexMetrics(
            24,
            UiMetrics.CONTROL_GAP,
            24,
            24,
            420,
            236,
            10,
            8,
            5
    );
}
