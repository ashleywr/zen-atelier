package com.sanhiruzu.atelier.synthesis.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SynthesisDefinitionCodecTest {
    @Test
    void parsesExtractionProfileAndConvertsToCore() {
        ExtractionProfileDefinition definition = ExtractionProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "schema": 1,
                  "id": "zen_atelier:blaze_powder",
                  "source": "minecraft:blaze_powder",
                  "source_tier_cap": 3,
                  "outcomes": [
                    {
                      "outcome": "success",
                      "weight": 90,
                      "reagents": [
                        {
                          "reagent": "zen_atelier:fire_reagent",
                          "amount": 50,
                          "tier": 2,
                          "quality": 40,
                          "purity": 65,
                          "instability": 20,
                          "elements": { "fire": 2 },
                          "traits": [ "zen_atelier:volatile" ],
                          "source_hints": [ "minecraft:blaze_powder" ]
                        }
                      ]
                    },
                    {
                      "outcome": "recoverable_failure",
                      "weight": 10,
                      "byproducts": [
                        {
                          "reagent": "zen_atelier:ash_residue",
                          "amount": 10,
                          "tier": 1
                        }
                      ]
                    }
                  ]
                }
                """)).getOrThrow();

        assertThat(definition.id()).isEqualTo(ResourceLocation.fromNamespaceAndPath("zen_atelier", "blaze_powder"));
        assertThat(definition.source()).isEqualTo("minecraft:blaze_powder");
        assertThat(definition.outcomes()).hasSize(2);
        assertThat(definition.toCore().outcomes().getFirst().reagents().getFirst().traits())
                .containsExactly("zen_atelier:volatile");
    }

    @Test
    void parsesExtractionReagentAttributeRanges() {
        ExtractionProfileDefinition definition = ExtractionProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "schema": 1,
                  "id": "zen_atelier:rotten_flesh",
                  "source": "minecraft:rotten_flesh",
                  "source_tier_cap": 1,
                  "outcomes": [
                    {
                      "outcome": "success",
                      "weight": 1,
                      "reagents": [
                        {
                          "reagent": "zen_atelier:decay_reagent",
                          "amount": 12,
                          "amount_range": { "min": 8, "max": 16 },
                          "tier": 1,
                          "quality_range": { "min": 5, "max": 25 },
                          "purity_range": { "min": 10, "max": 35 },
                          "instability_range": { "min": 40, "max": 70 },
                          "elements": { "life": 1, "decay": 2 },
                          "traits": [ "zen_atelier:spoiled" ],
                          "source_hints": [ "minecraft:rotten_flesh" ]
                        }
                      ]
                    }
                  ]
                }
                """)).getOrThrow();

        var reagent = new com.sanhiruzu.atelier.synthesis.engine.ExtractionEngine()
                .roll(new com.sanhiruzu.atelier.synthesis.engine.ExtractionAttempt(definition.toCore(), 1, 1, 1, 1, 17L))
                .reagents()
                .getFirst();

        assertThat(reagent.reagentId()).isEqualTo("zen_atelier:decay_reagent");
        assertThat(reagent.amount()).isBetween(8, 16);
        assertThat(reagent.quality()).isBetween(5, 25);
        assertThat(reagent.purity()).isBetween(10, 35);
        assertThat(reagent.instability()).isBetween(40, 70);
        assertThat(reagent.elements()).containsEntry("decay", 2);
        assertThat(reagent.traits()).containsExactly("zen_atelier:spoiled");
    }

    @Test
    void parsesTagExtractionSource() {
        ExtractionProfileDefinition definition = ExtractionProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "schema": 1,
                  "id": "zen_atelier:copper_ingots",
                  "source": "#c:ingots/copper",
                  "source_tier_cap": 2,
                  "outcomes": [
                    {
                      "outcome": "success",
                      "weight": 1
                    }
                  ]
                }
                """)).getOrThrow();

        assertThat(definition.source()).isEqualTo("#c:ingots/copper");
        assertThat(definition.toCore().sourceKey()).isEqualTo("#c:ingots/copper");
    }

    @Test
    void parsesSynthesisProfileAndConvertsToCore() {
        SynthesisProfileDefinition definition = SynthesisProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "schema": 1,
                  "id": "zen_atelier:heated_tool_coating",
                  "category": "bombs",
                  "recipe_tier_cap": 4,
                  "requirements": [
                    {
                      "amount": 50,
                      "query": {
                        "reagents": [ "zen_atelier:fire_reagent" ],
                        "min_tier": 2,
                        "min_purity": 50,
                        "min_elements": { "fire": 2 },
                        "required_traits": [ "zen_atelier:volatile" ]
                      }
                    }
                  ],
                  "outcomes": [
                    {
                      "outcome": "success",
                      "weight": 80,
                      "outputs": [
                        {
                          "output": "zen_atelier:heated_tool_coating",
                          "count": 1,
                          "tier": 3,
                          "quality": 70,
                          "virtual": true,
                          "affixes": [ "zen_atelier:swift" ]
                        }
                      ]
                    },
                    {
                      "outcome": "messy_failure",
                      "weight": 20,
                      "byproducts": [
                        {
                          "reagent": "zen_atelier:scorched_residue",
                          "amount": 8,
                          "tier": 1
                        }
                      ]
                    }
                  ]
                }
                """)).getOrThrow();

        assertThat(definition.toCore().requirements()).singleElement()
                .extracting(requirement -> requirement.query().requiredTraits())
                .isEqualTo(java.util.Set.of("zen_atelier:volatile"));
        assertThat(definition.toCore().category()).isEqualTo("bombs");
        assertThat(definition.toCore().outcomes().getFirst().outputs().getFirst().affixes())
                .containsExactly("zen_atelier:swift");
    }

    @Test
    void parsesSynthesisBoardMetadata() {
        SynthesisProfileDefinition definition = SynthesisProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "schema": 1,
                  "id": "zen_atelier:board_test",
                  "recipe_tier_cap": 3,
                  "board": {
                    "size": 5,
                    "empty_cell_success_penalty": 2,
                    "empty_cell_perfect_penalty": 5,
                    "nodes": [
                      {
                        "x": 2,
                        "y": 1,
                        "type": "element",
                        "required_element": "fire",
                        "required_element_value": 2,
                        "quality_bonus": 8
                      },
                      {
                        "x": 3,
                        "y": 3,
                        "type": "morph",
                        "required_element": "wind",
                        "morph_target": "zen_atelier:aero_lace_coating",
                        "perfect_bonus": 5
                      }
                    ]
                  },
                  "requirements": [
                    {
                      "amount": 10,
                      "query": {
                        "required_categories": [ "zen_atelier:volatile" ],
                        "min_elements": { "fire": 2 }
                      }
                    }
                  ],
                  "outcomes": [
                    {
                      "outcome": "success",
                      "weight": 1
                    }
                  ]
                }
                """)).getOrThrow();

        var board = definition.toCore().board();

        assertThat(board.size()).isEqualTo(5);
        assertThat(board.emptyCellSuccessPenalty()).isEqualTo(2);
        assertThat(board.nodes()).hasSize(2);
        assertThat(board.nodes().getLast().morphTarget()).contains("zen_atelier:aero_lace_coating");
    }

    @Test
    void rejectsUnknownOutcomeClass() {
        assertThat(ExtractionOutcomeDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "outcome": "too_good",
                  "weight": 1
                }
                """)).error()).isPresent();
    }

    @Test
    void rejectsInvalidExtractionProfileNumbersAtParseTime() {
        assertThat(ExtractionProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "schema": 1,
                  "id": "zen_atelier:bad",
                  "source": "minecraft:flint",
                  "source_tier_cap": 0,
                  "outcomes": [
                    {
                      "outcome": "success",
                      "weight": 1
                    }
                  ]
                }
                """)).error()).isPresent();

        assertThat(ExtractionProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "schema": 1,
                  "id": "zen_atelier:bad",
                  "source": "minecraft:flint",
                  "source_tier_cap": 1,
                  "outcomes": []
                }
                """)).error()).isPresent();
    }

    @Test
    void rejectsInvalidNestedDefinitionNumbersAtParseTime() {
        assertThat(ExtractionOutcomeDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "outcome": "success",
                  "weight": 0
                }
                """)).error()).isPresent();

        assertThat(ReagentStackDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "reagent": "zen_atelier:bad",
                  "amount": -1,
                  "tier": 1
                }
                """)).error()).isPresent();

        assertThat(ReagentQueryDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "min_tier": 4,
                  "max_tier": 2
                }
                """)).error()).isPresent();

        assertThat(ReagentStackDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "reagent": "zen_atelier:bad",
                  "amount": 1,
                  "amount_range": { "min": 0, "max": 1 },
                  "tier": 1
                }
                """)).error()).isPresent();

        assertThat(ReagentStackDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "reagent": "zen_atelier:bad",
                  "amount": 1,
                  "tier": 1,
                  "quality_range": { "min": 40, "max": 120 }
                }
                """)).error()).isPresent();
    }

    @Test
    void rejectsInvalidSynthesisProfileNumbersAtParseTime() {
        assertThat(SynthesisProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "schema": 1,
                  "id": "zen_atelier:bad",
                  "recipe_tier_cap": 7,
                  "requirements": [
                    {
                      "amount": 1,
                      "query": {}
                    }
                  ],
                  "outcomes": [
                    {
                      "outcome": "success",
                      "weight": 1
                    }
                  ]
                }
                """)).error()).isPresent();

        assertThat(SynthesisProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "schema": 1,
                  "id": "zen_atelier:bad",
                  "recipe_tier_cap": 1,
                  "requirements": [
                    {
                      "amount": 0,
                      "query": {}
                    }
                  ],
                  "outcomes": [
                    {
                      "outcome": "success",
                      "weight": 1
                    }
                  ]
                }
                """)).error()).isPresent();
    }

    @Test
    void registriesReplaceContents() {
        ResourceLocation first = ResourceLocation.fromNamespaceAndPath("zen_atelier", "first");
        ResourceLocation second = ResourceLocation.fromNamespaceAndPath("zen_atelier", "second");
        ExtractionProfileDefinition firstProfile = new ExtractionProfileDefinition(
                1,
                first,
                "minecraft:copper_ingot",
                2,
                java.util.List.of(new ExtractionOutcomeDefinition(OutcomeClass.SUCCESS, 1, java.util.List.of(), java.util.List.of()))
        );
        ExtractionProfileDefinition secondProfile = new ExtractionProfileDefinition(
                1,
                second,
                "minecraft:gold_ingot",
                3,
                java.util.List.of(new ExtractionOutcomeDefinition(OutcomeClass.SUCCESS, 1, java.util.List.of(), java.util.List.of()))
        );

        ExtractionProfileRegistry.replaceAll(Map.of(first, firstProfile));
        assertThat(ExtractionProfileRegistry.get(first)).isEqualTo(firstProfile);

        ExtractionProfileRegistry.replaceAll(Map.of(second, secondProfile));
        assertThat(ExtractionProfileRegistry.get(first)).isNull();
        assertThat(ExtractionProfileRegistry.get(second)).isEqualTo(secondProfile);
        assertThatThrownBy(() -> ExtractionProfileRegistry.all().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void extractionReloadSkipsProfilesWhoseEmbeddedIdDoesNotMatchResourceId() {
        ResourceLocation resourceId = ResourceLocation.fromNamespaceAndPath("zen_atelier", "resource_id");
        ExtractionProfileRegistry.replaceAll(Map.of());

        new ExtractionProfileReloadListener().apply(Map.of(resourceId, JsonParser.parseString("""
                {
                  "schema": 1,
                  "id": "zen_atelier:embedded_id",
                  "source": "minecraft:copper_ingot",
                  "source_tier_cap": 2,
                  "outcomes": [
                    {
                      "outcome": "success",
                      "weight": 1
                    }
                  ]
                }
                """)), null, null);

        assertThat(ExtractionProfileRegistry.get(resourceId)).isNull();
        assertThat(ExtractionProfileRegistry.all()).isEmpty();
    }

    @Test
    void synthesisReloadSkipsProfilesWhoseEmbeddedIdDoesNotMatchResourceId() {
        ResourceLocation resourceId = ResourceLocation.fromNamespaceAndPath("zen_atelier", "resource_id");
        SynthesisProfileRegistry.replaceAll(Map.of());

        new SynthesisProfileReloadListener().apply(Map.of(resourceId, JsonParser.parseString("""
                {
                  "schema": 1,
                  "id": "zen_atelier:embedded_id",
                  "recipe_tier_cap": 2,
                  "requirements": [
                    {
                      "amount": 10,
                      "query": {
                        "reagents": [ "zen_atelier:abrasive_reagent" ]
                      }
                    }
                  ],
                  "outcomes": [
                    {
                      "outcome": "success",
                      "weight": 1
                    }
                  ]
                }
                """)), null, null);

        assertThat(SynthesisProfileRegistry.get(resourceId)).isNull();
        assertThat(SynthesisProfileRegistry.all()).isEmpty();
    }
}
