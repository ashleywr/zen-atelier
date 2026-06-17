package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.engine.OutcomeWeight;
import com.sanhiruzu.atelier.synthesis.engine.RequirementStatus;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.List;

final class SynthesisStationText {
    private SynthesisStationText() {
    }

    static String summarizeQuery(RequirementStatus status) {
        return primaryNeed(status.requirement().query());
    }

    static String requirementLine(RequirementStatus status) {
        return status.availableAmount() + "/" + status.requirement().amount() + " " + primaryNeed(status.requirement().query());
    }

    static String primaryNeed(ReagentQuery query) {
        if (!query.requiredCategories().isEmpty()) {
            return query.requiredCategories().stream()
                    .sorted()
                    .findFirst()
                    .map(SynthesisNoun::label)
                    .orElse("Category");
        }
        if (!query.reagentIds().isEmpty()) {
            return query.reagentIds().stream()
                    .sorted()
                    .findFirst()
                    .map(SynthesisNoun::label)
                    .orElse("Reagent");
        }
        if (!query.requiredTraits().isEmpty()) {
            return query.requiredTraits().stream()
                    .sorted()
                    .findFirst()
                    .map(id -> SynthesisNoun.label(id) + " Trait")
                    .orElse("Trait");
        }
        return "Any Reagent";
    }

    static String elementBudget(ReagentQuery query) {
        if (query.minElements().isEmpty()) {
            return "";
        }
        return query.minElements().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> SynthesisNoun.label(entry.getKey()) + " " + entry.getValue())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    static String traitNeed(ReagentQuery query) {
        if (query.requiredTraits().isEmpty()) {
            return "";
        }
        return query.requiredTraits().stream()
                .sorted()
                .map(SynthesisNoun::label)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    static String queryQualifier(ReagentQuery query) {
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        String elements = elementBudget(query);
        if (!elements.isBlank()) {
            parts.add("Elements " + elements);
        }
        String traits = traitNeed(query);
        if (!traits.isBlank()) {
            parts.add("Trait " + traits);
        }
        if (query.minTier() > 1) {
            parts.add("Tier " + query.minTier() + "+");
        }
        if (query.minQuality() > 0) {
            parts.add("Quality " + query.minQuality() + "+");
        }
        if (query.minPurity() > 0) {
            parts.add("Purity " + query.minPurity() + "+");
        }
        return String.join(", ", parts);
    }

    static String compactElementBudget(java.util.Map<String, Integer> elements) {
        if (elements.isEmpty()) {
            return "None";
        }
        return elements.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> SynthesisNoun.label(entry.getKey()) + " " + entry.getValue())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    static String compactElementBudget(List<ReagentQuery> queries) {
        java.util.LinkedHashMap<String, Integer> elements = new java.util.LinkedHashMap<>();
        queries.stream()
                .flatMap(query -> query.minElements().entrySet().stream())
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> elements.merge(entry.getKey(), entry.getValue(), Integer::sum));
        return compactElementBudget(elements);
    }

    static String compactTraitList(java.util.Set<String> traits) {
        if (traits.isEmpty()) {
            return "From reagents";
        }
        return traits.stream()
                .sorted()
                .map(SynthesisNoun::label)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    static String profileName(SynthesisProfile profile) {
        return profile.primaryOutput()
                .map(output -> Component.translatable("item." + namespace(output.outputId()) + "." + path(output.outputId())).getString())
                .orElseGet(() -> shortId(profile.id()));
    }

    static String shortLabel(String id) {
        return shortId(id);
    }

    static String paletteLabel(String id) {
        String label = shortId(id);
        label = label.replace(" Reagent", "")
                .replace(" Residue", "")
                .replace(" Essence", "")
                .replace(" Solution", "")
                .replace(" Compound", "");
        String[] words = label.split(" ");
        if (words.length <= 2) {
            return label;
        }
        return words[0] + " " + words[1];
    }

    static String compactOutcome(OutcomeWeight weight) {
        return outcomeName(weight) + " " + percent(weight.probability());
    }

    static String outcomeName(OutcomeWeight weight) {
        String name = switch (weight.outcomeClass()) {
            case PERFECT_SUCCESS -> "Perfect";
            case PARTIAL_SUCCESS -> "Partial";
            case MUTATED_SUCCESS -> "Mutated";
            case UNSTABLE_SUCCESS -> "Unstable";
            case SUCCESS -> "Success";
            case DUD -> "Dud";
            case RECOVERABLE_FAILURE -> "Fizzle";
            case MESSY_FAILURE -> "Messy";
            case CATASTROPHIC_FAILURE -> "Fail";
        };
        return name;
    }

    static int outcomeColor(OutcomeWeight weight) {
        return switch (weight.outcomeClass()) {
            case PERFECT_SUCCESS, SUCCESS -> SynthesisScreenTheme.GOOD;
            case PARTIAL_SUCCESS, MUTATED_SUCCESS, UNSTABLE_SUCCESS -> SynthesisScreenTheme.ACCENT;
            case DUD, RECOVERABLE_FAILURE -> SynthesisScreenTheme.MUTED;
            case MESSY_FAILURE, CATASTROPHIC_FAILURE -> SynthesisScreenTheme.BAD;
        };
    }

    static String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0D);
    }

    static String fitWidth(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String ellipsis = "...";
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ellipsis))) + ellipsis;
    }

    static void drawFit(GuiGraphics graphics, Font font, String text, ScreenRect rect, int color) {
        graphics.drawString(font, fitWidth(font, text, rect.width()), rect.x(), rect.y(), color, false);
    }

    static void drawFit(GuiGraphics graphics, Font font, Component text, ScreenRect rect, int color) {
        drawFit(graphics, font, text.getString(), rect, color);
    }

    static void drawRichFit(GuiGraphics graphics, Font font, Component text, ScreenRect rect, int fallbackColor) {
        if (font.width(text) <= rect.width()) {
            graphics.drawString(font, text, rect.x(), rect.y(), fallbackColor, false);
            return;
        }
        drawFit(graphics, font, text.getString(), rect, fallbackColor);
    }

    static void drawCenteredFit(GuiGraphics graphics, Font font, Component text, ScreenRect rect, int color) {
        String fitted = fitWidth(font, text.getString(), rect.width());
        int x = rect.x() + (rect.width() - font.width(fitted)) / 2;
        int y = rect.y() + Math.max(0, (rect.height() - 8) / 2);
        graphics.drawString(font, fitted, x, y, color, false);
    }

    private static String shortId(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] words = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.isEmpty() ? id : builder.toString();
    }

    private static String namespace(String id) {
        return id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
    }

    private static String path(String id) {
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }
}
