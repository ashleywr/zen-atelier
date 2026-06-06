package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.ui.network.ExtractionKnowledgeSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PlayerExtractionKnowledge {
    private static final String KNOWN_SOURCES_KEY = "zen_atelier.extraction_known_sources";
    private static final String TESTED_EMPTY_KEY = "zen_atelier.extraction_tested_empty_sources";
    private static final String ATTEMPTS_KEY = "attempts";
    private static final String REAGENTS_KEY = "reagents";
    private static final String TRAITS_KEY = "traits";
    private static final String ELEMENTS_KEY = "elements";

    private PlayerExtractionKnowledge() {
    }

    public static void recordSuccess(ServerPlayer player, ItemSourceSnapshot source, Collection<ReagentStack> reagents) {
        if (reagents.isEmpty()) {
            recordTestedEmpty(player, source);
            return;
        }

        CompoundTag known = player.getPersistentData().getCompound(KNOWN_SOURCES_KEY);
        CompoundTag entry = known.getCompound(source.itemId());
        entry.putInt(ATTEMPTS_KEY, entry.getInt(ATTEMPTS_KEY) + 1);

        Set<String> reagentIds = readStringSet(entry, REAGENTS_KEY);
        Set<String> traits = readStringSet(entry, TRAITS_KEY);
        CompoundTag elements = entry.getCompound(ELEMENTS_KEY);

        for (ReagentStack reagent : reagents) {
            reagentIds.add(reagent.reagentId());
            traits.addAll(reagent.traits());
            for (Map.Entry<String, Integer> element : reagent.elements().entrySet()) {
                int current = elements.getInt(element.getKey());
                if (element.getValue() > current) {
                    elements.putInt(element.getKey(), element.getValue());
                }
            }
        }

        entry.put(REAGENTS_KEY, writeStringList(reagentIds));
        entry.put(TRAITS_KEY, writeStringList(traits));
        entry.put(ELEMENTS_KEY, elements);
        known.put(source.itemId(), entry);
        player.getPersistentData().put(KNOWN_SOURCES_KEY, known);
        removeTestedEmpty(player, source.itemId());
        sync(player);
        player.displayClientMessage(Component.translatable(
                "message.zen_atelier.extraction.codex_updated",
                readableId(source.itemId())
        ).withStyle(ChatFormatting.DARK_AQUA), true);
    }

    public static void recordTestedEmpty(ServerPlayer player, ItemSourceSnapshot source) {
        if (getKnownSourceReagents(player).containsKey(source.itemId())) {
            return;
        }

        Set<String> tested = getTestedEmptySources(player);
        if (tested.add(source.itemId())) {
            player.getPersistentData().put(TESTED_EMPTY_KEY, writeStringList(tested));
            sync(player);
        }
    }

    public static Map<String, List<String>> getKnownSourceReagents(Player player) {
        Map<String, List<String>> knownSources = new LinkedHashMap<>();
        CompoundTag known = player.getPersistentData().getCompound(KNOWN_SOURCES_KEY);
        known.getAllKeys().stream()
                .sorted()
                .forEach(sourceId -> {
                    List<String> reagents = readStringSet(known.getCompound(sourceId), REAGENTS_KEY).stream()
                            .sorted()
                            .toList();
                    if (!reagents.isEmpty()) {
                        knownSources.put(sourceId, reagents);
                    }
                });
        return knownSources;
    }

    public static Map<String, ExtractionKnowledgeSyncPayload.SourceKnowledge> getKnownSourceDetails(Player player) {
        Map<String, ExtractionKnowledgeSyncPayload.SourceKnowledge> knownSources = new LinkedHashMap<>();
        CompoundTag known = player.getPersistentData().getCompound(KNOWN_SOURCES_KEY);
        known.getAllKeys().stream()
                .sorted()
                .forEach(sourceId -> {
                    CompoundTag entry = known.getCompound(sourceId);
                    List<String> reagents = readStringSet(entry, REAGENTS_KEY).stream().sorted().toList();
                    if (reagents.isEmpty()) {
                        return;
                    }
                    List<String> traits = readStringSet(entry, TRAITS_KEY).stream().sorted().toList();
                    Map<String, Integer> elements = new LinkedHashMap<>();
                    CompoundTag elementTag = entry.getCompound(ELEMENTS_KEY);
                    elementTag.getAllKeys().stream()
                            .sorted()
                            .forEach(element -> elements.put(element, elementTag.getInt(element)));
                    knownSources.put(sourceId, new ExtractionKnowledgeSyncPayload.SourceKnowledge(
                            entry.getInt(ATTEMPTS_KEY),
                            reagents,
                            traits,
                            elements
                    ));
                });
        return knownSources;
    }

    public static Set<String> getTestedEmptySources(Player player) {
        return readStringSet(player.getPersistentData(), TESTED_EMPTY_KEY);
    }

    public static int knownSourceCount(Player player) {
        return getKnownSourceReagents(player).size();
    }

    public static void copy(Player original, Player target) {
        if (original.getPersistentData().contains(KNOWN_SOURCES_KEY)) {
            target.getPersistentData().put(KNOWN_SOURCES_KEY, original.getPersistentData().getCompound(KNOWN_SOURCES_KEY).copy());
        }
        if (original.getPersistentData().contains(TESTED_EMPTY_KEY)) {
            target.getPersistentData().put(TESTED_EMPTY_KEY, original.getPersistentData().getList(TESTED_EMPTY_KEY, Tag.TAG_STRING).copy());
        }
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ExtractionKnowledgeSyncPayload(
                getKnownSourceReagents(player),
                getTestedEmptySources(player),
                getKnownSourceDetails(player)
        ));
    }

    public static List<Component> codexSummary(Player player, int limit) {
        Map<String, List<String>> known = getKnownSourceReagents(player);
        if (known.isEmpty()) {
            return List.of(Component.translatable("message.zen_atelier.codex.no_sources")
                    .withStyle(ChatFormatting.GRAY));
        }

        return known.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .limit(limit)
                .map(entry -> (Component) Component.literal(readableId(entry.getKey()) + " -> " + formatIds(entry.getValue()))
                        .withStyle(ChatFormatting.GRAY))
                .toList();
    }

    public static String readableId(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] parts = path.replace('_', ' ').split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isBlank()) {
                parts[i] = parts[i].substring(0, 1).toUpperCase(Locale.ROOT) + parts[i].substring(1);
            }
        }
        return String.join(" ", parts);
    }

    public static String formatIds(Collection<String> ids) {
        return ids.stream()
                .map(PlayerExtractionKnowledge::readableId)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static void removeTestedEmpty(Player player, String sourceId) {
        Set<String> tested = getTestedEmptySources(player);
        if (tested.remove(sourceId)) {
            player.getPersistentData().put(TESTED_EMPTY_KEY, writeStringList(tested));
        }
    }

    private static Set<String> readStringSet(CompoundTag root, String key) {
        Set<String> values = new LinkedHashSet<>();
        ListTag tag = root.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < tag.size(); i++) {
            values.add(tag.getString(i));
        }
        return values;
    }

    private static ListTag writeStringList(Collection<String> values) {
        ListTag tag = new ListTag();
        values.stream().sorted().forEach(value -> tag.add(StringTag.valueOf(value)));
        return tag;
    }
}
