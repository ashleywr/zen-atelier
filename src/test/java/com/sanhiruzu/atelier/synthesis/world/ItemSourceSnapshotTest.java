package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.synthesis.core.SourceKey;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemSourceSnapshotTest {
    @Test
    void matchesManualTagSnapshots() {
        ItemSourceSnapshot snapshot = new ItemSourceSnapshot("example:copper_chunk", Set.of("c:ingots/copper"));

        assertThat(snapshot.matches(SourceKey.parse("#c:ingots/copper"))).isTrue();
        assertThat(snapshot.matches(SourceKey.parse("#c:ingots/gold"))).isFalse();
        assertThat(snapshot.matches(SourceKey.parse("example:copper_chunk"))).isTrue();
    }

    @Test
    void rejectsBlankIds() {
        assertThatThrownBy(() -> new ItemSourceSnapshot("", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
