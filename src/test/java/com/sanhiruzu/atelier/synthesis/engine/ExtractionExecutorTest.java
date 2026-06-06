package com.sanhiruzu.atelier.synthesis.engine;

import com.sanhiruzu.atelier.synthesis.core.ApparatusState;
import com.sanhiruzu.atelier.synthesis.core.AttemptContext;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.RoomAlchemyContext;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionExecutorTest {
    private final ExtractionExecutor executor = new ExtractionExecutor();

    @Test
    void depositsRolledReagentsIntoTargetContainer() {
        ReagentContainer target = new ReagentContainer();

        ExtractionExecutionResult execution = executor.execute(successProfile(), 3, target, context(), 1L);

        assertThat(execution.consumedSourceAmount()).isEqualTo(3);
        assertThat(execution.depositedReagents()).singleElement()
                .extracting(ReagentStack::amount)
                .isEqualTo(90);
        assertThat(target.totalAmount(ReagentQuery.any())).isEqualTo(90);
    }

    @Test
    void depositsFailureByproductsIntoTargetContainer() {
        ReagentContainer target = new ReagentContainer();

        ExtractionExecutionResult execution = executor.execute(failureProfile(), 2, target, context(), 1L);

        assertThat(execution.result().successful()).isFalse();
        assertThat(execution.depositedReagents()).singleElement()
                .extracting(ReagentStack::reagentId)
                .isEqualTo("zen_atelier:ash_residue");
        assertThat(target.totalAmount(ReagentQuery.any())).isEqualTo(10);
    }

    @Test
    void contextCapsDepositedReagentTier() {
        ReagentContainer target = new ReagentContainer();
        AttemptContext lowCap = new AttemptContext(
                new ApparatusState("zen_atelier:crude_extractor", 1, 0),
                RoomAlchemyContext.none(),
                6,
                0
        );

        executor.execute(successProfile(), 1, target, lowCap, 1L);

        assertThat(target.entries()).singleElement()
                .extracting(ReagentStack::tier)
                .isEqualTo(1);
    }

    private static ExtractionProfile successProfile() {
        return new ExtractionProfile(
                "zen_atelier:test_flint",
                "minecraft:flint",
                2,
                List.of(new ExtractionOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(ReagentStack.simple("zen_atelier:abrasive_reagent", 30, 2)),
                        List.of()
                ))
        );
    }

    private static ExtractionProfile failureProfile() {
        return new ExtractionProfile(
                "zen_atelier:test_failure",
                "minecraft:flint",
                2,
                List.of(new ExtractionOutcome(
                        OutcomeClass.RECOVERABLE_FAILURE,
                        1,
                        List.of(),
                        List.of(ReagentStack.simple("zen_atelier:ash_residue", 5, 1))
                ))
        );
    }

    private static AttemptContext context() {
        return new AttemptContext(
                new ApparatusState("zen_atelier:copper_extractor", 2, 0),
                RoomAlchemyContext.none(),
                2,
                0
        );
    }
}
