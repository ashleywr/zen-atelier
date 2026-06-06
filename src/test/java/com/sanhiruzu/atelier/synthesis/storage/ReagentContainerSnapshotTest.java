package com.sanhiruzu.atelier.synthesis.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReagentContainerSnapshotTest {
    @Test
    void snapshotCopiesContainerEntries() {
        ReagentContainer container = new ReagentContainer();
        container.insert(ReagentStack.simple("zen_atelier:test", 10, 1));

        ReagentContainerSnapshot snapshot = ReagentContainerSnapshot.fromContainer(container);
        container.insert(ReagentStack.simple("zen_atelier:test", 5, 1));

        assertThat(snapshot.entries()).singleElement()
                .extracting(ReagentStack::amount)
                .isEqualTo(10);
    }

    @Test
    void snapshotEntriesAreImmutable() {
        ReagentContainerSnapshot snapshot = new ReagentContainerSnapshot(List.of(ReagentStack.simple("zen_atelier:test", 10, 1)));

        assertThatThrownBy(() -> snapshot.entries().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void restoresContainerAndMergesIdenticalProfiles() {
        ReagentContainerSnapshot snapshot = new ReagentContainerSnapshot(List.of(
                ReagentStack.simple("zen_atelier:test", 10, 1),
                ReagentStack.simple("zen_atelier:test", 15, 1)
        ));

        ReagentContainer restored = snapshot.toContainer();

        assertThat(restored.entries()).singleElement()
                .extracting(ReagentStack::amount)
                .isEqualTo(25);
    }

    @Test
    void codecRoundTripsSnapshotEntries() {
        ReagentContainerSnapshot snapshot = new ReagentContainerSnapshot(List.of(new ReagentStack(
                "zen_atelier:fire_reagent",
                25,
                2,
                40,
                70,
                15,
                Map.of("fire", 2),
                List.of("zen_atelier:volatile"),
                Set.of("minecraft:blaze_powder")
        )));

        JsonElement encoded = ReagentContainerSnapshot.CODEC.encodeStart(JsonOps.INSTANCE, snapshot).getOrThrow();
        ReagentContainerSnapshot decoded = ReagentContainerSnapshot.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertThat(decoded).isEqualTo(snapshot);
    }

    @Test
    void codecRejectsInvalidStoredReagentNumbers() {
        JsonObject entry = new JsonObject();
        entry.addProperty("reagent", "zen_atelier:bad");
        entry.addProperty("amount", 1);
        entry.addProperty("tier", 9);
        JsonObject root = new JsonObject();
        root.add("entries", com.google.gson.JsonParser.parseString("[]"));
        root.getAsJsonArray("entries").add(entry);

        assertThat(ReagentContainerSnapshot.CODEC.parse(JsonOps.INSTANCE, root).error()).isPresent();
    }
}
