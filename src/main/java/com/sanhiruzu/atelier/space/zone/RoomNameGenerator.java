package com.sanhiruzu.atelier.space.zone;

import java.util.*;

public final class RoomNameGenerator {
    // Color name mappings from block name color prefixes
    private static final Map<String, String> COLOR_NAMES = Map.ofEntries(
            Map.entry("red_", "Red"),
            Map.entry("blue_", "Blue"),
            Map.entry("purple_", "Purple"),
            Map.entry("magenta_", "Magenta"),
            Map.entry("pink_", "Pink"),
            Map.entry("green_", "Green"),
            Map.entry("lime_", "Lime"),
            Map.entry("cyan_", "Cyan"),
            Map.entry("light_blue_", "Sky"),
            Map.entry("orange_", "Orange"),
            Map.entry("yellow_", "Yellow"),
            Map.entry("brown_", "Brown"),
            Map.entry("gray_", "Gray"),
            Map.entry("grey_", "Gray"),
            Map.entry("light_gray_", "Pearl"),
            Map.entry("light_grey_", "Pearl"),
            Map.entry("white_", "White"),
            Map.entry("black_", "Black")
    );

    private RoomNameGenerator() {
    }

    public static String generateName(RoomData room) {
        Random seededRandom = new Random(room.getRegionId().hashCode());

        String material = detectMaterial(room.getSurfaceCounts(), seededRandom);
        String descriptor = detectDescriptor(room, seededRandom);

        if (descriptor != null && material != null && descriptor.equals(material)) {
            descriptor = null;
        }

        // Randomize whether to include material/color for variety
        boolean includeMaterial = material != null && seededRandom.nextDouble() > 0.3;

        if (descriptor != null && includeMaterial) {
            return descriptor + " " + material;
        } else if (includeMaterial) {
            return material;
        } else if (descriptor != null) {
            return descriptor;
        } else {
            return null;
        }
    }

    private static String detectMaterial(Map<String, Integer> surfaceCounts, Random random) {
        if (surfaceCounts.isEmpty()) {
            return null;
        }

        Map<String, Integer> materialScores = new HashMap<>();

        for (Map.Entry<String, Integer> entry : surfaceCounts.entrySet()) {
            String blockKey = entry.getKey();
            int count = entry.getValue();

            // Try color detection first (works for any mod)
            String color = detectColor(blockKey);
            if (color != null) {
                materialScores.merge(color, count, Integer::sum);
                continue;
            }

            // Fall back to material family classification
            String family = classifyBlockFamily(blockKey);
            if (family != null) {
                materialScores.merge(family, count, Integer::sum);
            }
        }

        if (materialScores.isEmpty()) {
            return null;
        }

        int maxScore = materialScores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> winners = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : materialScores.entrySet()) {
            if (entry.getValue() == maxScore) {
                winners.add(entry.getKey());
            }
        }

        return winners.get(random.nextInt(winners.size()));
    }

    // Generic color detection from block name — works for any mod
    private static String detectColor(String blockKey) {
        String lowerKey = blockKey.toLowerCase();
        for (Map.Entry<String, String> colorEntry : COLOR_NAMES.entrySet()) {
            if (lowerKey.contains(colorEntry.getKey())) {
                return colorEntry.getValue();
            }
        }
        return null;
    }

    private static String classifyBlockFamily(String blockKey) {
        String lowerKey = blockKey.toLowerCase();

        // Wood detection (works for any mod wood type)
        if (containsAny(lowerKey, "oak_")) return "Oak";
        if (containsAny(lowerKey, "birch_")) return "Birch";
        if (containsAny(lowerKey, "spruce_")) return "Spruce";
        if (containsAny(lowerKey, "dark_oak_")) return "Dark Oak";
        if (containsAny(lowerKey, "acacia_")) return "Acacia";
        if (containsAny(lowerKey, "jungle_")) return "Jungle";
        if (containsAny(lowerKey, "mangrove_")) return "Mangrove";
        if (containsAny(lowerKey, "cherry_")) return "Cherry";
        if (containsAny(lowerKey, "crimson_")) return "Crimson";
        if (containsAny(lowerKey, "warped_")) return "Warped";
        if (lowerKey.contains("wood") && !lowerKey.contains("log")) return "Wood";

        if (containsAny(lowerKey, "stone_brick", "brick")) return "Stone Brick";
        if (containsAny(lowerKey, "cobblestone")) return "Cobblestone";
        if (lowerKey.equals("minecraft:stone")) return "Stone";
        if (containsAny(lowerKey, "granite")) return "Granite";
        if (containsAny(lowerKey, "diorite")) return "Diorite";
        if (containsAny(lowerKey, "andesite")) return "Andesite";

        if (containsAny(lowerKey, "deepslate_brick", "deepslate_tile", "cobbled_deepslate")) return "Deepslate";
        if (containsAny(lowerKey, "blackstone", "polished_blackstone")) return "Blackstone";
        if (containsAny(lowerKey, "sandstone")) return "Sandstone";
        if (containsAny(lowerKey, "red_sandstone")) return "Red Sandstone";
        if (containsAny(lowerKey, "nether_brick")) return "Nether Brick";

        if (containsAny(lowerKey, "quartz")) return "Quartz";
        if (containsAny(lowerKey, "prismarine")) return "Prismarine";
        if (containsAny(lowerKey, "purpur")) return "Purpur";
        if (containsAny(lowerKey, "mud_brick", "packed_mud")) return "Mud Brick";

        if (containsAny(lowerKey, "dirt", "grass_block", "rooted_dirt")) return "Earthen";
        if (containsAny(lowerKey, "sand", "gravel")) return "Sandy";

        if (containsAny(lowerKey, "gold_block", "gilded_blackstone")) return "Gilded";
        if (containsAny(lowerKey, "diamond_block")) return "Crystal";
        if (containsAny(lowerKey, "emerald_block")) return "Verdant";
        if (containsAny(lowerKey, "amethyst_block", "budding_amethyst")) return "Amethyst";
        if (containsAny(lowerKey, "copper_block", "cut_copper", "exposed_copper")) return "Copper";
        if (containsAny(lowerKey, "iron_block")) return "Iron";

        if (containsAny(lowerKey, "snow_block", "powder_snow", "ice", "blue_ice")) return "Frozen";
        if (containsAny(lowerKey, "packed_ice")) return "Ice";
        if (containsAny(lowerKey, "obsidian", "crying_obsidian")) return "Obsidian";
        if (containsAny(lowerKey, "calcite", "tuff")) return "Tuff";

        // Generic material detection (works for modded items)
        if (lowerKey.contains("concrete")) return "Concrete";
        if (lowerKey.contains("terracotta")) return "Terracotta";
        if (lowerKey.contains("wool")) return "Wool";
        if (lowerKey.contains("stained_glass")) return "Stained Glass";

        return null;
    }

    private static String detectDescriptor(RoomData room, Random random) {
        Map<String, Integer> descriptorScores = new HashMap<>();

        Map<String, Integer> furniture = room.getFurnitureCounts();
        Map<String, Integer> signals = room.getSignalCounts();
        Map<String, Integer> surfaces = room.getSurfaceCounts();

        // Outdoor/open-air flavor — low priority so furniture descriptors can override
        if (room.isDegraded()) {
            if (room.getEnclosureScore() < 0.3f) {
                descriptorScores.merge("Sunlit", 2, Integer::sum);
                descriptorScores.merge("Open", 2, Integer::sum);
                descriptorScores.merge("Wild", 2, Integer::sum);
            } else {
                descriptorScores.merge("Breezy", 2, Integer::sum);
                descriptorScores.merge("Alfresco", 2, Integer::sum);
                descriptorScores.merge("Airy", 2, Integer::sum);
            }
        }

        // Bed color detection — adds significant variety based on bed color
        String bedColor = detectBedColor(furniture);
        if (bedColor != null) {
            descriptorScores.merge(bedColor, 2, Integer::sum);
        }

        // Lighting and atmosphere
        if (countBlocksMatching(furniture, "furnace", "campfire", "smoker", "blast_furnace") >= 1) {
            descriptorScores.merge("Smoky", 1, Integer::sum);
        }

        if (countBlocksMatching(furniture, "candle", "lantern", "sea_lantern", "torch") >= 3) {
            descriptorScores.merge("Candlelit", 2, Integer::sum);
        }

        if (countBlocksMatching(furniture, "lantern", "sea_lantern", "glowstone", "shroomlight", "glow_lichen") >= 4) {
            descriptorScores.merge("Bright", 2, Integer::sum);
        }

        // Windows and openness
        int glassCount = countBlocksMatching(surfaces, "glass", "glass_pane");
        if (glassCount >= 5) {
            descriptorScores.merge("Windowed", 2, Integer::sum);
        }

        // Precious materials
        if (countBlocksMatching(furniture, "gold_block", "gold_ingot", "gilded") >= 1) {
            descriptorScores.merge("Gilded", 2, Integer::sum);
        }

        if (countBlocksMatching(furniture, "gold_block", "diamond_block", "emerald_block") >= 2) {
            descriptorScores.merge("Lavish", 2, Integer::sum);
        }

        if (countBlocksMatching(furniture, "amethyst", "diamond_block") >= 1) {
            descriptorScores.merge("Crystal", 1, Integer::sum);
        }

        // Decorative and ornate
        if (countBlocksMatching(furniture, "gold", "quartz", "amethyst_cluster", "copper") >= 3) {
            descriptorScores.merge("Ornate", 2, Integer::sum);
        }

        // Nature and plants
        if (countBlocksMatching(furniture, "moss_block", "moss_carpet", "fern", "vine") >= 2) {
            descriptorScores.merge("Mossy", 1, Integer::sum);
        }

        if (countBlocksMatching(furniture, "leaves", "azalea", "hanging_roots", "glow_lichen") >= 4) {
            descriptorScores.merge("Overgrown", 2, Integer::sum);
        }

        if (countBlocksMatching(furniture, "bamboo", "bamboo_block") >= 2) {
            descriptorScores.merge("Zen", 2, Integer::sum);
        }

        // Elements
        if (countBlocksMatching(furniture, "gravel", "sand") >= 2) {
            descriptorScores.merge("Dusty", 1, Integer::sum);
        }

        if (countBlocksMatching(furniture, "water_cauldron", "conduit", "prismarine") >= 2) {
            descriptorScores.merge("Watery", 1, Integer::sum);
        }

        if (countBlocksMatching(furniture, "ice", "blue_ice", "snow_block") >= 2) {
            descriptorScores.merge("Frozen", 1, Integer::sum);
        }

        if (countBlocksMatching(furniture, "magma_block", "netherrack", "lava") >= 1) {
            descriptorScores.merge("Blazing", 1, Integer::sum);
        }

        // Themed spaces
        if (countBlocksMatching(furniture, "prismarine", "sea_lantern", "sea_pickle") >= 2) {
            descriptorScores.merge("Maritime", 2, Integer::sum);
        }

        if (countBlocksMatching(signals, "enchanting_table", "brewing_stand", "ender_chest") >= 1) {
            descriptorScores.merge("Arcane", 2, Integer::sum);
        }

        if (countBlocksMatching(furniture, "soul_sand", "soul_soil", "ender_chest") >= 1) {
            descriptorScores.merge("Sacred", 2, Integer::sum);
        }

        if (countBlocksMatching(furniture, "redstone_lamp", "dispenser", "dropper", "piston", "repeater", "comparator") >= 1) {
            descriptorScores.merge("Mechanical", 2, Integer::sum);
        }

        // Storage and organization
        if (countBlocksMatching(furniture, "chest", "barrel", "shulker_box") >= 4) {
            descriptorScores.merge("Cluttered", 1, Integer::sum);
        }

        if (countBlocksMatching(furniture, "chest", "barrel", "shulker_box") == 0 && furniture.size() <= 3) {
            descriptorScores.merge("Minimal", 1, Integer::sum);
        }

        // Work and craft spaces
        if (countBlocksMatching(furniture, "barrel", "smoker", "grindstone", "fletching_table") >= 1) {
            descriptorScores.merge("Rustic", 1, Integer::sum);
        }

        // Living spaces
        if (countBlocksMatching(furniture, "composter", "potted_plant", "flower_pot") >= 2) {
            descriptorScores.merge("Verdant", 1, Integer::sum);
        }

        // Size-based descriptors
        int volume = room.getVolume();
        if (volume >= 400) {
            descriptorScores.merge("Vast", 1, Integer::sum);
        } else if (volume >= 200) {
            descriptorScores.merge("Grand", 1, Integer::sum);
        }

        if (volume <= 30) {
            descriptorScores.merge("Humble", 1, Integer::sum);
        } else if (volume <= 50) {
            descriptorScores.merge("Cozy", 1, Integer::sum);
        }

        // Ancient materials
        if (countBlocksMatching(surfaces, "ancient_debris", "deepslate") >= 2) {
            descriptorScores.merge("Ancient", 1, Integer::sum);
        }

        if (descriptorScores.isEmpty()) {
            return null;
        }

        int maxScore = descriptorScores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> winners = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : descriptorScores.entrySet()) {
            if (entry.getValue() == maxScore) {
                winners.add(entry.getKey());
            }
        }

        return winners.get(random.nextInt(winners.size()));
    }

    private static String detectBedColor(Map<String, Integer> furniture) {
        java.util.Map<String, String> colorPatterns = java.util.Map.ofEntries(
                java.util.Map.entry("red_bed", "Scarlet"),
                java.util.Map.entry("blue_bed", "Azure"),
                java.util.Map.entry("purple_bed", "Violet"),
                java.util.Map.entry("green_bed", "Jade"),
                java.util.Map.entry("pink_bed", "Rose"),
                java.util.Map.entry("orange_bed", "Amber"),
                java.util.Map.entry("yellow_bed", "Sunny"),
                java.util.Map.entry("cyan_bed", "Teal"),
                java.util.Map.entry("white_bed", "Ivory"),
                java.util.Map.entry("magenta_bed", "Magenta"),
                java.util.Map.entry("brown_bed", "Mahogany"),
                java.util.Map.entry("light_blue_bed", "Sky"),
                java.util.Map.entry("gray_bed", "Ash"),
                java.util.Map.entry("light_gray_bed", "Pearl"),
                java.util.Map.entry("black_bed", "Ebony")
        );

        int maxCount = 0;
        String bestColor = null;

        for (java.util.Map.Entry<String, String> colorEntry : colorPatterns.entrySet()) {
            int count = countBlocksMatching(furniture, colorEntry.getKey());
            if (count > maxCount) {
                maxCount = count;
                bestColor = colorEntry.getValue();
            }
        }

        return maxCount > 0 ? bestColor : null;
    }

    private static int countBlocksMatching(Map<String, Integer> blockCounts, String... patterns) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : blockCounts.entrySet()) {
            String key = entry.getKey().toLowerCase();
            for (String pattern : patterns) {
                if (key.contains(pattern.toLowerCase())) {
                    total += entry.getValue();
                    break;
                }
            }
        }
        return total;
    }

    private static boolean containsAny(String text, String... patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
