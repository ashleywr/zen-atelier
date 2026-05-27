package com.sanhiruzu.atelier.synthesis;

public enum AlchemyWandTier {
    COPPER(0, 8),
    SILVER(1, 16),
    GOLD(2, 24);

    private final int level;
    private final int qualityBonus;

    AlchemyWandTier(int level, int qualityBonus) {
        this.level = level;
        this.qualityBonus = qualityBonus;
    }

    public int level() {
        return level;
    }

    public int qualityBonus() {
        return qualityBonus;
    }

    public boolean atLeast(AlchemyWandTier other) {
        return level >= other.level;
    }
}
