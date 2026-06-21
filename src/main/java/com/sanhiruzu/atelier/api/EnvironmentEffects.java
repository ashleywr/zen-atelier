package com.sanhiruzu.atelier.api;

public final class EnvironmentEffects {
    private EnvironmentEffects() {
    }

    public static EnvironmentEffect modifySpeed(String id, float amount) {
        return new EnvironmentEffect(EnvironmentEffectType.SPEED_MODIFIER, id, amount);
    }

    public static EnvironmentEffect modifyYield(String id, float amount) {
        return new EnvironmentEffect(EnvironmentEffectType.YIELD_MODIFIER, id, amount);
    }

    public static EnvironmentEffect modifyRisk(String id, float amount) {
        return new EnvironmentEffect(EnvironmentEffectType.RISK_MODIFIER, id, amount);
    }

    public static EnvironmentEffect modifyStability(String id, float amount) {
        return new EnvironmentEffect(EnvironmentEffectType.STABILITY_MODIFIER, id, amount);
    }

    public static EnvironmentEffect modifyComfort(String id, float amount) {
        return new EnvironmentEffect(EnvironmentEffectType.COMFORT_MODIFIER, id, amount);
    }

    public static EnvironmentEffect veto(String id) {
        return new EnvironmentEffect(EnvironmentEffectType.VETO, id, 0f);
    }

    public static EnvironmentEffect label(String id) {
        return new EnvironmentEffect(EnvironmentEffectType.LABEL, id, 0f);
    }
}
