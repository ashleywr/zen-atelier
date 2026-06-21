package com.sanhiruzu.atelier.environment;

import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class EnvironmentFacetSignals {
    private static final List<EnvironmentFacetProvider> PROVIDERS = new ArrayList<>();
    private static EnvironmentFacetMappings mappings = EnvironmentFacetMappings.empty();

    private EnvironmentFacetSignals() {
    }

    public static synchronized void registerProvider(EnvironmentFacetProvider provider) {
        PROVIDERS.add(Objects.requireNonNull(provider, "provider must not be null"));
    }

    public static synchronized void replaceMappings(EnvironmentFacetMappings replacement) {
        mappings = Objects.requireNonNull(replacement, "replacement must not be null");
    }

    public static synchronized Set<String> signalsFor(BlockState state) {
        Objects.requireNonNull(state, "state must not be null");
        if (PROVIDERS.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> facets = new LinkedHashSet<>();
        for (EnvironmentFacetProvider provider : PROVIDERS) {
            Set<String> provided = provider.facetsFor(state);
            if (provided != null) {
                facets.addAll(provided);
            }
        }
        return mappings.signalsFor(facets);
    }

    static synchronized void clearProvidersForTest() {
        PROVIDERS.clear();
    }
}
