package com.sanhiruzu.atelier.integration.jei;

import com.sanhiruzu.atelier.ZenAtelier;
import mezz.jei.api.recipe.RecipeType;

final class AtelierJeiRecipeTypes {
    static final RecipeType<JeiExtractionRecipe> EXTRACTION =
            RecipeType.create(ZenAtelier.MODID, "extraction", JeiExtractionRecipe.class);
    static final RecipeType<JeiSynthesisRecipe> SYNTHESIS =
            RecipeType.create(ZenAtelier.MODID, "synthesis", JeiSynthesisRecipe.class);

    private AtelierJeiRecipeTypes() {
    }
}
