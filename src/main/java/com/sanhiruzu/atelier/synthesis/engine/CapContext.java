package com.sanhiruzu.atelier.synthesis.engine;

public record CapContext(
        int sourceCap,
        int reagentCap,
        int apparatusCap,
        int roomCap,
        int recipeCap,
        int configCap
) {
    public static final int UNBOUNDED = Integer.MAX_VALUE;

    public CapContext {
        sourceCap = normalize(sourceCap);
        reagentCap = normalize(reagentCap);
        apparatusCap = normalize(apparatusCap);
        roomCap = normalize(roomCap);
        recipeCap = normalize(recipeCap);
        configCap = normalize(configCap);
    }

    private static int normalize(int value) {
        return value <= 0 ? UNBOUNDED : value;
    }
}
