package com.sanhiruzu.atelier.integration.emi;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory;
import com.sanhiruzu.atelier.synthesis.menu.SynthesisStationMenu;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutcome;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.item.SynthesisOutputItemFactory;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class EmiSynthesisRecipe extends BasicEmiRecipe {
    private static final int MAX_VISIBLE_REQUIREMENTS = 4;
    private static final int MAX_VISIBLE_OUTPUTS = 4;

    private final SynthesisProfile profile;
    private final List<RequirementDisplay> requirements;

    private EmiSynthesisRecipe(SynthesisProfile profile) {
        super(AtelierEmiPlugin.SYNTHESIS, EmiRecipeIds.synthetic(ResourceLocation.parse(profile.id())), 170, 96);
        this.profile = profile;
        this.requirements = profile.requirements().stream()
                .map(RequirementDisplay::fromRequirement)
                .toList();
        inputs.addAll(requirements.stream().map(RequirementDisplay::ingredient).toList());
        catalysts.add(EmiStack.of(ZenAtelier.SYNTHESIS_STATION_ITEM.get()));
        outputs.addAll(distinctOutputs(profile.outcomes()));
    }

    static EmiSynthesisRecipe fromProfile(SynthesisProfile profile) {
        return new EmiSynthesisRecipe(profile);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addText(Component.translatable(SynthesisRecipeCategory.translationKey(profile.category())),
                7, 4, SynthesisRecipeCategory.color(profile.category()), false);
        widgets.addText(Component.translatable("jei.zen_atelier.synthesis.outputs"), 116, 4, 0xFF6B5A42, false);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 78, 34);
        widgets.addSlot(EmiStack.of(ZenAtelier.SYNTHESIS_STATION_ITEM.get()), 72, 72).catalyst(true);

        int visibleRequirements = Math.min(requirements.size(), MAX_VISIBLE_REQUIREMENTS);
        for (int i = 0; i < visibleRequirements; i++) {
            RequirementDisplay requirement = requirements.get(i);
            widgets.addSlot(requirement.ingredient(), 8 + (i % 2) * 28, 18 + (i / 2) * 28)
                    .appendTooltip(requirement.tooltip().copy().withStyle(ChatFormatting.GRAY));
            widgets.addText(Component.literal("x" + requirement.requirement().amount()),
                    26 + (i % 2) * 28, 27 + (i / 2) * 28, 0xFF806F56, false);
        }

        int visibleOutputs = Math.min(outputs.size(), MAX_VISIBLE_OUTPUTS);
        for (int i = 0; i < visibleOutputs; i++) {
            widgets.addSlot(outputs.get(i), 118 + (i % 2) * 24, 18 + (i / 2) * 24).recipeContext(this);
        }

        int pctIndoor  = successPct(profile, SynthesisStationMenu.ROOM_CONTEXT_INDOOR);
        int pctAtelier = successPct(profile, SynthesisStationMenu.ROOM_CONTEXT_ATELIER);
        int pctFine    = successPct(profile, SynthesisStationMenu.ROOM_CONTEXT_FINE_ATELIER);
        widgets.addText(Component.literal(pctIndoor + "% / " + pctAtelier + "% / " + pctFine + "%"), 7, 74, 0xFF806F56, false);
        widgets.addText(Component.literal("indoor / atelier / fine"), 7, 83, 0xFF4A3C2C, false);
    }

    private static List<EmiStack> distinctOutputs(List<SynthesisOutcome> outcomes) {
        Map<String, EmiStack> distinct = new LinkedHashMap<>();
        for (SynthesisOutcome outcome : outcomes) {
            outcome.outputs().stream()
                    .map(SynthesisOutputItemFactory::createStack)
                    .map(EmiStack::of)
                    .forEach(stack -> distinct.putIfAbsent(stackKey(stack), stack));
            outcome.byproducts().stream()
                    .map(ReagentItem::createStack)
                    .map(EmiStack::of)
                    .forEach(stack -> distinct.putIfAbsent(stackKey(stack), stack));
        }
        return List.copyOf(distinct.values());
    }

    private static String stackKey(EmiStack stack) {
        return stack.getId() + "|" + stack.getComponentChanges();
    }

    private static int successPct(SynthesisProfile profile, int context) {
        SynthesisProfile effective = SynthesisStationMenu.effectiveProfile(profile, context);
        int total = effective.outcomes().stream().mapToInt(o -> o.weight()).sum();
        int success = effective.outcomes().stream().filter(o -> o.outcomeClass().successful()).mapToInt(o -> o.weight()).sum();
        return total > 0 ? (int) Math.round(100.0 * success / total) : 0;
    }

    private record RequirementDisplay(SynthesisRequirement requirement, EmiIngredient ingredient, Component tooltip) {
        private static RequirementDisplay fromRequirement(SynthesisRequirement requirement) {
            List<EmiStack> stacks = representativeReagents(requirement).stream()
                    .map(ReagentItem::createStack)
                    .map(EmiStack::of)
                    .toList();
            EmiIngredient ingredient = stacks.size() == 1 ? stacks.getFirst() : EmiIngredient.of(stacks);
            return new RequirementDisplay(requirement, ingredient, requirementTooltip(requirement));
        }

        private static List<ReagentStack> representativeReagents(SynthesisRequirement requirement) {
            ReagentQuery query = requirement.query();
            List<String> reagentIds = query.reagentIds().stream()
                    .sorted()
                    .toList();
            if (reagentIds.isEmpty()) {
                reagentIds = List.of(genericReagentId(query));
            }

            List<ReagentStack> reagents = new ArrayList<>();
            for (String reagentId : reagentIds) {
                reagents.add(new ReagentStack(
                        reagentId,
                        requirement.amount(),
                        query.minTier(),
                        query.minQuality(),
                        query.minPurity(),
                        Math.min(100, query.maxInstability()),
                        query.minElements(),
                        query.requiredTraits().stream().sorted().toList(),
                        query.requiredSourceHints()
                ));
            }
            return reagents;
        }

        private static Component requirementTooltip(SynthesisRequirement requirement) {
            ReagentQuery query = requirement.query();
            StringBuilder text = new StringBuilder();
            text.append("Requires ").append(requirement.amount()).append(" reagent");
            if (!query.reagentIds().isEmpty()) {
                text.append(": ").append(readable(query.reagentIds().stream().sorted().toList()));
            }
            if (!query.minElements().isEmpty()) {
                text.append(" | Elements ").append(formatElements(query.minElements()));
            }
            if (!query.requiredTraits().isEmpty()) {
                text.append(" | Traits ").append(readable(query.requiredTraits().stream().sorted().toList()));
            }
            if (query.minTier() > 1 || query.minQuality() > 0 || query.minPurity() > 0 || query.maxInstability() < 100) {
                text.append(" | Tier ").append(query.minTier()).append("-").append(query.maxTier())
                        .append(", Quality ").append(query.minQuality()).append("+")
                        .append(", Purity ").append(query.minPurity()).append("+")
                        .append(", Instability <= ").append(query.maxInstability());
            }
            return Component.literal(text.toString());
        }

        private static String genericReagentId(ReagentQuery query) {
            if (!query.minElements().isEmpty()) {
                String element = query.minElements().keySet().stream()
                        .min(Comparator.naturalOrder())
                        .orElse("matched");
                return "zen_atelier:" + element + "_reagent";
            }
            Set<String> traits = query.requiredTraits();
            if (!traits.isEmpty()) {
                return "zen_atelier:" + traits.stream().sorted().findFirst().orElse("matched") + "_reagent";
            }
            return "zen_atelier:any_reagent";
        }

        private static String formatElements(Map<String, Integer> elements) {
            return elements.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + " " + entry.getValue())
                    .collect(Collectors.joining(", "));
        }

        private static String readable(List<String> ids) {
            return ids.stream()
                    .map(id -> id.contains(":") ? id.substring(id.indexOf(':') + 1) : id)
                    .map(id -> id.replace('_', ' '))
                    .collect(Collectors.joining(", "));
        }
    }
}
