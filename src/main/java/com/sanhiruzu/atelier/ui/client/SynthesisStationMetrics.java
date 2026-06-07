package com.sanhiruzu.atelier.ui.client;

record SynthesisStationMetrics(
        int width,
        int height,
        int recipeRows,
        int categoryTabWidth,
        int categoryTabHeight,
        int categoryTabStep,
        int recipeCellHeight,
        int slotSize
) {
    static final SynthesisStationMetrics DEFAULT = new SynthesisStationMetrics(
            480,
            326,
            6,
            50,
            18,
            53,
            21,
            18
    );
}
