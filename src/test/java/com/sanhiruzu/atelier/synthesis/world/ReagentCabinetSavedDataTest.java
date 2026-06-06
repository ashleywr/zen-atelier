package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainerSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReagentCabinetSavedDataTest {
    @Test
    void roundTripsSnapshotWithReagentMetadata() {
        ReagentStack stack = new ReagentStack(
                "zen_atelier:verdant_reagent",
                24,
                3,
                82,
                64,
                12,
                Map.of("plant", 3, "water", 1),
                List.of("fresh", "volatile"),
                Set.of("minecraft:honey_bottle", "#minecraft:flowers")
        );
        ReagentContainerSnapshot snapshot = new ReagentContainerSnapshot(List.of(stack));

        CompoundTag tag = ReagentCabinetSavedData.saveSnapshot(snapshot);
        ReagentContainerSnapshot loaded = ReagentCabinetSavedData.loadSnapshot(tag);

        assertThat(loaded.entries()).containsExactly(stack);
    }

    @Test
    void roundTripsCabinetsByPosition() {
        ReagentCabinetSavedData data = new ReagentCabinetSavedData();
        BlockPos pos = new BlockPos(7, 65, -11);
        ReagentContainer container = new ReagentContainer();
        container.insert(ReagentStack.simple("zen_atelier:abrasive_reagent", 35, 2));

        data.putContainer(pos, container);
        CompoundTag saved = data.save(new CompoundTag(), null);
        ReagentCabinetSavedData loaded = ReagentCabinetSavedData.load(saved, null);

        assertThat(loaded.getSnapshot(pos).entries())
                .containsExactly(ReagentStack.simple("zen_atelier:abrasive_reagent", 35, 2));
        assertThat(loaded.getSnapshot(new BlockPos(0, 64, 0)).entries()).isEmpty();
    }

    @Test
    void loadSnapshotSkipsInvalidEntries() {
        CompoundTag snapshot = new CompoundTag();
        ListTag entries = new ListTag();
        CompoundTag invalid = new CompoundTag();
        invalid.putString("reagent", "zen_atelier:broken");
        invalid.putInt("amount", 0);
        invalid.putInt("tier", 1);
        entries.add(invalid);
        snapshot.put("entries", entries);

        ReagentContainerSnapshot loaded = ReagentCabinetSavedData.loadSnapshot(snapshot);

        assertThat(loaded.entries()).isEmpty();
    }
}
