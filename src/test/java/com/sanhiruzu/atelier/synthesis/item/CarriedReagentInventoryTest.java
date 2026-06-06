package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CarriedReagentInventoryTest {
    @Test
    void consumePlanRemovesWholeAndPartialReagentItems() {
        ReagentStack first = reagent("zen_atelier:binding_reagent", 20);
        ReagentStack second = reagent("zen_atelier:binding_reagent", 15);

        Optional<List<ReagentStack>> remaining = CarriedReagentInventory.remainingAfterConsume(
                List.of(first, second),
                List.of(first.withAmount(25))
        );

        assertThat(remaining).isPresent();
        assertThat(remaining.get().get(0)).isNull();
        assertThat(remaining.get().get(1)).isEqualTo(second.withAmount(10));
    }

    @Test
    void consumePlanIsAllOrNothingWhenMissingAmount() {
        ReagentStack reagent = reagent("zen_atelier:binding_reagent", 20);

        Optional<List<ReagentStack>> remaining = CarriedReagentInventory.remainingAfterConsume(
                List.of(reagent),
                List.of(reagent.withAmount(25))
        );

        assertThat(remaining).isEmpty();
    }

    private static ReagentStack reagent(String id, int amount) {
        return new ReagentStack(
                id,
                amount,
                1,
                20,
                50,
                10,
                Map.of("binding", 1),
                List.of("zen_atelier:binding"),
                Set.of("minecraft:honey_bottle")
        );
    }
}
