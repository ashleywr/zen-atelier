package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.resources.ResourceLocation;

final class AtelierUiSprites {
    static final ResourceLocation WINDOW = sprite("window");
    static final ResourceLocation PANEL = sprite("panel");
    static final ResourceLocation RECESSED_PANEL = sprite("panel_recessed");
    static final ResourceLocation SLOT = sprite("slot");
    static final ResourceLocation RECIPE_CELL = sprite("recipe_cell");
    static final ResourceLocation RECIPE_CELL_SELECTED = sprite("recipe_cell_selected");
    static final ResourceLocation BUTTON = sprite("button");
    static final ResourceLocation BUTTON_HOVERED = sprite("button_hovered");

    private AtelierUiSprites() {
    }

    private static ResourceLocation sprite(String path) {
        return ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "synthesis/" + path);
    }
}
