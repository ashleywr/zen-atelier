package com.sanhiruzu.atelier.space;

public enum ClassificationState {
    SOLID(0),
    OUTSIDE(1),
    INSIDE(2),
    PARTIAL(3);

    private final int bits;

    ClassificationState(int bits) {
        this.bits = bits;
    }

    public int getBits() {
        return bits;
    }

    public static ClassificationState fromBits(int bits) {
        return switch (bits) {
            case 0 -> SOLID;
            case 1 -> OUTSIDE;
            case 2 -> INSIDE;
            case 3 -> PARTIAL;
            default -> throw new IllegalArgumentException("Invalid state bits: " + bits);
        };
    }
}
