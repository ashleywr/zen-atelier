package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ApparatusState;
import com.sanhiruzu.atelier.synthesis.core.AttemptContext;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.RoomAlchemyContext;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SynthesisEngineTest {
    private final SynthesisEngine engine = new SynthesisEngine();

    @Test
    void sameSeedProducesSameResult() {
        SynthesisAttempt attempt = new SynthesisAttempt(sampleProfile(), sampleInputs(), 6, 6, 6, 42L);

        SynthesisResult first = engine.roll(attempt);
        SynthesisResult second = engine.roll(attempt);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void traceIncludesRisk() {
        SynthesisResult result = engine.roll(new SynthesisAttempt(sampleProfile(), sampleInputs(), 6, 6, 6, 45, 42L));

        assertThat(result.trace().lines()).contains("risk 45");
        assertThat(result.trace().lines()).anySatisfy(line -> assertThat(line).startsWith("success probability "));
    }

    @Test
    void canBuildAttemptFromContext() {
        AttemptContext context = new AttemptContext(
                new ApparatusState("zen_atelier:copper_cauldron", 3, 10),
                new RoomAlchemyContext("zen_atelier:atelier", 4, 50, 0, 25, Map.of(), Set.of()),
                5,
                20
        );

        SynthesisResult result = engine.roll(new SynthesisAttempt(sampleProfile(), sampleInputs(), context, 42L));

        assertThat(result.effectiveTierCap()).isEqualTo(3);
        assertThat(result.trace().lines()).contains("risk 35");
    }

    @Test
    void validatesRequiredReagentAmounts() {
        SynthesisProfile profile = sampleProfile();
        List<ReagentStack> insufficient = List.of(fireReagent(20, 3, List.of("volatile")));

        assertThatThrownBy(() -> engine.roll(new SynthesisAttempt(profile, insufficient, 6, 6, 6, 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required reagents");
    }

    @Test
    void validationDoesNotReuseSameReagentForOverlappingRequirements() {
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:test_overcommit",
                List.of(
                        new SynthesisRequirement(ReagentQuery.any(), 50),
                        new SynthesisRequirement(ReagentQuery.any(), 50)
                ),
                3,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:impossible", 1, 1, 1, List.of())),
                        List.of()
                ))
        );

        assertThatThrownBy(() -> engine.roll(new SynthesisAttempt(
                profile,
                List.of(fireReagent(60, 3, List.of("volatile"))),
                3,
                3,
                3,
                1L
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required reagents");
    }

    @Test
    void inputTierCapsOutputEvenWhenRecipeAndRoomAllowMore() {
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:test_coating",
                List.of(new SynthesisRequirement(ReagentQuery.any(), 10)),
                6,
                List.of(new SynthesisOutcome(
                        OutcomeClass.PERFECT_SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:perfect_tool_coating", 1, 6, 90, List.of("expansive"))),
                        List.of()
                ))
        );

        SynthesisResult result = engine.roll(new SynthesisAttempt(
                profile,
                List.of(fireReagent(10, 2, List.of("volatile"))),
                6,
                6,
                6,
                1L
        ));

        assertThat(result.effectiveTierCap()).isEqualTo(2);
        assertThat(result.outputs()).singleElement()
                .extracting(SynthesisOutput::tier)
                .isEqualTo(2);
    }

    @Test
    void apparatusCapLimitsOutput() {
        SynthesisResult result = engine.roll(new SynthesisAttempt(sampleProfile(), sampleInputs(), 2, 6, 6, 1L));

        assertThat(result.effectiveTierCap()).isEqualTo(2);
        assertThat(result.outputs()).singleElement()
                .extracting(SynthesisOutput::tier)
                .isEqualTo(2);
    }

    @Test
    void failureOutcomeCanReturnByproducts() {
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:test_failure",
                List.of(new SynthesisRequirement(ReagentQuery.any(), 10)),
                3,
                List.of(new SynthesisOutcome(
                        OutcomeClass.RECOVERABLE_FAILURE,
                        1,
                        List.of(),
                        List.of(ReagentStack.simple("zen_atelier:unstable_residue", 5, 1))
                ))
        );

        SynthesisResult result = engine.roll(new SynthesisAttempt(profile, sampleInputs(), 3, 3, 3, 1L));

        assertThat(result.successful()).isFalse();
        assertThat(result.byproducts()).singleElement()
                .extracting(ReagentStack::reagentId)
                .isEqualTo("zen_atelier:unstable_residue");
    }

    @Test
    void rejectsInvalidProfile() {
        assertThatThrownBy(() -> new SynthesisProfile("", List.of(), 1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SynthesisRequirement(ReagentQuery.any(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SynthesisOutput("", 1, 1, 1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SynthesisOutcome(OutcomeClass.SUCCESS, 0, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SynthesisProfile sampleProfile() {
        return new SynthesisProfile(
                "zen_atelier:test_tool_coating",
                List.of(new SynthesisRequirement(ReagentQuery.builder()
                        .requiredTraits(Set.of("volatile"))
                        .minElements(Map.of("fire", 2))
                        .build(), 50)),
                4,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:heated_tool_coating", 1, 4, 75, List.of("swift"))),
                        List.of()
                ))
        );
    }

    private static List<ReagentStack> sampleInputs() {
        return List.of(fireReagent(60, 4, List.of("volatile")));
    }

    private static ReagentStack fireReagent(int amount, int tier, List<String> traits) {
        return new ReagentStack(
                "zen_atelier:fire_reagent",
                amount,
                tier,
                70,
                65,
                20,
                Map.of("fire", tier),
                traits,
                Set.of("minecraft:blaze_powder")
        );
    }
}
