package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.engine.RequirementStatus;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisNounTest {
    @Test
    void findsNounsFromNamespacedIdsAndPlainAliases() {
        assertThat(SynthesisNoun.find("water")).contains(SynthesisNoun.WATER);
        assertThat(SynthesisNoun.find("zen_atelier:binding")).contains(SynthesisNoun.BINDING);
        assertThat(SynthesisNoun.find("zen_atelier:spark_reagent")).contains(SynthesisNoun.SPARK);
        assertThat(SynthesisNoun.find("zen_atelier:sparking")).contains(SynthesisNoun.SPARK);
    }

    @Test
    void importantPrototypeNounsHaveStableEmotiveColors() {
        assertThat(SynthesisNoun.WATER.color()).isEqualTo(0xFF76B7E8);
        assertThat(SynthesisNoun.BINDING.color()).isEqualTo(0xFFB06CD7);
        assertThat(SynthesisNoun.SPARK.color()).isEqualTo(0xFFFFC857);
    }

    @Test
    void synthesisNeedsPreferRecipeFamiliesOverElementQualifiers() {
        ReagentQuery query = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:binding"))
                .minElements(Map.of("water", 1))
                .build();
        RequirementStatus status = new RequirementStatus(new SynthesisRequirement(query, 20), 12, 8, false);

        assertThat(SynthesisStationText.summarizeQuery(status)).isEqualTo("Binding");
        assertThat(SynthesisStationText.requirementLine(status)).isEqualTo("12/20 Binding");
        assertThat(SynthesisStationText.elementBudget(query)).isEqualTo("Water 1");
    }
}
