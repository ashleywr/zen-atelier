package com.sanhiruzu.atelier.integration.ami;

import com.sanhiruzu.ami.index.FacetIndexer;
import com.sanhiruzu.ami.index.FacetProfile;
import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.environment.EnvironmentFacetSignals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class AtelierAmiEnvironmentFacets {
    private static final Map<Item, Set<String>> CACHE = new ConcurrentHashMap<>();
    private static boolean registered;

    private AtelierAmiEnvironmentFacets() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }
        registered = true;
        EnvironmentFacetSignals.registerProvider(AtelierAmiEnvironmentFacets::facetsFor);
        ZenAtelier.LOGGER.info("AMI environment facet bridge enabled");
    }

    private static Set<String> facetsFor(BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            return Set.of();
        }
        return CACHE.computeIfAbsent(item, AtelierAmiEnvironmentFacets::indexItemFacets);
    }

    private static Set<String> indexItemFacets(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
            return Set.of();
        }
        FacetProfile profile = FacetIndexer.index(item, itemId, new ItemStack(item));
        return profile.facets().stream()
                .map(facet -> "ami:" + facet.id())
                .collect(Collectors.toUnmodifiableSet());
    }
}
