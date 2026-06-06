package com.sanhiruzu.atelier.synthesis.core;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceKeyTest {
    @Test
    void exactSourceMatchesItemIdOnly() {
        SourceKey source = SourceKey.parse("minecraft:copper_ingot");

        assertThat(source.tag()).isFalse();
        assertThat(source.matches("minecraft:copper_ingot", Set.of("#c:ingots/copper"))).isTrue();
        assertThat(source.matches("minecraft:gold_ingot", Set.of("#c:ingots/copper"))).isFalse();
    }

    @Test
    void tagSourceMatchesTagsWithOrWithoutPrefix() {
        SourceKey source = SourceKey.parse("#c:ingots/copper");

        assertThat(source.tag()).isTrue();
        assertThat(source.id()).isEqualTo("c:ingots/copper");
        assertThat(source.matches("minecraft:copper_ingot", Set.of("#c:ingots/copper"))).isTrue();
        assertThat(source.matches("minecraft:copper_ingot", Set.of("c:ingots/copper"))).isTrue();
        assertThat(source.matches("minecraft:copper_ingot", Set.of("c:ingots/gold"))).isFalse();
    }

    @Test
    void rejectsBlankSource() {
        assertThatThrownBy(() -> SourceKey.parse(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SourceKey.parse("#"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
