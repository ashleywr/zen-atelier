package com.sanhiruzu.atelier.synthesis.core;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SynthesisRecipeCategory {
    public static final String MATERIALS = "materials";
    public static final String MISC = "misc";
    private static final List<String> ORDER = List.of(
            "bombs",
            "healing",
            "food",
            "tools",
            MATERIALS,
            MISC
    );
    private static final Map<String, Integer> COLORS = Map.of(
            "bombs", 0xFFE86A4A,
            "healing", 0xFF65C981,
            "food", 0xFFE0B45D,
            "tools", 0xFF6BA8E8,
            MATERIALS, 0xFFC6A56A,
            MISC, 0xFFA78ACB
    );

    private SynthesisRecipeCategory() {
    }

    public static List<String> orderedIds() {
        return ORDER;
    }

    public static String normalize(String category) {
        if (category == null || category.isBlank()) {
            return MATERIALS;
        }
        String normalized = category.toLowerCase(Locale.ROOT).replace(' ', '_');
        return ORDER.contains(normalized) ? normalized : MISC;
    }

    public static int color(String category) {
        return COLORS.getOrDefault(normalize(category), COLORS.get(MISC));
    }

    public static int order(String category) {
        int index = ORDER.indexOf(normalize(category));
        return index < 0 ? ORDER.size() : index;
    }

    public static String translationKey(String category) {
        return "screen.zen_atelier.synthesis.category." + normalize(category);
    }

    public static String tabTranslationKey(String category) {
        return "screen.zen_atelier.synthesis.category_tab." + normalize(category);
    }

    public static Comparator<String> comparator() {
        return Comparator.comparingInt(SynthesisRecipeCategory::order)
                .thenComparing(SynthesisRecipeCategory::normalize);
    }
}
