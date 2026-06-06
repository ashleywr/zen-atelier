package com.sanhiruzu.atelier.integration.ami;

import com.sanhiruzu.ami.api.AmiApi;
import com.sanhiruzu.ami.api.AmiContextMenuAction;
import com.sanhiruzu.ami.api.AmiItemContext;
import com.sanhiruzu.ami.api.IAmiPlugin;
import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static java.util.Map.entry;

public final class AtelierAmiPlugin implements IAmiPlugin {
    private static final ResourceLocation REAGENT_ITEM_ID = ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "reagent");
    private static final ResourceLocation ALCHEMIST_CODEX_ID = ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "alchemist_codex");
    private static final ResourceLocation ALCHEMIST_LENS_ID = ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "alchemist_lens");
    private static final List<SampleReagent> SAMPLE_REAGENTS = List.of(
            new SampleReagent("abrasive", "zen_atelier:abrasive_reagent", 'a',
                    Map.of("earth", 1, "sharp", 2), List.of("abrasive"), Set.of("ami:debug")),
            new SampleReagent("binding", "zen_atelier:binding_reagent", 'b',
                    Map.of("life", 1, "binding", 2), List.of("binding", "soothing"), Set.of("ami:debug")),
            new SampleReagent("conductive", "zen_atelier:conductive_reagent", 'c',
                    Map.of("metal", 1, "conductive", 2), List.of("conductive"), Set.of("ami:debug")),
            new SampleReagent("organic", "zen_atelier:organic_reagent", 'o',
                    Map.of("life", 1, "decay", 1), List.of("organic", "fermenting"), Set.of("ami:debug")),
            new SampleReagent("preserving", "zen_atelier:preserving_reagent", 'p',
                    Map.of("life", 1, "binding", 2, "preserving", 2), List.of("binding", "preserving", "stable"), Set.of("ami:debug"))
    );

    @Override
    public List<ItemStack> getHeroItems() {
        return List.of(
                new ItemStack(ZenAtelier.ALCHEMIST_CODEX.get()),
                new ItemStack(ZenAtelier.ALCHEMIST_LENS.get()),
                new ItemStack(ZenAtelier.ALCHEMIST_PRIMER.get()),
                new ItemStack(ZenAtelier.CRUCIBLE_SPOON.get()),
                new ItemStack(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get()),
                new ItemStack(ZenAtelier.SYNTHESIS_STATION_ITEM.get()),
                ReagentItem.createStack(universalReagent())
        );
    }

    @Override
    public void addItemContextMenuActions(AmiItemContext context, Consumer<AmiContextMenuAction> actions) {
        if (context == null || actions == null || !context.cheatEnabled() || !context.isItem()) {
            return;
        }
        if (ALCHEMIST_CODEX_ID.equals(context.id()) || ALCHEMIST_LENS_ID.equals(context.id())) {
            actions.accept(AmiContextMenuAction.enabled(
                    ZenAtelier.MODID + ":give_discovery_kit",
                    Component.translatable("zen_atelier.ami.context.give_discovery_kit"),
                    'k',
                    AtelierAmiPlugin::giveDiscoveryKit
            ));
            return;
        }

        if (!REAGENT_ITEM_ID.equals(context.id())) {
            return;
        }

        actions.accept(AmiContextMenuAction.enabled(
                ZenAtelier.MODID + ":spawn_random_reagent",
                Component.translatable("zen_atelier.ami.context.spawn_random_reagent"),
                'r',
                () -> AmiApi.cheatGiveItem(ReagentItem.createStack(randomReagent()))
        ));
        actions.accept(AmiContextMenuAction.enabled(
                ZenAtelier.MODID + ":spawn_universal_reagent",
                Component.translatable("zen_atelier.ami.context.spawn_universal_reagent"),
                'u',
                () -> AmiApi.cheatGiveItem(ReagentItem.createStack(universalReagent()))
        ));

        for (SampleReagent reagent : SAMPLE_REAGENTS) {
            actions.accept(AmiContextMenuAction.enabled(
                    ZenAtelier.MODID + ":spawn_" + reagent.shortName(),
                    Component.translatable("zen_atelier.ami.context.spawn_reagent", reagent.displayName()),
                    reagent.mnemonic(),
                    () -> AmiApi.cheatGiveItem(ReagentItem.createStack(reagent.stack()))
            ));
        }
    }

    private static void giveDiscoveryKit() {
        discoveryKitItems().forEach(AmiApi::cheatGiveItem);
    }

    private static List<ItemStack> discoveryKitItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(new ItemStack(ZenAtelier.ALCHEMIST_CODEX.get()));
        items.add(new ItemStack(ZenAtelier.ALCHEMIST_LENS.get()));
        items.add(new ItemStack(ZenAtelier.ALCHEMIST_PRIMER.get()));
        items.add(new ItemStack(ZenAtelier.CRUCIBLE_SPOON.get()));
        items.add(new ItemStack(ZenAtelier.EXTRACTION_CAULDRON_ITEM.get()));
        items.add(new ItemStack(ZenAtelier.SYNTHESIS_STATION_ITEM.get()));
        items.add(new ItemStack(ZenAtelier.UNI.get(), 8));
        items.add(new ItemStack(ZenAtelier.TAUN_HERB.get(), 8));
        items.add(new ItemStack(ZenAtelier.PHLOGISTON_PEBBLE.get(), 8));
        items.add(new ItemStack(ZenAtelier.AQUA_GEL.get(), 8));
        items.add(new ItemStack(ZenAtelier.EMBER_GEL.get(), 8));
        items.add(new ItemStack(Items.FLINT, 8));
        items.add(new ItemStack(Items.HONEY_BOTTLE, 4));
        items.add(new ItemStack(Items.COPPER_INGOT, 8));
        items.add(new ItemStack(Items.ROTTEN_FLESH, 8));
        items.add(ReagentItem.createStack(universalReagent()));
        return items;
    }

    private static ReagentStack randomReagent() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        SampleReagent template = SAMPLE_REAGENTS.get(random.nextInt(SAMPLE_REAGENTS.size()));
        return new ReagentStack(
                template.reagentId(),
                random.nextInt(24, 97),
                random.nextInt(1, 4),
                random.nextInt(15, 101),
                random.nextInt(20, 101),
                random.nextInt(0, 46),
                template.elements(),
                template.traits(),
                Set.of("ami:random_debug")
        );
    }

    private static ReagentStack universalReagent() {
        return new ReagentStack(
                ReagentQuery.DEBUG_UNIVERSAL_REAGENT_ID,
                256,
                6,
                100,
                100,
                0,
                Map.ofEntries(
                        entry("earth", 9),
                        entry("sharp", 9),
                        entry("life", 9),
                        entry("binding", 9),
                        entry("metal", 9),
                        entry("conductive", 9),
                        entry("decay", 9),
                        entry("preserving", 9),
                        entry("fire", 9),
                        entry("water", 9),
                        entry("air", 9)
                ),
                List.of("abrasive", "binding", "soothing", "conductive", "organic", "fermenting", "preserving", "stable"),
                Set.of("ami:universal_debug")
        );
    }

    private record SampleReagent(
            String shortName,
            String reagentId,
            char mnemonic,
            Map<String, Integer> elements,
            List<String> traits,
            Set<String> sourceHints
    ) {
        private ReagentStack stack() {
            return new ReagentStack(reagentId, 64, 1, 60, 70, 10, elements, traits, sourceHints);
        }

        private Component displayName() {
            return Component.translatable("zen_atelier.reagent." + shortName);
        }
    }
}
