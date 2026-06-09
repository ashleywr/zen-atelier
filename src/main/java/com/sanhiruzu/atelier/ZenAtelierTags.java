package com.sanhiruzu.atelier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ZenAtelierTags {
    private ZenAtelierTags() {}

    public static final class Items {
        private Items() {}

        public static final TagKey<Item> THROWABLE = item("throwable");
        public static final TagKey<Item> CONSUMABLE = item("consumable");

        private static TagKey<Item> item(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, path));
        }
    }
}
