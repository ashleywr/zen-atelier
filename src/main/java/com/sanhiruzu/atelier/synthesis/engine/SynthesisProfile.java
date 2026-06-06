package com.sanhiruzu.atelier.synthesis.engine;

import java.util.List;

public record SynthesisProfile(
        String id,
        String category,
        List<SynthesisRequirement> requirements,
        int recipeTierCap,
        SynthesisBoard board,
        List<SynthesisOutcome> outcomes
) {
    public SynthesisProfile(String id, List<SynthesisRequirement> requirements, int recipeTierCap, List<SynthesisOutcome> outcomes) {
        this(id, com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory.MATERIALS, requirements, recipeTierCap, outcomes);
    }

    public SynthesisProfile(String id, String category, List<SynthesisRequirement> requirements, int recipeTierCap, List<SynthesisOutcome> outcomes) {
        this(id, category, requirements, recipeTierCap, SynthesisBoard.CRUDE_3X3, outcomes);
    }

    public SynthesisProfile {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        category = com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory.normalize(category);
        requirements = List.copyOf(requirements);
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("requirements must not be empty");
        }
        if (recipeTierCap <= 0) {
            throw new IllegalArgumentException("recipeTierCap must be positive");
        }
        board = board == null ? SynthesisBoard.CRUDE_3X3 : board;
        outcomes = List.copyOf(outcomes);
        if (outcomes.isEmpty()) {
            throw new IllegalArgumentException("outcomes must not be empty");
        }
    }

    public java.util.Optional<SynthesisOutput> primaryOutput() {
        return outcomes.stream()
                .flatMap(outcome -> outcome.outputs().stream())
                .findFirst();
    }
}
