package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.ui.network.ExtractionKnowledgeSyncPayload;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClientExtractionKnowledgeData {
    private static final Map<String, List<String>> KNOWN_SOURCE_REAGENTS = new HashMap<>();
    private static final Map<String, ExtractionKnowledgeSyncPayload.SourceKnowledge> KNOWN_SOURCE_DETAILS = new HashMap<>();
    private static final Set<String> TESTED_EMPTY_SOURCES = new HashSet<>();
    private static final Set<String> PINNED_REAGENT_GOAL = new LinkedHashSet<>();

    private ClientExtractionKnowledgeData() {
    }

    public static void update(Map<String, List<String>> knownSourceReagents, Set<String> testedEmptySources) {
        update(knownSourceReagents, testedEmptySources, Map.of());
    }

    public static void update(
            Map<String, List<String>> knownSourceReagents,
            Set<String> testedEmptySources,
            Map<String, ExtractionKnowledgeSyncPayload.SourceKnowledge> knownSourceDetails
    ) {
        KNOWN_SOURCE_REAGENTS.clear();
        knownSourceReagents.forEach((sourceId, reagents) -> KNOWN_SOURCE_REAGENTS.put(sourceId, List.copyOf(reagents)));
        KNOWN_SOURCE_DETAILS.clear();
        knownSourceReagents.forEach((sourceId, reagents) ->
                KNOWN_SOURCE_DETAILS.put(sourceId, knownSourceDetails.getOrDefault(
                        sourceId,
                        new ExtractionKnowledgeSyncPayload.SourceKnowledge(0, reagents, List.of(), Map.of())
                )));
        TESTED_EMPTY_SOURCES.clear();
        TESTED_EMPTY_SOURCES.addAll(testedEmptySources);
    }

    public static List<String> knownReagents(String sourceId) {
        return KNOWN_SOURCE_REAGENTS.getOrDefault(sourceId, List.of());
    }

    public static Map<String, List<String>> allKnownSourceReagents() {
        return Map.copyOf(KNOWN_SOURCE_REAGENTS);
    }

    public static ExtractionKnowledgeSyncPayload.SourceKnowledge knownSourceDetails(String sourceId) {
        return KNOWN_SOURCE_DETAILS.getOrDefault(
                sourceId,
                new ExtractionKnowledgeSyncPayload.SourceKnowledge(0, knownReagents(sourceId), List.of(), Map.of())
        );
    }

    public static boolean hasKnownSource(String sourceId) {
        return KNOWN_SOURCE_REAGENTS.containsKey(sourceId);
    }

    public static List<String> knownSourcesForReagent(String reagentId) {
        return knownSourcesForAny(List.of(reagentId));
    }

    public static List<String> knownSourcesForAny(Collection<String> reagentIds) {
        Set<String> wanted = Set.copyOf(reagentIds);
        if (wanted.isEmpty()) {
            return List.of();
        }
        return KNOWN_SOURCE_REAGENTS.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(wanted::contains))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    public static boolean isTestedEmpty(String sourceId) {
        return TESTED_EMPTY_SOURCES.contains(sourceId);
    }

    public static Set<String> allTestedEmptySources() {
        return Set.copyOf(TESTED_EMPTY_SOURCES);
    }

    public static void pinReagentGoal(Collection<String> reagentIds) {
        PINNED_REAGENT_GOAL.clear();
        reagentIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .sorted()
                .forEach(PINNED_REAGENT_GOAL::add);
    }

    public static void clearPinnedReagentGoal() {
        PINNED_REAGENT_GOAL.clear();
    }

    public static boolean hasPinnedReagentGoal() {
        return !PINNED_REAGENT_GOAL.isEmpty();
    }

    public static Set<String> pinnedReagentGoal() {
        return Set.copyOf(PINNED_REAGENT_GOAL);
    }

    public static boolean matchesPinnedReagentGoal(Collection<String> reagentIds) {
        if (PINNED_REAGENT_GOAL.isEmpty() || reagentIds.isEmpty()) {
            return false;
        }
        for (String reagentId : reagentIds) {
            if (PINNED_REAGENT_GOAL.contains(reagentId)) {
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        KNOWN_SOURCE_REAGENTS.clear();
        KNOWN_SOURCE_DETAILS.clear();
        TESTED_EMPTY_SOURCES.clear();
        PINNED_REAGENT_GOAL.clear();
    }
}
