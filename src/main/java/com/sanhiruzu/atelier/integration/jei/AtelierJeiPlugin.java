package com.sanhiruzu.atelier.integration.jei;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileDefinition;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileRegistry;
import com.sanhiruzu.atelier.synthesis.data.SynthesisProfileDefinition;
import com.sanhiruzu.atelier.synthesis.data.SynthesisProfileRegistry;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@JeiPlugin
public final class AtelierJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ZenAtelier.REAGENT.get(), new ISubtypeInterpreter<>() {
            @Override
            public @Nullable Object getSubtypeData(ItemStack ingredient, UidContext context) {
                return ReagentItem.getReagent(ingredient);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack ingredient, UidContext context) {
                var reagent = ReagentItem.getReagent(ingredient);
                return reagent == null ? "" : reagent.reagentId();
            }
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new AtelierExtractionCategory(guiHelper),
                new AtelierSynthesisCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AtelierJeiRecipeTypes.EXTRACTION, extractionRecipes());
        registration.addRecipes(AtelierJeiRecipeTypes.SYNTHESIS, synthesisRecipes());

        registration.addItemStackInfo(
                new ItemStack(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get()),
                Component.translatable("jei.zen_atelier.info.extraction_cauldron")
        );
        registration.addItemStackInfo(
                new ItemStack(ZenAtelier.SYNTHESIS_STATION_ITEM.get()),
                Component.translatable("jei.zen_atelier.info.synthesis_station")
        );
        registration.addItemStackInfo(
                new ItemStack(ZenAtelier.ALCHEMIST_PRIMER.get()),
                Component.translatable("jei.zen_atelier.info.extraction_cauldron")
        );
        registration.addItemStackInfo(
                new ItemStack(ZenAtelier.CRUCIBLE_SPOON.get()),
                Component.translatable("jei.zen_atelier.info.extraction_cauldron")
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(AtelierJeiRecipeTypes.EXTRACTION,
                new ItemStack(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get()),
                new ItemStack(Items.CAULDRON));
        registration.addRecipeCatalysts(AtelierJeiRecipeTypes.SYNTHESIS,
                new ItemStack(ZenAtelier.SYNTHESIS_STATION_ITEM.get()));
    }

    private static List<JeiExtractionRecipe> extractionRecipes() {
        List<JeiExtractionRecipe> recipes = new ArrayList<>();
        recipes.add(JeiExtractionRecipe.createPriming());
        ExtractionProfileRegistry.all().stream()
                .sorted(Comparator.comparing(profile -> profile.id().toString()))
                .map(ExtractionProfileDefinition::toCore)
                .map(JeiExtractionRecipe::fromProfile)
                .forEach(recipes::add);
        return recipes;
    }

    private static List<JeiSynthesisRecipe> synthesisRecipes() {
        return SynthesisProfileRegistry.all().stream()
                .sorted(Comparator.comparing(profile -> profile.id().toString()))
                .map(SynthesisProfileDefinition::toCore)
                .map(JeiSynthesisRecipe::fromProfile)
                .toList();
    }
}
