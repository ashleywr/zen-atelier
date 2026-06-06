package com.sanhiruzu.atelier.synthesis.core;

import java.util.Set;

public record SourceKey(String value, boolean tag, String id) {
    public SourceKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }

    public static SourceKey parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("source key must not be blank");
        }
        boolean tag = value.startsWith("#");
        String id = tag ? value.substring(1) : value;
        if (id.isBlank()) {
            throw new IllegalArgumentException("source key id must not be blank");
        }
        return new SourceKey(value, tag, id);
    }

    public boolean matches(String itemId, Set<String> itemTags) {
        if (tag) {
            return itemTags.stream().map(SourceKey::normalizeTag).anyMatch(id::equals);
        }
        return id.equals(itemId);
    }

    private static String normalizeTag(String tag) {
        return tag.startsWith("#") ? tag.substring(1) : tag;
    }
}
