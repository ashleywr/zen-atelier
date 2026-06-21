package com.sanhiruzu.atelier.api;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public record EnvironmentEffectRule(
        Predicate<EnvironmentEffectContext> target,
        EnvironmentCondition condition,
        BiFunction<EnvironmentEffectContext, EnvironmentSnapshot, List<EnvironmentEffect>> effectFactory
) {
    public EnvironmentEffectRule {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("condition must not be null");
        }
        if (effectFactory == null) {
            throw new IllegalArgumentException("effectFactory must not be null");
        }
    }

    public List<EnvironmentEffect> evaluate(EnvironmentEffectContext context, EnvironmentSnapshot snapshot) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        if (!target.test(context) || !condition.matches(snapshot)) {
            return List.of();
        }
        List<EnvironmentEffect> effects = effectFactory.apply(context, snapshot);
        return effects == null ? List.of() : List.copyOf(effects);
    }
}
