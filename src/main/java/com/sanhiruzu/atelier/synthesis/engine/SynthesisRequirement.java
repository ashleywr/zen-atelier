package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;

public record SynthesisRequirement(ReagentQuery query, int amount) {
    public SynthesisRequirement {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
