package com.sanhiruzu.atelier.synthesis.core;

import java.util.ArrayList;
import java.util.List;

public record RollTrace(List<String> lines) {
    public RollTrace {
        lines = List.copyOf(lines);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<String> lines = new ArrayList<>();

        public Builder add(String line) {
            lines.add(line);
            return this;
        }

        public RollTrace build() {
            return new RollTrace(lines);
        }
    }
}
