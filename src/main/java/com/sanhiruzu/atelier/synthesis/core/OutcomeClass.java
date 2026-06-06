package com.sanhiruzu.atelier.synthesis.core;

public enum OutcomeClass {
    PERFECT_SUCCESS(true),
    SUCCESS(true),
    UNSTABLE_SUCCESS(true),
    PARTIAL_SUCCESS(true),
    MUTATED_SUCCESS(true),
    DUD(false),
    RECOVERABLE_FAILURE(false),
    MESSY_FAILURE(false),
    CATASTROPHIC_FAILURE(false);

    private final boolean successful;

    OutcomeClass(boolean successful) {
        this.successful = successful;
    }

    public boolean successful() {
        return successful;
    }
}
