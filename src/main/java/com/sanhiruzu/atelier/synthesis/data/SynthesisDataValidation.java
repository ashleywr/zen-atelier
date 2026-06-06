package com.sanhiruzu.atelier.synthesis.data;

import com.mojang.serialization.DataResult;

import java.util.Map;

final class SynthesisDataValidation {
    private SynthesisDataValidation() {
    }

    static DataResult<Integer> schema(int schema) {
        return schema == 1 ? DataResult.success(schema) : error("schema must be 1");
    }

    static DataResult<Integer> positive(String field, int value) {
        return value > 0 ? DataResult.success(value) : error(field + " must be positive");
    }

    static DataResult<Integer> tier(String field, int value) {
        return range(field, value, 1, 6);
    }

    static DataResult<Integer> percent(String field, int value) {
        return range(field, value, 0, 100);
    }

    static DataResult<Integer> range(String field, int value, int min, int max) {
        return value >= min && value <= max
                ? DataResult.success(value)
                : error(field + " must be between " + min + " and " + max);
    }

    static DataResult<Map<String, Integer>> positiveElementValues(String field, Map<String, Integer> elements) {
        for (Map.Entry<String, Integer> entry : elements.entrySet()) {
            if (entry.getKey().isBlank()) {
                return error(field + " keys must not be blank");
            }
            if (entry.getValue() <= 0) {
                return error(field + " values must be positive");
            }
        }
        return DataResult.success(elements);
    }

    static <T> DataResult<T> error(String message) {
        return DataResult.error(() -> message);
    }
}
