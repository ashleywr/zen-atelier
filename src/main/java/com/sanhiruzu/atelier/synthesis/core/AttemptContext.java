package com.sanhiruzu.atelier.synthesis.core;

public record AttemptContext(
        ApparatusState apparatus,
        RoomAlchemyContext room,
        int configTierCap,
        int risk
) {
    public AttemptContext {
        if (apparatus == null) {
            throw new IllegalArgumentException("apparatus must not be null");
        }
        if (room == null) {
            throw new IllegalArgumentException("room must not be null");
        }
        configTierCap = Math.clamp(configTierCap, 1, 6);
        risk = Math.clamp(risk + room.riskBias() - apparatus.stabilityBonus(), 0, 100);
    }

    public int apparatusTierCap() {
        return apparatus.tierCap();
    }

    public int roomTierCap() {
        return room.tierCap();
    }
}
