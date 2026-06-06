package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ApparatusState;
import com.sanhiruzu.atelier.synthesis.core.AttemptContext;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.RoomAlchemyContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtractionEngineTest {
    private final ExtractionEngine engine = new ExtractionEngine();

    @Test
    void sameSeedProducesSameResult() {
        ExtractionProfile profile = sampleProfile();
        ExtractionAttempt attempt = new ExtractionAttempt(profile, 1, 6, 6, 6, 12345L);

        ExtractionResult first = engine.roll(attempt);
        ExtractionResult second = engine.roll(attempt);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void traceIncludesRisk() {
        ExtractionResult result = engine.roll(new ExtractionAttempt(sampleProfile(), 1, 6, 6, 6, 30, 12345L));

        assertThat(result.trace().lines()).contains("risk 30");
        assertThat(result.trace().lines()).anySatisfy(line -> assertThat(line).startsWith("success probability "));
    }

    @Test
    void canBuildAttemptFromContext() {
        AttemptContext context = new AttemptContext(
                new ApparatusState("zen_atelier:copper_extractor", 2, 10),
                new RoomAlchemyContext("zen_atelier:atelier", 3, 50, 0, 20, Map.of(), Set.of()),
                4,
                30
        );

        ExtractionResult result = engine.roll(new ExtractionAttempt(sampleProfile(), 1, context, 12345L));

        assertThat(result.effectiveTierCap()).isEqualTo(2);
        assertThat(result.trace().lines()).contains("risk 40");
    }

    @Test
    void sourceCapLimitsOutputTierEvenWithBetterApparatusAndRoom() {
        ExtractionProfile profile = new ExtractionProfile(
                "zen_atelier:test_copper",
                "minecraft:copper_ingot",
                2,
                List.of(new ExtractionOutcome(
                        OutcomeClass.PERFECT_SUCCESS,
                        1,
                        List.of(ReagentStack.simple("zen_atelier:conductive_reagent", 50, 5)),
                        List.of()
                ))
        );

        ExtractionResult result = engine.roll(new ExtractionAttempt(profile, 1, 6, 6, 6, 1L));

        assertThat(result.effectiveTierCap()).isEqualTo(2);
        assertThat(result.reagents()).singleElement()
                .extracting(ReagentStack::tier)
                .isEqualTo(2);
    }

    @Test
    void apparatusCapLimitsOutputTier() {
        ExtractionProfile profile = new ExtractionProfile(
                "zen_atelier:test_gold",
                "minecraft:gold_ingot",
                5,
                List.of(new ExtractionOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(ReagentStack.simple("zen_atelier:resonant_reagent", 50, 5)),
                        List.of()
                ))
        );

        ExtractionResult result = engine.roll(new ExtractionAttempt(profile, 1, 3, 6, 6, 1L));

        assertThat(result.effectiveTierCap()).isEqualTo(3);
        assertThat(result.reagents()).singleElement()
                .extracting(ReagentStack::tier)
                .isEqualTo(3);
    }

    @Test
    void batchExtractionScalesReagentAmounts() {
        ExtractionProfile profile = new ExtractionProfile(
                "zen_atelier:test_flint",
                "minecraft:flint",
                2,
                List.of(new ExtractionOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(ReagentStack.simple("zen_atelier:abrasive_reagent", 25, 1)),
                        List.of(ReagentStack.simple("zen_atelier:stone_dust", 5, 1))
                ))
        );

        ExtractionResult result = engine.roll(new ExtractionAttempt(profile, 8, 2, 2, 2, 1L));

        assertThat(result.reagents()).singleElement()
                .extracting(ReagentStack::amount)
                .isEqualTo(200);
        assertThat(result.byproducts()).singleElement()
                .extracting(ReagentStack::amount)
                .isEqualTo(40);
    }

    @Test
    void reagentAttributeRangesRollWithinProfileIdentity() {
        ExtractionProfile profile = new ExtractionProfile(
                "zen_atelier:test_rotten_flesh",
                "minecraft:rotten_flesh",
                1,
                List.of(ExtractionOutcome.fromTemplates(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new ReagentRollTemplate(
                                "zen_atelier:organic_decay_reagent",
                                new ReagentRollTemplate.IntRange(8, 16),
                                1,
                                new ReagentRollTemplate.IntRange(5, 30),
                                new ReagentRollTemplate.IntRange(10, 45),
                                new ReagentRollTemplate.IntRange(35, 80),
                                Map.of("life", 1, "decay", 2),
                                List.of("zen_atelier:organic", "zen_atelier:spoiled"),
                                Set.of("minecraft:rotten_flesh")
                        )),
                        List.of()
                ))
        );

        ExtractionResult first = engine.roll(new ExtractionAttempt(profile, 1, 1, 1, 1, 123L));
        ExtractionResult second = engine.roll(new ExtractionAttempt(profile, 1, 1, 1, 1, 123L));

        ReagentStack reagent = first.reagents().getFirst();
        assertThat(first.reagents()).isEqualTo(second.reagents());
        assertThat(reagent.reagentId()).isEqualTo("zen_atelier:organic_decay_reagent");
        assertThat(reagent.amount()).isBetween(8, 16);
        assertThat(reagent.quality()).isBetween(5, 30);
        assertThat(reagent.purity()).isBetween(10, 45);
        assertThat(reagent.instability()).isBetween(35, 80);
        assertThat(reagent.elements()).containsEntry("decay", 2);
        assertThat(reagent.traits()).containsExactly("zen_atelier:organic", "zen_atelier:spoiled");
    }

    @Test
    void recoverableFailureCanReturnUsefulByproducts() {
        ExtractionProfile profile = new ExtractionProfile(
                "zen_atelier:test_failure",
                "minecraft:rotten_flesh",
                1,
                List.of(new ExtractionOutcome(
                        OutcomeClass.RECOVERABLE_FAILURE,
                        1,
                        List.of(),
                        List.of(new ReagentStack(
                                "zen_atelier:decay_residue",
                                10,
                                1,
                                5,
                                20,
                                30,
                                Map.of("life", 1),
                                List.of("spoiled"),
                                Set.of("minecraft:rotten_flesh")
                        ))
                ))
        );

        ExtractionResult result = engine.roll(new ExtractionAttempt(profile, 1, 1, 1, 1, 1L));

        assertThat(result.successful()).isFalse();
        assertThat(result.byproducts()).singleElement()
                .extracting(ReagentStack::reagentId)
                .isEqualTo("zen_atelier:decay_residue");
    }

    @Test
    void invalidProfilesAreRejected() {
        assertThatThrownBy(() -> new ExtractionProfile("", "minecraft:copper_ingot", 1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExtractionOutcome(OutcomeClass.SUCCESS, 0, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ExtractionProfile sampleProfile() {
        return new ExtractionProfile(
                "zen_atelier:test_blaze_powder",
                "minecraft:blaze_powder",
                3,
                List.of(
                        new ExtractionOutcome(
                                OutcomeClass.SUCCESS,
                                90,
                                List.of(ReagentStack.simple("zen_atelier:fire_reagent", 50, 2)),
                                List.of()
                        ),
                        new ExtractionOutcome(
                                OutcomeClass.RECOVERABLE_FAILURE,
                                10,
                                List.of(),
                                List.of(ReagentStack.simple("zen_atelier:ash_residue", 10, 1))
                        )
                )
        );
    }
}
