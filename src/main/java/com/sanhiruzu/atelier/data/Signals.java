package com.sanhiruzu.atelier.data;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

// Maps signal names used in room_profiles JSON (e.g. "bed", "cooking_block",
// "smithing_or_repair_block") to a BlockState predicate. ZoneEvaluator counts how many
// blocks adjacent to a room's INSIDE air match each signal so RoomProfile.requiredFeatures
// can be checked.
public class Signals {
    private static final Map<String, Predicate<BlockState>> PREDICATES = new HashMap<>();

    static {
        PREDICATES.put("bed", s -> s.is(BlockTags.BEDS));
        PREDICATES.put("bookshelf", s -> s.is(Blocks.BOOKSHELF) || s.is(Blocks.CHISELED_BOOKSHELF));
        PREDICATES.put("brewing_stand", s -> s.is(Blocks.BREWING_STAND));
        PREDICATES.put("cartography_table", s -> s.is(Blocks.CARTOGRAPHY_TABLE));
        PREDICATES.put("cauldron", s ->
                s.is(Blocks.CAULDRON) || s.is(Blocks.WATER_CAULDRON)
                        || s.is(Blocks.LAVA_CAULDRON) || s.is(Blocks.POWDER_SNOW_CAULDRON));
        PREDICATES.put("composter", s -> s.is(Blocks.COMPOSTER));
        PREDICATES.put("cooking_block", s ->
                s.is(Blocks.FURNACE) || s.is(Blocks.SMOKER)
                        || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE));
        PREDICATES.put("crafting_table", s -> s.is(Blocks.CRAFTING_TABLE));
        PREDICATES.put("enchanting_table", s -> s.is(Blocks.ENCHANTING_TABLE));
        PREDICATES.put("fletching_table", s -> s.is(Blocks.FLETCHING_TABLE));
        PREDICATES.put("glass", s ->
                s.is(Blocks.GLASS) || s.is(Blocks.TINTED_GLASS) || s.is(Blocks.GLASS_PANE)
                        || s.getBlock() instanceof StainedGlassBlock
                        || s.getBlock() instanceof StainedGlassPaneBlock);
        PREDICATES.put("loom", s -> s.is(Blocks.LOOM));
        PREDICATES.put("plant", s ->
                s.is(BlockTags.CROPS) || s.is(BlockTags.FLOWERS)
                        || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.LEAVES));
        PREDICATES.put("smithing_or_repair_block", s ->
                s.is(Blocks.ANVIL) || s.is(Blocks.CHIPPED_ANVIL) || s.is(Blocks.DAMAGED_ANVIL)
                        || s.is(Blocks.SMITHING_TABLE) || s.is(Blocks.GRINDSTONE)
                        || s.is(Blocks.BLAST_FURNACE));
        PREDICATES.put("stonecutter", s -> s.is(Blocks.STONECUTTER));
        PREDICATES.put("storage", s ->
                s.is(Blocks.CHEST) || s.is(Blocks.TRAPPED_CHEST) || s.is(Blocks.BARREL));
        PREDICATES.put("villager_workstation", s ->
                s.is(Blocks.BARREL) || s.is(Blocks.BLAST_FURNACE) || s.is(Blocks.BREWING_STAND)
                        || s.is(Blocks.CARTOGRAPHY_TABLE) || s.is(Blocks.CAULDRON)
                        || s.is(Blocks.COMPOSTER) || s.is(Blocks.FLETCHING_TABLE)
                        || s.is(Blocks.GRINDSTONE) || s.is(Blocks.LECTERN) || s.is(Blocks.LOOM)
                        || s.is(Blocks.SMITHING_TABLE) || s.is(Blocks.SMOKER) || s.is(Blocks.STONECUTTER));
    }

    public static boolean matches(String signal, BlockState state) {
        Predicate<BlockState> p = PREDICATES.get(signal);
        return p != null && p.test(state);
    }

    public static Map<String, Predicate<BlockState>> predicates() {
        return PREDICATES;
    }
}
