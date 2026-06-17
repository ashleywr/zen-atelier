package com.sanhiruzu.atelier.synthesis.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory;
import com.sanhiruzu.atelier.synthesis.engine.OutcomePreview;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

    @Test
    void bundledSynthesisOutputsHaveLocalizedNamesAndTooltips() throws IOException {
        JsonObject lang = loadLang();
        Set<String> missing = new TreeSet<>();

        for (SynthesisProfileDefinition profile : synthesisDefinitions()) {
            for (SynthesisOutcomeDefinition outcome : profile.outcomes()) {
                for (SynthesisOutputDefinition output : outcome.outputs()) {
                    String key = itemKey(output.output());
                    if (!lang.has(key) && !lang.has(blockKey(output.output()))) {
                        missing.add(key);
                    }
                    if (output.virtual() && !lang.has("tooltip." + output.output().getNamespace() + "." + output.output().getPath())) {
                        missing.add("tooltip." + output.output().getNamespace() + "." + output.output().getPath());
                    }
                }
            }
        }

        assertThat(missing).isEmpty();
    }

    @Test
    void bundledSynthesisAffixesHaveLocalizedNamesAndDescriptions() throws IOException {
        JsonObject lang = loadLang();
        Set<String> missing = new TreeSet<>();

        for (String affix : usedAffixes()) {
            ResourceLocation id = ResourceLocation.parse(affix);
            String nameKey = "zen_atelier.affix." + id.getPath();
            if (!lang.has(nameKey)) {
                missing.add(nameKey);
            }
        }
        for (String affix : finalOutputAffixes()) {
            ResourceLocation id = ResourceLocation.parse(affix);
            String descKey = "zen_atelier.affix." + id.getPath() + ".desc";
            if (!lang.has(descKey)) {
                missing.add(descKey);
            }
        }

        assertThat(missing).isEmpty();
    }

    @Test
    void bundledSynthesisRequirementsAreReachableFromExtractionOrByproducts() throws IOException {
        Set<String> producedCategories = new LinkedHashSet<>();
        Set<String> producedElements = new LinkedHashSet<>();

        for (ExtractionProfileDefinition profile : extractionDefinitions()) {
            for (ExtractionOutcomeDefinition outcome : profile.outcomes()) {
                for (ReagentStackDefinition reagent : outcome.reagents()) {
                    reagent.categories().stream().map(ResourceLocation::toString).forEach(producedCategories::add);
                    producedElements.addAll(reagent.elements().keySet());
                }
                for (ReagentStackDefinition byproduct : outcome.byproducts()) {
                    byproduct.categories().stream().map(ResourceLocation::toString).forEach(producedCategories::add);
                    producedElements.addAll(byproduct.elements().keySet());
                }
            }
        }
        for (SynthesisProfileDefinition profile : synthesisDefinitions()) {
            for (SynthesisOutcomeDefinition outcome : profile.outcomes()) {
                for (ReagentStackDefinition byproduct : outcome.byproducts()) {
                    byproduct.categories().stream().map(ResourceLocation::toString).forEach(producedCategories::add);
                    producedElements.addAll(byproduct.elements().keySet());
                }
            }
        }

        Set<String> missing = new TreeSet<>();
        for (SynthesisProfileDefinition profile : synthesisDefinitions()) {
            for (SynthesisRequirementDefinition requirement : profile.requirements()) {
                requirement.query().requiredCategories().stream()
                        .map(ResourceLocation::toString)
                        .filter(category -> !producedCategories.contains(category))
                        .forEach(category -> missing.add(profile.id() + " category " + category));
                requirement.query().minElements().keySet().stream()
                        .filter(element -> !producedElements.contains(element))
                        .forEach(element -> missing.add(profile.id() + " element " + element));
            }
        }

        assertThat(missing).isEmpty();
    }

    @Test
    void visibleSynthesisCategoriesHaveRecipes() throws IOException {
        Set<String> categories = new LinkedHashSet<>();
        for (SynthesisProfileDefinition profile : synthesisDefinitions()) {
            categories.add(SynthesisRecipeCategory.normalize(profile.category()));
        }

        assertThat(categories).containsAll(SynthesisRecipeCategory.orderedIds());
    }

    private static List<Path> jsonFiles(Path directory) throws IOException {
        try (var stream = Files.walk(directory)) {
            return stream.filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static List<ExtractionProfileDefinition> extractionDefinitions() throws IOException {
        List<Path> files = jsonFiles(Path.of("src/main/resources/data/zen_atelier/atelier/extraction_profiles"));
        return files.stream()
                .map(SynthesisResourceFilesTest::parseExtraction)
                .toList();
    }

    private static List<SynthesisProfileDefinition> synthesisDefinitions() throws IOException {
        List<Path> files = jsonFiles(Path.of("src/main/resources/data/zen_atelier/atelier/synthesis_profiles"));
        return files.stream()
                .map(SynthesisResourceFilesTest::parseSynthesis)
                .toList();
    }

    private static ExtractionProfileDefinition parseExtraction(Path file) {
        try {
            return ExtractionProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file)))
                    .result()
                    .orElseThrow();
        } catch (IOException e) {
            throw new IllegalStateException(file.toString(), e);
        }
    }

    private static SynthesisProfileDefinition parseSynthesis(Path file) {
        try {
            return SynthesisProfileDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file)))
                    .result()
                    .orElseThrow();
        } catch (IOException e) {
            throw new IllegalStateException(file.toString(), e);
        }
    }

    private static JsonObject loadLang() throws IOException {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/zen_atelier/lang/en_us.json")))
                .getAsJsonObject();
    }

    private static Set<String> usedAffixes() throws IOException {
        Set<String> affixes = new TreeSet<>();
        affixes.addAll(finalOutputAffixes());
        for (SynthesisProfileDefinition profile : synthesisDefinitions()) {
            for (SynthesisRequirementDefinition requirement : profile.requirements()) {
                requirement.query().requiredTraits().stream().map(ResourceLocation::toString).forEach(affixes::add);
            }
            for (SynthesisOutcomeDefinition outcome : profile.outcomes()) {
                for (ReagentStackDefinition byproduct : outcome.byproducts()) {
                    byproduct.traits().stream().map(ResourceLocation::toString).forEach(affixes::add);
                }
            }
        }
        for (ExtractionProfileDefinition profile : extractionDefinitions()) {
            for (ExtractionOutcomeDefinition outcome : profile.outcomes()) {
                for (ReagentStackDefinition reagent : outcome.reagents()) {
                    reagent.traits().stream().map(ResourceLocation::toString).forEach(affixes::add);
                }
                for (ReagentStackDefinition byproduct : outcome.byproducts()) {
                    byproduct.traits().stream().map(ResourceLocation::toString).forEach(affixes::add);
                }
            }
        }
        JsonObject fusionFile = JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/zen_atelier/atelier/trait_fusions/core_fusions.json")))
                .getAsJsonObject();
        for (JsonElement ruleElement : fusionFile.getAsJsonArray("rules")) {
            JsonObject rule = ruleElement.getAsJsonObject();
            if (rule.has("inputs")) {
                for (JsonElement input : rule.getAsJsonArray("inputs")) {
                    affixes.add(input.getAsString());
                }
            }
            if (rule.has("output_affix")) {
                affixes.add(rule.get("output_affix").getAsString());
            }
        }
        return affixes;
    }

    private static Set<String> finalOutputAffixes() throws IOException {
        Set<String> affixes = new TreeSet<>();
        for (SynthesisProfileDefinition profile : synthesisDefinitions()) {
            for (SynthesisOutcomeDefinition outcome : profile.outcomes()) {
                for (SynthesisOutputDefinition output : outcome.outputs()) {
                    output.affixes().stream().map(ResourceLocation::toString).forEach(affixes::add);
                }
            }
        }
        return affixes;
    }

    private static String itemKey(ResourceLocation id) {
        return "item." + id.getNamespace() + "." + id.getPath();
    }

    private static String blockKey(ResourceLocation id) {
        return "block." + id.getNamespace() + "." + id.getPath();
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
