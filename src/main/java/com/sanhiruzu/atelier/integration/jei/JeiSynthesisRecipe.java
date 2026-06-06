package com.sanhiruzu.atelier.integration.jei;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutcome;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.item.SynthesisOutputItemFactory;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

record JeiSynthesisRecipe(
        ResourceLocation id,
        SynthesisProfile profile,
        List<RequirementDisplay> requirements,
        List<ItemStack> outputs
) {
    static JeiSynthesisRecipe fromProfile(SynthesisProfile profile) {
        List<RequirementDisplay> requirements = profile.requirements().stream()
                .map(RequirementDisplay::fromRequirement)
                .toList();
        List<ItemStack> outputs = distinctOutputs(profile.outcomes());
        return new JeiSynthesisRecipe(ResourceLocation.parse(profile.id()), profile, requirements, outputs);
    }

    private static List<ItemStack> distinctOutputs(List<SynthesisOutcome> outcomes) {
        Map<String, ItemStack> distinct = new LinkedHashMap<>();
        for (SynthesisOutcome outcome : outcomes) {
            outcome.outputs().stream()
                    .map(SynthesisOutputItemFactory::createStack)
                    .forEach(stack -> distinct.putIfAbsent(stackKey(stack), stack));
            outcome.byproducts().stream()
                    .map(ReagentItem::createStack)
                    .forEach(stack -> distinct.putIfAbsent(stackKey(stack), stack));
        }
        return List.copyOf(distinct.values());
    }

    private static String stackKey(ItemStack stack) {
        return stack.getItem() + "|" + stack.getComponentsPatch();
    }

    record RequirementDisplay(SynthesisRequirement requirement, List<ItemStack> stacks) {
        private static RequirementDisplay fromRequirement(SynthesisRequirement requirement) {
            List<ReagentStack> reagents = representativeReagents(requirement);
            return new RequirementDisplay(requirement, reagents.stream().map(ReagentItem::createStack).toList());
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
    }
}
