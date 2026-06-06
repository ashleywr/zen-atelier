package com.sanhiruzu.atelier.integration.emi;

import net.minecraft.resources.ResourceLocation;

final class EmiRecipeIds {
    private EmiRecipeIds() {
    }

    static ResourceLocation synthetic(ResourceLocation id) {
        if (id.getPath().startsWith("/")) {
            return id;
        }
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "/" + id.getPath());
    }
}
