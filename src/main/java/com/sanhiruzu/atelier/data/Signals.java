package com.sanhiruzu.atelier.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

// Maps signal names used in room_profiles JSON (e.g. "bed", "cooking_block",
// "smithing_or_repair_block") to a BlockState predicate. ZoneEvaluator counts how many
// blocks adjacent to a room's INSIDE air match each signal so RoomProfile.requiredFeatures
// can be checked.
public class Signals {
    private static final Map<String, Predicate<BlockState>> PREDICATES = new HashMap<>();
    private static final TagKey<Block> BEDS = commonBlockTag("beds");
    private static final TagKey<Block> CHISELED_BOOKSHELVES = commonBlockTag("chiseled_bookshelves");
    private static final TagKey<Block> BREWING_STANDS = commonBlockTag("brewing_stands");
    private static final TagKey<Block> CARTOGRAPHY_TABLES = commonBlockTag("cartography_tables");
    private static final TagKey<Block> CAULDRONS = commonBlockTag("cauldrons");
    private static final TagKey<Block> COMPOSTERS = commonBlockTag("composters");
    private static final TagKey<Block> CAMPFIRES = commonBlockTag("campfires");
    private static final TagKey<Block> CRAFTING_TABLES = commonBlockTag("crafting_tables");
    private static final TagKey<Block> WORKBENCH = commonBlockTag("workbench");
    private static final TagKey<Block> FURNACES = commonBlockTag("furnaces");
    private static final TagKey<Block> SMOKERS = commonBlockTag("smokers");
    private static final TagKey<Block> ENCHANTING_TABLES = commonBlockTag("enchanting_tables");
    private static final TagKey<Block> FLETCHING_TABLES = commonBlockTag("fletching_tables");
    private static final TagKey<Block> LOOMS = commonBlockTag("looms");
    private static final TagKey<Block> CROPS = commonBlockTag("crops");
    private static final TagKey<Block> FLOWERS = commonBlockTag("flowers");
    private static final TagKey<Block> SAPLINGS = commonBlockTag("saplings");
    private static final TagKey<Block> LEAVES = commonBlockTag("leaves");
    private static final TagKey<Block> FROG_PLANTS = commonBlockTag("frog_plants");
    private static final TagKey<Block> GRASS_LIKE = commonBlockTag("grass_like");
    private static final TagKey<Block> MUSHROOMS = commonBlockTag("mushrooms");
    private static final TagKey<Block> ANVILS = commonBlockTag("anvils");
    private static final TagKey<Block> SMITHING_TABLES = commonBlockTag("smithing_tables");
    private static final TagKey<Block> GRINDSTONES = commonBlockTag("grindstones");
    private static final TagKey<Block> BLAST_FURNACES = commonBlockTag("blast_furnaces");
    private static final TagKey<Block> STONECUTTERS = commonBlockTag("stonecutters");
    private static final TagKey<Block> GLASS_SLABS = commonBlockTag("glass_slabs");
    private static final TagKey<Block> GLASS_STAIRS = commonBlockTag("glass_stairs");
    private static final TagKey<Block> IRON_BARS = commonBlockTag("iron_bars");
    private static final TagKey<Block> LANTERNS = commonBlockTag("lanterns");
    private static final TagKey<Block> MACHINES = commonBlockTag("machines");
    private static final TagKey<Block> PIPES = commonBlockTag("pipes");
    private static final Set<ResourceLocation> MINECOLONIES_RESIDENCE_BLOCKS = minecoloniesBlocks(
            "blockhutcitizen", "blockhuttavern"
    );
    private static final Set<ResourceLocation> MINECOLONIES_STORAGE_BLOCKS = minecoloniesBlocks(
            "blockhutwarehouse", "blockhutdeliveryman", "blockminecoloniesrack", "blockpostbox",
            "blockstash", "barrel_block"
    );
    private static final Set<ResourceLocation> MINECOLONIES_COOKING_BLOCKS = minecoloniesBlocks(
            "blockhutcook", "blockhutkitchen", "blockhutbaker"
    );
    private static final Set<ResourceLocation> MINECOLONIES_LIBRARY_BLOCKS = minecoloniesBlocks(
            "blockhutlibrary", "blockhutuniversity", "blockhutschool"
    );
    private static final Set<ResourceLocation> MINECOLONIES_SMITHING_BLOCKS = minecoloniesBlocks(
            "blockhutblacksmith", "blockhutsmeltery", "blockhutstonesmeltery", "blockhutcrusher",
            "blockhutmechanic"
    );
    private static final Set<ResourceLocation> MINECOLONIES_MASONRY_BLOCKS = minecoloniesBlocks(
            "blockhutstonemason"
    );
    private static final Set<ResourceLocation> MINECOLONIES_FLETCHING_BLOCKS = minecoloniesBlocks(
            "blockhutfletcher", "blockhutarchery"
    );
    private static final Set<ResourceLocation> MINECOLONIES_GARDEN_BLOCKS = minecoloniesBlocks(
            "blockhutcomposter", "blockhutflorist", "blockhutbeekeeper"
    );
    private static final Set<ResourceLocation> MINECOLONIES_FARM_BLOCKS = minecoloniesBlocks(
            "blockhutfarmer", "blockhutplantation", "blockhutcowboy", "blockhutshepherd",
            "blockhutswineherder", "blockhutchickenherder", "blockhutrabbithutch", "blockhutstable",
            "blockhutfisherman", "blockhutfield", "blockhutplantationfield", "farmland", "floodedfarmland",
            "bell_pepper", "cabbage", "chickpea", "durum", "eggplant", "garlic", "onion",
            "soybean", "tomato", "rice", "butternut_squash", "corn", "mint", "nether_pepper",
            "peas"
    );
    private static final Set<ResourceLocation> MINECOLONIES_ARCANE_BLOCKS = minecoloniesBlocks(
            "blockhutenchanter", "blockhutmysticalsite", "blockhutalchemist"
    );
    private static final Set<ResourceLocation> MINECOLONIES_WORKSHOP_BLOCKS = minecoloniesBlocks(
            "blockhutbuilder", "blockhutlumberjack", "blockhutsawmill", "blockhutminer",
            "simplequarry", "mediumquarry", "blockhutsifter", "blockhutglassblower",
            "blockhutdyer", "blockhutconcretemixer", "blockhutnetherworker"
    );
    private static final Set<ResourceLocation> MINECOLONIES_GUARD_BLOCKS = minecoloniesBlocks(
            "blockhutguardtower", "blockhutbarracks", "blockhutbarrackstower",
            "blockhutcombatacademy", "blockhutgatehouse"
    );
    private static final Set<ResourceLocation> MINECOLONIES_TOWN_HALL_BLOCKS = minecoloniesBlocks(
            "blockhuttownhall"
    );
    private static final Set<ResourceLocation> MINECOLONIES_CIVIC_BLOCKS = minecoloniesBlocks(
            "blockhuthospital", "blockhutgraveyard", "colonysign"
    );
    private static final Set<ResourceLocation> MINECOLONIES_FACTORY_BLOCKS = merge(
            MINECOLONIES_SMITHING_BLOCKS,
            minecoloniesBlocks("blockhutconcretemixer", "blockhutmechanic", "blockhutcrusher")
    );

    // Zone-type signature block tags — backed by data/zen_atelier/tags/block/*.json
    private static final TagKey<Block> SYNTHESIS_STATION_BLOCKS = atelierBlockTag("synthesis_station");
    private static final TagKey<Block> CREATE_ROTATIONAL       = atelierBlockTag("create_rotational");
    private static final TagKey<Block> MALUM_SOUL              = atelierBlockTag("malum_soul_block");
    private static final TagKey<Block> TERRARIUM_BLOCK         = atelierBlockTag("terrarium_block");
    private static final TagKey<Block> TELESCOPE               = atelierBlockTag("telescope");
    private static final TagKey<Block> SPECTRUM_CRYSTAL        = atelierBlockTag("spectrum_crystal");
    private static final TagKey<Block> SWEM_TACK               = atelierBlockTag("swem_tack");
    private static final TagKey<Block> FISH_TANK               = atelierBlockTag("fish_tank");
    private static final TagKey<Block> CANDLELIGHT_TABLE       = atelierBlockTag("candlelight_table");
    private static final TagKey<Block> SEATING                 = atelierBlockTag("seating");
    private static final TagKey<Block> VINERY_BARREL           = atelierBlockTag("vinery_barrel");

    static {
        PREDICATES.put("bed", s -> s.is(BEDS) || s.is(BlockTags.BEDS) || isMineColoniesResidence(s));
        PREDICATES.put("bookshelf", s ->
                s.is(Tags.Blocks.BOOKSHELVES) || s.is(CHISELED_BOOKSHELVES)
                        || s.is(Blocks.BOOKSHELF) || s.is(Blocks.CHISELED_BOOKSHELF)
                        || isMineColoniesLibrary(s));
        PREDICATES.put("brewing_stand", s -> s.is(BREWING_STANDS) || s.is(Blocks.BREWING_STAND));
        PREDICATES.put("cartography_table", s -> s.is(CARTOGRAPHY_TABLES) || s.is(Blocks.CARTOGRAPHY_TABLE));
        PREDICATES.put("cauldron", s ->
                s.is(CAULDRONS)
                        || s.is(Blocks.CAULDRON) || s.is(Blocks.WATER_CAULDRON)
                        || s.is(Blocks.LAVA_CAULDRON) || s.is(Blocks.POWDER_SNOW_CAULDRON));
        PREDICATES.put("composter", s -> s.is(COMPOSTERS) || s.is(Blocks.COMPOSTER) || isAny(s, MINECOLONIES_GARDEN_BLOCKS));
        PREDICATES.put("cooking_block", s ->
                s.is(Tags.Blocks.PLAYER_WORKSTATIONS_FURNACES)
                        || s.is(FURNACES) || s.is(SMOKERS) || s.is(CAMPFIRES)
                        || s.is(Blocks.FURNACE) || s.is(Blocks.SMOKER)
                        || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE)
                        || isAny(s, MINECOLONIES_COOKING_BLOCKS));
        PREDICATES.put("crafting_table", s ->
                s.is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)
                        || s.is(CRAFTING_TABLES) || s.is(WORKBENCH) || s.is(Blocks.CRAFTING_TABLE)
                        || isAny(s, MINECOLONIES_WORKSHOP_BLOCKS));
        PREDICATES.put("enchanting_table", s -> s.is(ENCHANTING_TABLES) || s.is(Blocks.ENCHANTING_TABLE) || isAny(s, MINECOLONIES_ARCANE_BLOCKS));
        PREDICATES.put("fletching_table", s -> s.is(FLETCHING_TABLES) || s.is(Blocks.FLETCHING_TABLE) || isAny(s, MINECOLONIES_FLETCHING_BLOCKS));
        PREDICATES.put("glass", s ->
                s.is(Tags.Blocks.GLASS_BLOCKS) || s.is(Tags.Blocks.GLASS_PANES));
        PREDICATES.put("loom", s -> s.is(LOOMS) || s.is(Blocks.LOOM));
        PREDICATES.put("plant", s ->
                s.is(CROPS) || s.is(FLOWERS) || s.is(SAPLINGS) || s.is(LEAVES)
                        || s.is(GRASS_LIKE) || s.is(MUSHROOMS)
                        || s.is(BlockTags.CROPS) || s.is(BlockTags.FLOWERS)
                        || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.LEAVES)
                        || isAny(s, MINECOLONIES_FARM_BLOCKS) || isAny(s, MINECOLONIES_GARDEN_BLOCKS));
        PREDICATES.put("frog_plant", s ->
                s.is(FROG_PLANTS)
                        || s.is(Blocks.SMALL_DRIPLEAF) || s.is(Blocks.BIG_DRIPLEAF)
                        || s.is(Blocks.LILY_PAD));
        PREDICATES.put("smithing_or_repair_block", s ->
                s.is(ANVILS) || s.is(SMITHING_TABLES) || s.is(GRINDSTONES) || s.is(BLAST_FURNACES)
                        || s.is(Blocks.ANVIL) || s.is(Blocks.CHIPPED_ANVIL) || s.is(Blocks.DAMAGED_ANVIL)
                        || s.is(Blocks.SMITHING_TABLE) || s.is(Blocks.GRINDSTONE) || s.is(Blocks.BLAST_FURNACE)
                        || isAny(s, MINECOLONIES_SMITHING_BLOCKS));
        PREDICATES.put("stonecutter", s -> s.is(STONECUTTERS) || s.is(Blocks.STONECUTTER) || isAny(s, MINECOLONIES_MASONRY_BLOCKS));
        PREDICATES.put("storage", s ->
                s.is(Tags.Blocks.CHESTS) || s.is(Tags.Blocks.BARRELS) || isAny(s, MINECOLONIES_STORAGE_BLOCKS));
        PREDICATES.put("villager_workstation", s -> s.is(Tags.Blocks.VILLAGER_JOB_SITES));
        PREDICATES.put("tool_blocks", Signals::isToolBlock);
        PREDICATES.put("stone_or_metal_materials", Signals::isStoneOrMetalMaterial);
        PREDICATES.put("urban_block", Signals::isUrbanBlock);
        PREDICATES.put("factory_block", Signals::isFactoryBlock);
        PREDICATES.put("industrial_blocks", s -> isFactoryBlock(s) || isUrbanBlock(s));
        PREDICATES.put("water_coverage", s ->
                s.getFluidState().is(FluidTags.WATER) && s.getFluidState().isSource());
        PREDICATES.put("carpet", s -> s.is(BlockTags.WOOL_CARPETS));
        PREDICATES.put("wool", s -> s.is(BlockTags.WOOL));
        PREDICATES.put("lectern", s -> s.is(Blocks.LECTERN));
        PREDICATES.put("flower_pots", s -> s.getBlock() instanceof net.minecraft.world.level.block.FlowerPotBlock);
        PREDICATES.put("wood_materials", s ->
                s.is(BlockTags.PLANKS) || s.is(BlockTags.LOGS)
                        || s.is(BlockTags.WOODEN_SLABS) || s.is(BlockTags.WOODEN_STAIRS));
        PREDICATES.put("minecolonies_town_hall", s -> isAny(s, MINECOLONIES_TOWN_HALL_BLOCKS));
        PREDICATES.put("minecolonies_residence", Signals::isMineColoniesResidence);
        PREDICATES.put("minecolonies_warehouse", s -> isAny(s, MINECOLONIES_STORAGE_BLOCKS));
        PREDICATES.put("minecolonies_kitchen", s -> isAny(s, MINECOLONIES_COOKING_BLOCKS));
        PREDICATES.put("minecolonies_library", Signals::isMineColoniesLibrary);
        PREDICATES.put("minecolonies_smithy", s -> isAny(s, MINECOLONIES_SMITHING_BLOCKS));
        PREDICATES.put("minecolonies_masonry", s -> isAny(s, MINECOLONIES_MASONRY_BLOCKS));
        PREDICATES.put("minecolonies_fletchery", s -> isAny(s, MINECOLONIES_FLETCHING_BLOCKS));
        PREDICATES.put("minecolonies_garden", s -> isAny(s, MINECOLONIES_GARDEN_BLOCKS));
        PREDICATES.put("minecolonies_farm", s -> isAny(s, MINECOLONIES_FARM_BLOCKS));
        PREDICATES.put("minecolonies_arcane", s -> isAny(s, MINECOLONIES_ARCANE_BLOCKS));
        PREDICATES.put("minecolonies_workshop", s -> isAny(s, MINECOLONIES_WORKSHOP_BLOCKS));
        PREDICATES.put("minecolonies_guard", s -> isAny(s, MINECOLONIES_GUARD_BLOCKS));
        PREDICATES.put("minecolonies_civic", s -> isAny(s, MINECOLONIES_CIVIC_BLOCKS));

        // Zone-type signature signals — tag-backed, extend via zen_atelier/tags/block/*.json
        PREDICATES.put("synthesis_station", s -> s.is(SYNTHESIS_STATION_BLOCKS));
        PREDICATES.put("hay_bale",          s -> s.is(Blocks.HAY_BLOCK));
        PREDICATES.put("candle",            s -> s.is(BlockTags.CANDLES));
        PREDICATES.put("create_rotational", s -> s.is(CREATE_ROTATIONAL));
        PREDICATES.put("malum_soul_block",  s -> s.is(MALUM_SOUL));
        PREDICATES.put("terrarium_block",   s -> s.is(TERRARIUM_BLOCK));
        PREDICATES.put("telescope",         s -> s.is(TELESCOPE));
        PREDICATES.put("spectrum_crystal",  s -> s.is(SPECTRUM_CRYSTAL));
        PREDICATES.put("swem_tack",         s -> s.is(SWEM_TACK));
        PREDICATES.put("fish_tank",         s -> s.is(FISH_TANK));
        PREDICATES.put("candlelight_table", s -> s.is(CANDLELIGHT_TABLE));
        PREDICATES.put("seating",           s -> s.is(SEATING));
        PREDICATES.put("vinery_barrel",     s -> s.is(VINERY_BARREL));
    }

    public static void register(String signal, Predicate<BlockState> predicate) {
        PREDICATES.put(signal, predicate);
    }

    public static boolean matches(String signal, BlockState state) {
        Predicate<BlockState> p = PREDICATES.get(signal);
        return p != null && p.test(state);
    }

    public static Map<String, Predicate<BlockState>> predicates() {
        return PREDICATES;
    }

    private static TagKey<Block> commonBlockTag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static TagKey<Block> atelierBlockTag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("zen_atelier", path));
    }

    private static Set<ResourceLocation> minecoloniesBlocks(String... paths) {
        Set<ResourceLocation> ids = new java.util.HashSet<>();
        for (String path : paths) {
            ids.add(ResourceLocation.fromNamespaceAndPath("minecolonies", path));
        }
        return Set.copyOf(ids);
    }

    @SafeVarargs
    private static Set<ResourceLocation> merge(Set<ResourceLocation>... sets) {
        Set<ResourceLocation> ids = new java.util.HashSet<>();
        for (Set<ResourceLocation> set : sets) {
            ids.addAll(set);
        }
        return Set.copyOf(ids);
    }

    private static boolean isAny(BlockState state, Set<ResourceLocation> ids) {
        return ids.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static boolean isMineColoniesResidence(BlockState state) {
        return isAny(state, MINECOLONIES_RESIDENCE_BLOCKS);
    }

    private static boolean isMineColoniesLibrary(BlockState state) {
        return isAny(state, MINECOLONIES_LIBRARY_BLOCKS);
    }

    private static boolean isToolBlock(BlockState state) {
        return state.is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)
                || state.is(Tags.Blocks.PLAYER_WORKSTATIONS_FURNACES)
                || state.is(Tags.Blocks.VILLAGER_JOB_SITES)
                || state.is(CRAFTING_TABLES) || state.is(WORKBENCH)
                || state.is(FURNACES) || state.is(SMOKERS)
                || state.is(STONECUTTERS) || state.is(GRINDSTONES)
                || state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.STONECUTTER)
                || state.is(Blocks.GRINDSTONE)
                || isAny(state, MINECOLONIES_WORKSHOP_BLOCKS);
    }

    private static boolean isStoneOrMetalMaterial(BlockState state) {
        return state.is(Tags.Blocks.STONES)
                || state.is(Tags.Blocks.COBBLESTONES)
                || state.is(Tags.Blocks.CHAINS)
                || state.is(Tags.Blocks.OBSIDIANS)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_IRON)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_COPPER)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_GOLD)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_NETHERITE)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_RAW_IRON)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD)
                || state.is(IRON_BARS)
                || state.is(Blocks.IRON_BARS)
                || isAny(state, MINECOLONIES_MASONRY_BLOCKS)
                || isAny(state, MINECOLONIES_SMITHING_BLOCKS);
    }

    private static boolean isUrbanBlock(BlockState state) {
        return state.is(Tags.Blocks.CONCRETES)
                || state.is(Tags.Blocks.GLAZED_TERRACOTTAS)
                || state.is(Tags.Blocks.GLASS_BLOCKS)
                || state.is(Tags.Blocks.GLASS_PANES)
                || state.is(GLASS_SLABS)
                || state.is(GLASS_STAIRS)
                || state.is(Tags.Blocks.CHAINS)
                || state.is(IRON_BARS)
                || state.is(LANTERNS)
                || state.is(Blocks.IRON_BARS)
                || state.is(Blocks.LANTERN)
                || state.is(Blocks.SOUL_LANTERN)
                || isAny(state, MINECOLONIES_GUARD_BLOCKS)
                || isAny(state, MINECOLONIES_TOWN_HALL_BLOCKS);
    }

    private static boolean isFactoryBlock(BlockState state) {
        return state.is(MACHINES)
                || state.is(PIPES)
                || state.is(Tags.Blocks.CHAINS)
                || state.is(Tags.Blocks.PLAYER_WORKSTATIONS_FURNACES)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_IRON)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_COPPER)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_RAW_IRON)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_REDSTONE)
                || state.is(Tags.Blocks.ORES_IRON)
                || state.is(Tags.Blocks.ORES_COPPER)
                || state.is(Tags.Blocks.ORES_REDSTONE)
                || state.is(FURNACES)
                || state.is(SMOKERS)
                || state.is(BLAST_FURNACES)
                || state.is(Blocks.FURNACE)
                || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER)
                || state.is(Blocks.HOPPER)
                || state.is(Blocks.DISPENSER)
                || state.is(Blocks.DROPPER)
                || state.is(Blocks.PISTON)
                || state.is(Blocks.STICKY_PISTON)
                || state.is(Blocks.OBSERVER)
                || isAny(state, MINECOLONIES_FACTORY_BLOCKS);
    }
}
