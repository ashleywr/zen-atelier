package com.sanhiruzu.atelier.integration.emi;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileDefinition;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileRegistry;
import com.sanhiruzu.atelier.synthesis.data.SynthesisProfileDefinition;
import com.sanhiruzu.atelier.synthesis.data.SynthesisProfileRegistry;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

@EmiEntrypoint
public final class AtelierEmiPlugin implements EmiPlugin {
    static final EmiRecipeCategory EXTRACTION = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "extraction"),
            EmiStack.of(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get())
    );
    static final EmiRecipeCategory SYNTHESIS = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "synthesis"),
            EmiStack.of(ZenAtelier.SYNTHESIS_STATION_ITEM.get())
    );

    @Override
    public void register(EmiRegistry registry) {
        registry.setDefaultComparison(ZenAtelier.REAGENT.get(), Comparison.compareComponents());

        registry.addCategory(EXTRACTION);
        registry.addCategory(SYNTHESIS);
        registry.addWorkstation(EXTRACTION, EmiStack.of(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get()));
        registry.addWorkstation(EXTRACTION, EmiStack.of(Items.CAULDRON));
        registry.addWorkstation(SYNTHESIS, EmiStack.of(ZenAtelier.SYNTHESIS_STATION_ITEM.get()));

        registry.addRecipe(EmiExtractionRecipe.priming());
        ExtractionProfileRegistry.all().stream()
                .sorted(Comparator.comparing(profile -> profile.id().toString()))
                .map(ExtractionProfileDefinition::toCore)
                .map(EmiExtractionRecipe::fromProfile)
                .forEach(registry::addRecipe);

        SynthesisProfileRegistry.all().stream()
                .sorted(Comparator.comparing(profile -> profile.id().toString()))
                .map(SynthesisProfileDefinition::toCore)
                .map(EmiSynthesisRecipe::fromProfile)
                .forEach(registry::addRecipe);

        registry.addRecipe(new EmiInfoRecipe(
                List.of(
                        EmiStack.of(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get()),
                        EmiStack.of(ZenAtelier.ALCHEMIST_PRIMER.get()),
                        EmiStack.of(ZenAtelier.CRUCIBLE_SPOON.get())
                ),
                List.of(Component.translatable("jei.zen_atelier.info.extraction_cauldron")),
                EmiRecipeIds.synthetic(ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "info/extraction_cauldron"))
        ));
        registry.addRecipe(new EmiInfoRecipe(
                List.of(EmiStack.of(ZenAtelier.SYNTHESIS_STATION_ITEM.get())),
                List.of(Component.translatable("jei.zen_atelier.info.synthesis_station")),
                EmiRecipeIds.synthetic(ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "info/synthesis_station"))
        ));
    }
}
