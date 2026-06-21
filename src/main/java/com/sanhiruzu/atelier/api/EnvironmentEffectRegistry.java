package com.sanhiruzu.atelier.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class EnvironmentEffectRegistry {
    private static final EnvironmentEffectRegistry GLOBAL = new EnvironmentEffectRegistry();

    private final List<EnvironmentEffectRule> rules = new ArrayList<>();

    public static EnvironmentEffectRegistry global() {
        return GLOBAL;
    }

    public synchronized void register(Consumer<EnvironmentEffectRegistrar> registration) {
        if (registration == null) {
            throw new IllegalArgumentException("registration must not be null");
        }
        EnvironmentEffectRegistrar registrar = new EnvironmentEffectRegistrar();
        registration.accept(registrar);
        rules.addAll(registrar.rules());
    }

    public synchronized List<EnvironmentEffect> evaluate(EnvironmentEffectContext context, EnvironmentSnapshot snapshot) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        List<EnvironmentEffect> effects = new ArrayList<>();
        for (EnvironmentEffectRule rule : rules) {
            effects.addAll(rule.evaluate(context, snapshot));
        }
        return List.copyOf(effects);
    }

    public synchronized void clear() {
        rules.clear();
    }
}
