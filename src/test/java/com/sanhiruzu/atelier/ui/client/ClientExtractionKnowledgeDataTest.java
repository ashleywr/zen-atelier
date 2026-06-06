package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.ui.network.ExtractionKnowledgeSyncPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClientExtractionKnowledgeDataTest {
    @BeforeEach
    void setUp() {
        ClientExtractionKnowledgeData.clear();
    }

    @Test
    void updateStoresKnownSourcesAndTestedEmptySources() {
        ClientExtractionKnowledgeData.update(
                Map.of("minecraft:flint", List.of("zen_atelier:abrasive_reagent")),
                Set.of("minecraft:cobblestone")
        );

        assertTrue(ClientExtractionKnowledgeData.hasKnownSource("minecraft:flint"));
        assertEquals(List.of("zen_atelier:abrasive_reagent"), ClientExtractionKnowledgeData.knownReagents("minecraft:flint"));
        assertTrue(ClientExtractionKnowledgeData.isTestedEmpty("minecraft:cobblestone"));
    }

    @Test
    void updateReplacesPreviousKnowledge() {
        ClientExtractionKnowledgeData.update(
                Map.of("minecraft:flint", List.of("zen_atelier:abrasive_reagent")),
                Set.of("minecraft:cobblestone")
        );

        ClientExtractionKnowledgeData.update(
                Map.of("minecraft:honey_bottle", List.of("zen_atelier:binding_reagent")),
                Set.of()
        );

        assertFalse(ClientExtractionKnowledgeData.hasKnownSource("minecraft:flint"));
        assertFalse(ClientExtractionKnowledgeData.isTestedEmpty("minecraft:cobblestone"));
        assertEquals(List.of("zen_atelier:binding_reagent"), ClientExtractionKnowledgeData.knownReagents("minecraft:honey_bottle"));
    }

    @Test
    void snapshotsAreDefensiveCopies() {
        ClientExtractionKnowledgeData.update(
                Map.of("minecraft:flint", List.of("zen_atelier:abrasive_reagent")),
                Set.of("minecraft:cobblestone")
        );

        Map<String, List<String>> known = ClientExtractionKnowledgeData.allKnownSourceReagents();
        Set<String> empty = ClientExtractionKnowledgeData.allTestedEmptySources();

        assertThrows(UnsupportedOperationException.class, () -> known.put("minecraft:honey_bottle", List.of()));
        assertThrows(UnsupportedOperationException.class, () -> empty.add("minecraft:dirt"));
    }

    @Test
    void findsKnownSourcesForReagentGoals() {
        ClientExtractionKnowledgeData.update(
                Map.of(
                        "minecraft:flint", List.of("zen_atelier:abrasive_reagent", "zen_atelier:spark_reagent"),
                        "minecraft:honey_bottle", List.of("zen_atelier:binding_reagent"),
                        "minecraft:copper_ingot", List.of("zen_atelier:conductive_reagent")
                ),
                Set.of()
        );

        assertEquals(List.of("minecraft:honey_bottle"),
                ClientExtractionKnowledgeData.knownSourcesForReagent("zen_atelier:binding_reagent"));
        assertEquals(List.of("minecraft:copper_ingot", "minecraft:flint"),
                ClientExtractionKnowledgeData.knownSourcesForAny(List.of(
                        "zen_atelier:conductive_reagent",
                        "zen_atelier:spark_reagent"
                )));
        assertTrue(ClientExtractionKnowledgeData.knownSourcesForAny(List.of()).isEmpty());
    }

    @Test
    void storesDetailedSourceKnowledge() {
        ClientExtractionKnowledgeData.update(
                Map.of("minecraft:flint", List.of("zen_atelier:abrasive_reagent")),
                Set.of(),
                Map.of("minecraft:flint", new ExtractionKnowledgeSyncPayload.SourceKnowledge(
                        3,
                        List.of("zen_atelier:abrasive_reagent"),
                        List.of("zen_atelier:abrasive"),
                        Map.of("sharp", 2)
                ))
        );

        ExtractionKnowledgeSyncPayload.SourceKnowledge details =
                ClientExtractionKnowledgeData.knownSourceDetails("minecraft:flint");
        assertEquals(3, details.attempts());
        assertEquals(List.of("zen_atelier:abrasive"), details.traits());
        assertEquals(Map.of("sharp", 2), details.elements());
    }

    @Test
    void pinsReagentGoalForClientFiltering() {
        ClientExtractionKnowledgeData.pinReagentGoal(List.of(
                "zen_atelier:spark_reagent",
                "zen_atelier:binding_reagent"
        ));

        assertTrue(ClientExtractionKnowledgeData.hasPinnedReagentGoal());
        assertTrue(ClientExtractionKnowledgeData.matchesPinnedReagentGoal(List.of("zen_atelier:binding_reagent")));
        assertTrue(ClientExtractionKnowledgeData.matchesPinnedReagentGoal(List.of("zen_atelier:spark_reagent")));
        assertFalse(ClientExtractionKnowledgeData.matchesPinnedReagentGoal(List.of("zen_atelier:abrasive_reagent")));
    }

    @Test
    void clearPinnedReagentGoalRemovesOnlyPinnedGoal() {
        ClientExtractionKnowledgeData.update(
                Map.of("minecraft:flint", List.of("zen_atelier:abrasive_reagent")),
                Set.of()
        );
        ClientExtractionKnowledgeData.pinReagentGoal(List.of("zen_atelier:binding_reagent"));

        ClientExtractionKnowledgeData.clearPinnedReagentGoal();

        assertFalse(ClientExtractionKnowledgeData.hasPinnedReagentGoal());
        assertTrue(ClientExtractionKnowledgeData.hasKnownSource("minecraft:flint"));
    }

    @Test
    void clearRemovesAllKnowledge() {
        ClientExtractionKnowledgeData.update(
                Map.of("minecraft:flint", List.of("zen_atelier:abrasive_reagent")),
                Set.of("minecraft:cobblestone")
        );
        ClientExtractionKnowledgeData.pinReagentGoal(List.of("zen_atelier:binding_reagent"));

        ClientExtractionKnowledgeData.clear();

        assertFalse(ClientExtractionKnowledgeData.hasKnownSource("minecraft:flint"));
        assertFalse(ClientExtractionKnowledgeData.isTestedEmpty("minecraft:cobblestone"));
        assertTrue(ClientExtractionKnowledgeData.allKnownSourceReagents().isEmpty());
        assertTrue(ClientExtractionKnowledgeData.allTestedEmptySources().isEmpty());
        assertFalse(ClientExtractionKnowledgeData.hasPinnedReagentGoal());
    }
}
