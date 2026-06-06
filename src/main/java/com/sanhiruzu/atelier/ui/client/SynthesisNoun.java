package com.sanhiruzu.atelier.ui.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

enum SynthesisNoun {
    FIRE(Kind.ELEMENT, "Fire", 0xFFE37A61, "fire"),
    WATER(Kind.ELEMENT, "Water", 0xFF76B7E8, "water"),
    EARTH(Kind.ELEMENT, "Earth", 0xFFC5A66D, "earth"),
    WIND(Kind.ELEMENT, "Wind", 0xFF85D7A0, "wind"),
    LIFE(Kind.ELEMENT, "Life", 0xFFA9E27D, "life"),

    ORGANIC(Kind.CATEGORY, "Organic", 0xFF88C978, "organic"),
    MEDICINAL(Kind.CATEGORY, "Medicinal", 0xFF7FD889, "medicinal"),
    BINDING(Kind.CATEGORY, "Binding", 0xFFB06CD7, "binding"),
    ABRASIVE(Kind.CATEGORY, "Abrasive", 0xFFD0BA76, "abrasive"),
    COMBUSTIBLE(Kind.CATEGORY, "Combustible", 0xFFE28552, "combustible"),
    EARTHY(Kind.CATEGORY, "Earthy", 0xFFC5A66D, "earthy"),
    ELASTIC(Kind.CATEGORY, "Elastic", 0xFF8DD6CF, "elastic"),
    PRESERVING(Kind.CATEGORY, "Preserving", 0xFFA6E3E0, "preserving"),
    VOLATILE(Kind.CATEGORY, "Volatile", 0xFFE45A54, "volatile"),
    CONDUCTIVE(Kind.CATEGORY, "Conductive", 0xFFF0C84B, "conductive"),
    FILLER(Kind.CATEGORY, "Filler", 0xFF9B9288, "filler"),
    WASTE(Kind.CATEGORY, "Waste", 0xFF8D7A70, "waste"),

    SOOTHING(Kind.TRAIT, "Soothing", 0xFF8EDC76, "soothing"),
    FRESH(Kind.TRAIT, "Fresh", 0xFFA5E4C8, "fresh"),
    STICKY(Kind.TRAIT, "Sticky", 0xFFB06CD7, "sticky"),
    BOUNCY(Kind.TRAIT, "Bouncy", 0xFF8DD6CF, "bouncy"),
    FRAGILE(Kind.TRAIT, "Fragile", 0xFFD37A6A, "fragile"),
    SPARK(Kind.TRAIT, "Spark", 0xFFFFC857, "spark", "sparking", "spark_reagent"),
    DESTRUCTION(Kind.TRAIT, "Destruction", 0xFFE45A54, "destruction"),
    FLOW(Kind.TRAIT, "Flow", 0xFF76B7E8, "flow"),
    GUARD(Kind.TRAIT, "Guard", 0xFFC5A66D, "guard"),
    SPEED(Kind.TRAIT, "Speed", 0xFF85D7A0, "speed");

    private final Kind kind;
    private final String label;
    private final int color;
    private final Set<String> aliases;

    SynthesisNoun(Kind kind, String label, int color, String... aliases) {
        this.kind = kind;
        this.label = label;
        this.color = color;
        this.aliases = Set.copyOf(Arrays.asList(aliases));
    }

    Kind kind() {
        return kind;
    }

    String label() {
        return label;
    }

    int color() {
        return color;
    }

    Component component() {
        return Component.literal(label).withStyle(style -> style.withColor(color));
    }

    static Optional<SynthesisNoun> find(String id) {
        String key = path(id).toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(noun -> noun.aliases.contains(key))
                .findFirst();
    }

    static Component component(String id) {
        return find(id)
                .map(SynthesisNoun::component)
                .orElseGet(() -> Component.literal(SynthesisStationText.shortLabel(id)));
    }

    static String label(String id) {
        return find(id)
                .map(SynthesisNoun::label)
                .orElseGet(() -> SynthesisStationText.shortLabel(id));
    }

    static int color(String id, int fallback) {
        return find(id)
                .map(SynthesisNoun::color)
                .orElse(fallback);
    }

    static MutableComponent line(Object... parts) {
        MutableComponent line = Component.empty();
        for (Object part : parts) {
            if (part instanceof Component component) {
                line.append(component);
            } else {
                line.append(Component.literal(String.valueOf(part)).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
            }
        }
        return line;
    }

    private static String path(String id) {
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }

    enum Kind {
        ELEMENT,
        CATEGORY,
        TRAIT
    }
}
