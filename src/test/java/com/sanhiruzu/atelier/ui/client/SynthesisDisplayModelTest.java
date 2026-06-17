package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutcome;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlanner;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisDisplayModelTest {
    @Test
    void separatesRequiredEssencesAndElementsFromAddedTraitsAndResonance() {
        ReagentQuery medicinalWater = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:medicinal"))
                .minElements(Map.of("water", 1))
                .build();
        ReagentQuery binding = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:binding"))
                .build();
        SynthesisRequirement medicinalRequirement = new SynthesisRequirement(medicinalWater, 30);
        SynthesisRequirement bindingRequirement = new SynthesisRequirement(binding, 15);
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:instant_salve",
                "recovery",
                List.of(medicinalRequirement, bindingRequirement),
                2,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:instant_salve", 1, 1, 1, List.of())),
                        List.of()
                ))
        );
        ReagentStack waterReagent = new ReagentStack(
                "zen_atelier:aqua_gel_reagent",
                Set.of("zen_atelier:medicinal"),
                12,
                1,
                50,
                50,
                0,
                Map.of("water", 1),
                List.of("zen_atelier:soothing"),
                ReagentShape.SINGLE,
                Set.of()
        );
        ReagentContainer container = new ReagentContainer();
        container.insert(waterReagent);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, container, 0);

        SynthesisDisplayModel model = SynthesisDisplayModel.from(
                plan,
                List.of(waterReagent),
                List.of("Soothing +1"),
                List.of("None")
        );

        assertThat(model.essences()).extracting(SynthesisDisplayModel.Line::label)
                .containsExactly("Medicinal", "Binding");
        assertThat(model.elements()).singleElement()
                .satisfies(line -> {
                    assertThat(line.label()).isEqualTo("Water");
                    assertThat(line.available()).isEqualTo(1);
                    assertThat(line.required()).isEqualTo(1);
                    assertThat(line.requiredBlocker()).isTrue();
                });
        assertThat(model.traits()).extracting(SynthesisDisplayModel.TextLine::text)
                .containsExactly("Soothing +1");
        assertThat(model.resonance()).extracting(SynthesisDisplayModel.TextLine::text)
                .containsExactly("None");
    }
}
