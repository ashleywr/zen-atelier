package com.sanhiruzu.atelier.ui.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

final class AlchemyDiscoveryIcons {
    private AlchemyDiscoveryIcons() {
    }

    static ItemStack sourceIcon(String sourceId) {
        if (sourceId == null || sourceId.isBlank() || sourceId.startsWith("#")) {
            return new ItemStack(Items.NAME_TAG);
        }

        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(sourceId));
            if (item == Items.AIR) {
                return new ItemStack(Items.PAPER);
            }
            return new ItemStack(item);
        } catch (RuntimeException ignored) {
            return new ItemStack(Items.PAPER);
        }
    }

    static ItemStack reagentIcon(String id) {
        String key = path(id).toLowerCase(Locale.ROOT);
        if (key.contains("binding") || key.contains("preserving")) {
            return new ItemStack(Items.HONEY_BOTTLE);
        }
        if (key.contains("fibrous") || key.contains("fiber") || key.contains("thread")) {
            return new ItemStack(Items.STRING);
        }
        if (key.contains("luminous") || key.contains("glow")) {
            return new ItemStack(Items.GLOWSTONE_DUST);
        }
        if (key.contains("quickening") || key.contains("sugar")) {
            return new ItemStack(Items.SUGAR);
        }
        if (key.contains("harmonic") || key.contains("resonant") || key.contains("crystal")) {
            return new ItemStack(Items.AMETHYST_SHARD);
        }
        if (key.contains("elastic") || key.contains("sticky") || key.contains("slime")) {
            return new ItemStack(Items.SLIME_BALL);
        }
        if (key.contains("organic") || key.contains("verdant") || key.contains("life")) {
            return new ItemStack(Items.OAK_LEAVES);
        }
        if (key.contains("abrasive") || key.contains("sharp")) {
            return new ItemStack(Items.FLINT);
        }
        if (key.contains("spark") || key.contains("heat") || key.contains("fire")) {
            return new ItemStack(Items.BLAZE_POWDER);
        }
        if (key.contains("conductive") || key.contains("copper")) {
            return new ItemStack(Items.COPPER_INGOT);
        }
        if (key.contains("stone") || key.contains("earth") || key.contains("mineral")) {
            return new ItemStack(Items.AMETHYST_SHARD);
        }
        return new ItemStack(Items.GLASS_BOTTLE);
    }

    static ItemStack filterIcon(AlchemistCodexScreen.Filter filter) {
        return switch (filter) {
            case ALL -> new ItemStack(Items.COMPASS);
            case KNOWN -> new ItemStack(Items.WRITABLE_BOOK);
            case SUSPECTED -> new ItemStack(Items.SPYGLASS);
            case EMPTY -> new ItemStack(Items.BARRIER);
        };
    }

    static ItemStack modeIcon(AlchemistCodexScreen.Mode mode) {
        return switch (mode) {
            case SOURCES -> new ItemStack(Items.WRITABLE_BOOK);
            case GOALS -> new ItemStack(Items.CRAFTING_TABLE);
        };
    }

    static ItemStack clearGoalIcon() {
        return new ItemStack(Items.BARRIER);
    }

    static ItemStack elementIcon(String id) {
        String key = path(id).toLowerCase(Locale.ROOT);
        if (key.contains("binding")) {
            return new ItemStack(Items.HONEYCOMB);
        }
        if (key.contains("conductive") || key.contains("metal")) {
            return new ItemStack(Items.COPPER_INGOT);
        }
        if (key.contains("fiber") || key.contains("fibrous")) {
            return new ItemStack(Items.STRING);
        }
        if (key.contains("luminous") || key.contains("glow") || key.contains("light")) {
            return new ItemStack(Items.GLOWSTONE_DUST);
        }
        if (key.contains("quickening") || key.contains("sweet")) {
            return new ItemStack(Items.SUGAR);
        }
        if (key.contains("harmonic") || key.contains("resonant") || key.contains("crystal")) {
            return new ItemStack(Items.AMETHYST_SHARD);
        }
        if (key.contains("elastic") || key.contains("sticky")) {
            return new ItemStack(Items.SLIME_BALL);
        }
        if (key.contains("sharp")) {
            return new ItemStack(Items.FLINT);
        }
        if (key.contains("fire") || key.contains("heat")) {
            return new ItemStack(Items.BLAZE_POWDER);
        }
        if (key.contains("life") || key.contains("organic")) {
            return new ItemStack(Items.OAK_SAPLING);
        }
        if (key.contains("decay")) {
            return new ItemStack(Items.ROTTEN_FLESH);
        }
        if (key.contains("earth") || key.contains("stone")) {
            return new ItemStack(Items.COBBLESTONE);
        }
        return new ItemStack(Items.AMETHYST_SHARD);
    }

    static String readableId(String id) {
        String path = path(id);
        String[] parts = path.replace('_', ' ').split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isBlank()) {
                parts[i] = parts[i].substring(0, 1).toUpperCase(Locale.ROOT) + parts[i].substring(1);
            }
        }
        return String.join(" ", parts);
    }

    private static String path(String id) {
        String clean = id.startsWith("#") ? id.substring(1) : id;
        return clean.contains(":") ? clean.substring(clean.indexOf(':') + 1) : clean;
    }
}
