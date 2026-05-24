package com.sanhiruzu.atelier.zone.discovery;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PlayerRoomDiscovery {
    private static final String DISCOVERED_ROOM_TYPES_KEY = "zen_atelier.discovered_room_types";
    private static final String BEST_SCORES_KEY = "zen_atelier.best_room_scores";

    private PlayerRoomDiscovery() {
    }

    public static boolean hasDiscovered(Player player, String profileId) {
        ListTag tag = player.getPersistentData().getList(DISCOVERED_ROOM_TYPES_KEY, Tag.TAG_STRING);
        for (int i = 0; i < tag.size(); i++) {
            if (tag.getString(i).equals(profileId)) {
                return true;
            }
        }
        return false;
    }

    public static void markDiscovered(Player player, String profileId) {
        if (hasDiscovered(player, profileId)) {
            return;
        }
        ListTag tag = player.getPersistentData().getList(DISCOVERED_ROOM_TYPES_KEY, Tag.TAG_STRING);
        tag.add(net.minecraft.nbt.StringTag.valueOf(profileId));
        player.getPersistentData().put(DISCOVERED_ROOM_TYPES_KEY, tag);
    }

    public static int getDiscoveredCount(Player player) {
        return player.getPersistentData().getList(DISCOVERED_ROOM_TYPES_KEY, Tag.TAG_STRING).size();
    }

    public static Set<String> getDiscoveredIds(Player player) {
        Set<String> ids = new HashSet<>();
        ListTag tag = player.getPersistentData().getList(DISCOVERED_ROOM_TYPES_KEY, Tag.TAG_STRING);
        for (int i = 0; i < tag.size(); i++) {
            ids.add(tag.getString(i));
        }
        return ids;
    }

    public static void updateBestScore(Player player, String profileId, int score) {
        CompoundTag scores = player.getPersistentData().getCompound(BEST_SCORES_KEY);
        int current = scores.getInt(profileId);
        if (score > current) {
            scores.putInt(profileId, score);
            player.getPersistentData().put(BEST_SCORES_KEY, scores);
        }
    }

    public static int getBestScore(Player player, String profileId) {
        return player.getPersistentData().getCompound(BEST_SCORES_KEY).getInt(profileId);
    }

    public static Map<String, Integer> getAllBestScores(Player player) {
        Map<String, Integer> scores = new HashMap<>();
        CompoundTag tag = player.getPersistentData().getCompound(BEST_SCORES_KEY);
        for (String key : tag.getAllKeys()) {
            scores.put(key, tag.getInt(key));
        }
        return scores;
    }
}
