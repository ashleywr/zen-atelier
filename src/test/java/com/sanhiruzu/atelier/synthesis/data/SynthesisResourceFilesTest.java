package com.sanhiruzu.atelier.synthesis.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.engine.OutcomePreview;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisResourceFilesTest {
    @Test
    void bundledExtractionProfilesParse() throws IOException {
        List<Path> files = jsonFiles(Path.of("src/main/resources/data/zen_atelier/atelier/extraction_profiles"));

        assertThat(files).isNotEmpty();
        for (Path file : files) {
            assertThat(ExtractionProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file))).result())
                    .as(file.toString())
                    .isPresent();
        }
    }

    @Test
    void bundledSynthesisProfilesParse() throws IOException {
        List<Path> files = jsonFiles(Path.of("src/main/resources/data/zen_atelier/atelier/synthesis_profiles"));

        assertThat(files).isNotEmpty();
        for (Path file : files) {
            assertThat(SynthesisProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file))).result())
                    .as(file.toString())
                    .isPresent();
        }
    }

    @Test
    void redstoneProfileUsesCategoriesAndElementalAffinitiesSeparately() throws IOException {
        ExtractionProfileDefinition definition = ExtractionProfileDefinition.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/zen_atelier/atelier/extraction_profiles/redstone.json")))
                )
                .result()
                .orElseThrow();

        var reagent = definition.toCore().outcomes().getFirst().reagents().getFirst();

        assertThat(reagent.categories()).containsExactlyInAnyOrder("zen_atelier:conductive", "zen_atelier:abrasive");
        assertThat(reagent.elements()).containsOnly(
                java.util.Map.entry("fire", 1),
                java.util.Map.entry("wind", 1)
        );
        assertThat(reagent.shape()).isEqualTo(ReagentShape.LINE_TWO);
    }

    @Test
    void starterIngredientProfilesExposeEraOneRoles() throws IOException {
        assertFirstReagent("uni.json", "zen_atelier:abrasive", "earth", ReagentShape.LINE_TWO);
        assertFirstReagent("taun_herb.json", "zen_atelier:medicinal", "water", ReagentShape.SINGLE);
        assertFirstReagent("phlogiston_pebble.json", "zen_atelier:combustible", "fire", ReagentShape.LINE_TWO);
        assertFirstReagent("aqua_gel.json", "zen_atelier:binding", "water", ReagentShape.SQUARE_TWO);
        assertFirstReagent("ember_gel.json", "zen_atelier:binding", "fire", ReagentShape.ELBOW);
    }

    @Test
    void tierTwoSynthesisProfilesHaveLowBaseFailureChance() throws IOException {
        List<Path> files = jsonFiles(Path.of("src/main/resources/data/zen_atelier/atelier/synthesis_profiles"));

        for (Path file : files) {
            SynthesisProfileDefinition definition = SynthesisProfileDefinition.CODEC.parse(
                            JsonOps.INSTANCE,
                            JsonParser.parseString(Files.readString(file)))
                    .result()
                    .orElseThrow();
            if (definition.recipeTierCap() > 2) {
                continue;
            }

            double baseFailure = OutcomePreview.forSynthesis(definition.toCore().outcomes(), 0)
                    .failureProbability();
            assertThat(baseFailure)
                    .as(file.getFileName().toString())
                    .isLessThanOrEqualTo(0.05);
            assertThat(OutcomePreview.forSynthesis(definition.toCore().outcomes(), 100)
                    .probabilityOf(OutcomeClass.RECOVERABLE_FAILURE))
                    .as(file.getFileName().toString() + " risky failure remains possible")
                    .isGreaterThan(baseFailure);
        }
    }

    private static List<Path> jsonFiles(Path directory) throws IOException {
        try (var stream = Files.walk(directory)) {
            return stream.filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static void assertFirstReagent(String file, String category, String element, ReagentShape shape) throws IOException {
        ExtractionProfileDefinition definition = ExtractionProfileDefinition.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/zen_atelier/atelier/extraction_profiles", file)))
                )
                .result()
                .orElseThrow();
        var reagent = definition.toCore().outcomes().getFirst().reagents().getFirst();

        assertThat(reagent.categories()).contains(category);
        assertThat(reagent.elements()).containsKey(element);
        assertThat(reagent.shape()).isEqualTo(shape);
    }
}
