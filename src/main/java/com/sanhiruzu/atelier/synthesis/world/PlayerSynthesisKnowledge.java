package com.sanhiruzu.atelier.synthesis.world;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlayerSynthesisKnowledge {
    private static final String CRAFTED_RECIPES_KEY = "zen_atelier.crafted_synthesis_recipes";

    private PlayerSynthesisKnowledge() {
    }

    public static boolean hasCrafted(Player player, String profileId) {
        return craftedIds(player).contains(profileId);
    }

    public static void markCrafted(Player player, String profileId) {
        if (hasCrafted(player, profileId)) {
            return;
        }
        ListTag tag = player.getPersistentData().getList(CRAFTED_RECIPES_KEY, Tag.TAG_STRING);
        tag.add(StringTag.valueOf(profileId));
        player.getPersistentData().put(CRAFTED_RECIPES_KEY, tag);
    }

    public static Set<String> craftedIds(Player player) {
        Set<String> ids = new HashSet<>();
        ListTag tag = player.getPersistentData().getList(CRAFTED_RECIPES_KEY, Tag.TAG_STRING);
        for (int i = 0; i < tag.size(); i++) {
            ids.add(tag.getString(i));
        }
        return ids;
    }

    public static int maskFor(Player player, List<String> profileIds, int segment) {
        Set<String> crafted = craftedIds(player);
        int mask = 0;
        int start = Math.max(0, segment) * Integer.SIZE;
        int end = Math.min(profileIds.size(), start + Integer.SIZE);
        for (int i = start; i < end; i++) {
            if (crafted.contains(profileIds.get(i))) {
                mask |= 1 << (i - start);
            }
        }
        return mask;
    }

    public static void copy(Player original, Player target) {
        if (original.getPersistentData().contains(CRAFTED_RECIPES_KEY)) {
            target.getPersistentData().put(
                    CRAFTED_RECIPES_KEY,
                    original.getPersistentData().getList(CRAFTED_RECIPES_KEY, Tag.TAG_STRING).copy()
            );
        }
    }
}
