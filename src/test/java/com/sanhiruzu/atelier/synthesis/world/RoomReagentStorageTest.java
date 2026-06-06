package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoomReagentStorageTest {
    @Test
    void consumptionUsesCarriedReagentsBeforeRoomStorage() {
        ReagentContainer carried = new ReagentContainer();
        carried.insert(reagent("zen_atelier:binding_reagent", 10));

        BlockPos storagePos = new BlockPos(1, 2, 3);
        ReagentContainer storage = new ReagentContainer();
        storage.insert(reagent("zen_atelier:binding_reagent", 25));

        var plan = RoomReagentStorage.planConsumption(
                carried,
                Map.of(storagePos, storage),
                List.of(reagent("zen_atelier:binding_reagent", 20))
        );

        assertThat(plan).isPresent();
        assertThat(plan.get().carriedConsumed()).extracting(ReagentStack::amount).containsExactly(10);
        assertThat(plan.get().storageConsumed().get(storagePos)).extracting(ReagentStack::amount).containsExactly(10);
    }

    @Test
    void consumptionFailsWhenCarriedAndRoomStorageCannotSatisfyRequest() {
        ReagentContainer carried = new ReagentContainer();
        carried.insert(reagent("zen_atelier:binding_reagent", 5));

        ReagentContainer storage = new ReagentContainer();
        storage.insert(reagent("zen_atelier:binding_reagent", 5));

        var plan = RoomReagentStorage.planConsumption(
                carried,
                Map.of(new BlockPos(1, 2, 3), storage),
                List.of(reagent("zen_atelier:binding_reagent", 20))
        );

        assertThat(plan).isEmpty();
    }

    private static ReagentStack reagent(String id, int amount) {
        return new ReagentStack(id, amount, 1, 30, 50, 0, Map.of("binding", 1), List.of(), Set.of());
    }
}
