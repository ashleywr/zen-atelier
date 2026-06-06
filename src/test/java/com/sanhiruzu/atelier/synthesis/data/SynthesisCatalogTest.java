package com.sanhiruzu.atelier.synthesis.data;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionAttempt;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionEngine;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionOutcome;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionProfile;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisAttempt;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisEngine;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutcome;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisCatalogTest {
    @Test
    void findsExtractionProfilesByExactItemAndTagSources() {
        ExtractionProfileDefinition exact = new ExtractionProfileDefinition(
                1,
                id("exact_copper"),
                "minecraft:copper_ingot",
                2,
                List.of(new ExtractionOutcomeDefinition(OutcomeClass.SUCCESS, 1, List.of(), List.of()))
        );
        ExtractionProfileDefinition tag = new ExtractionProfileDefinition(
                1,
                id("tagged_copper"),
                "#c:ingots/copper",
                2,
                List.of(new ExtractionOutcomeDefinition(OutcomeClass.SUCCESS, 1, List.of(), List.of()))
        );
        ExtractionProfileRegistry.replaceAll(Map.of(exact.id(), exact, tag.id(), tag));

        assertThat(SynthesisCatalog.findExtractionProfiles("minecraft:copper_ingot", Set.of("#c:ingots/copper")))
                .extracting(ExtractionProfile::id)
                .containsExactlyInAnyOrder("zen_atelier:exact_copper", "zen_atelier:tagged_copper");
        assertThat(SynthesisCatalog.findExtractionProfiles("minecraft:gold_ingot", Set.of("#c:ingots/gold")))
                .isEmpty();
    }

    @Test
    void registryProfilesFeedEngines() {
        ExtractionProfileDefinition extractionDefinition = new ExtractionProfileDefinition(
                1,
                id("flint"),
                "minecraft:flint",
                2,
                List.of(new ExtractionOutcomeDefinition(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new ReagentStackDefinition(
                                id("abrasive_reagent"),
                                50,
                                1,
                                30,
                                50,
                                10,
                                Map.of("sharp", 2),
                                List.of(id("abrasive")),
                                List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "flint"))
                        )),
                        List.of()
                ))
        );
        SynthesisProfileDefinition synthesisDefinition = new SynthesisProfileDefinition(
                1,
                id("crude_mining_coating"),
                2,
                List.of(new SynthesisRequirementDefinition(
                        new ReagentQueryDefinition(
                                List.of(id("abrasive_reagent")),
                                1,
                                6,
                                0,
                                0,
                                100,
                                Map.of("sharp", 1),
                                List.of(),
                                List.of()
                        ),
                        50
                )),
                List.of(new SynthesisOutcomeDefinition(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutputDefinition(
                                id("crude_mining_coating"),
                                1,
                                1,
                                40,
                                List.of(id("sharp"))
                        )),
                        List.of()
                ))
        );
        ExtractionProfileRegistry.replaceAll(Map.of(extractionDefinition.id(), extractionDefinition));
        SynthesisProfileRegistry.replaceAll(Map.of(synthesisDefinition.id(), synthesisDefinition));

        ExtractionProfile extractionProfile = SynthesisCatalog.findExtractionProfiles("minecraft:flint", Set.of()).getFirst();
        ReagentStack reagent = new ExtractionEngine()
                .roll(new ExtractionAttempt(extractionProfile, 1, 2, 2, 2, 1L))
                .reagents()
                .getFirst();
        SynthesisProfile synthesisProfile = SynthesisCatalog.getSynthesisProfile(id("crude_mining_coating")).orElseThrow();

        SynthesisOutput output = new SynthesisEngine()
                .roll(new SynthesisAttempt(synthesisProfile, List.of(reagent), 2, 2, 2, 1L))
                .outputs()
                .getFirst();

        assertThat(output.outputId()).isEqualTo("zen_atelier:crude_mining_coating");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("zen_atelier", path);
    }
}
