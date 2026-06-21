package com.sanhiruzu.atelier.api;

@FunctionalInterface
public interface EnvironmentCondition {
    boolean matches(EnvironmentSnapshot snapshot);
}
