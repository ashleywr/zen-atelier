package com.sanhiruzu.atelier.environment;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record EnvironmentFacetMappings(Map<String, Set<String>> facetsToSignals) {
    public EnvironmentFacetMappings {
        facetsToSignals = sanitize(facetsToSignals);
    }

    public static EnvironmentFacetMappings empty() {
        return new EnvironmentFacetMappings(Map.of());
    }

    public static EnvironmentFacetMappings of(Map<String, Set<String>> facetsToSignals) {
        return new EnvironmentFacetMappings(facetsToSignals);
    }

    public Set<String> signalsFor(Set<String> facets) {
        if (facets == null || facets.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        for (String facet : facets) {
            requireName(facet, "facet");
            signals.addAll(facetsToSignals.getOrDefault(facet, Set.of()));
        }
        return Set.copyOf(signals);
    }

    public EnvironmentFacetMappings mergedWith(EnvironmentFacetMappings other) {
        if (other == null || other.facetsToSignals().isEmpty()) {
            return this;
        }
        LinkedHashMap<String, Set<String>> merged = new LinkedHashMap<>(facetsToSignals);
        other.facetsToSignals().forEach((facet, signals) -> {
            LinkedHashSet<String> combined = new LinkedHashSet<>(merged.getOrDefault(facet, Set.of()));
            combined.addAll(signals);
            merged.put(facet, Set.copyOf(combined));
        });
        return new EnvironmentFacetMappings(merged);
    }

    private static Map<String, Set<String>> sanitize(Map<String, Set<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Set<String>> sanitized = new LinkedHashMap<>();
        source.forEach((facet, signals) -> {
            requireName(facet, "facet");
            LinkedHashSet<String> cleanSignals = new LinkedHashSet<>();
            if (signals != null) {
                for (String signal : signals) {
                    requireName(signal, "signal");
                    cleanSignals.add(signal);
                }
            }
            if (!cleanSignals.isEmpty()) {
                sanitized.put(facet, Set.copyOf(cleanSignals));
            }
        });
        return Map.copyOf(sanitized);
    }

    private static void requireName(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
    }
}
