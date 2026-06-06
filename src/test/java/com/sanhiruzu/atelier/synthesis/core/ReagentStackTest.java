package com.sanhiruzu.atelier.synthesis.core;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReagentStackTest {
    @Test
    void copiesMutableInputs() {
        Map<String, Integer> elements = new HashMap<>();
        elements.put("fire", 2);
        List<String> traits = new ArrayList<>(List.of("volatile"));
        Set<String> hints = new HashSet<>(Set.of("minecraft:blaze_powder"));

        ReagentStack stack = new ReagentStack(
                "zen_atelier:fire_reagent",
                100,
                3,
                50,
                60,
                20,
                elements,
                traits,
                hints
        );

        elements.put("water", 99);
        traits.add("soothing");
        hints.add("minecraft:honey_bottle");

        assertThat(stack.elements()).containsOnly(Map.entry("fire", 2));
        assertThat(stack.traits()).containsExactly("volatile");
        assertThat(stack.sourceHints()).containsExactly("minecraft:blaze_powder");
    }

    @Test
    void clampsBoundedFields() {
        ReagentStack stack = new ReagentStack(
                "zen_atelier:unstable_reagent",
                1,
                99,
                150,
                -20,
                120,
                Map.of(),
                List.of(),
                Set.of()
        );

        assertThat(stack.tier()).isEqualTo(6);
        assertThat(stack.quality()).isEqualTo(100);
        assertThat(stack.purity()).isZero();
        assertThat(stack.instability()).isEqualTo(100);
    }

    @Test
    void rejectsBlankIdAndNonPositiveAmount() {
        assertThatThrownBy(() -> ReagentStack.simple("", 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReagentStack.simple("zen_atelier:test", 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsLegacyStacksToSingleShape() {
        ReagentStack stack = new ReagentStack(
                "zen_atelier:legacy_reagent",
                1,
                1,
                0,
                0,
                0,
                Map.of(),
                List.of(),
                Set.of()
        );

        assertThat(stack.shape()).isEqualTo(ReagentShape.SINGLE);
    }

    @Test
    void serializesShapeInReagentComponentPayload() {
        ReagentStack stack = new ReagentStack(
                "zen_atelier:shaped_reagent",
                Set.of("zen_atelier:conductive", "zen_atelier:abrasive"),
                1,
                2,
                50,
                60,
                0,
                Map.of("fire", 2),
                List.of("destruction"),
                ReagentShape.ELBOW,
                Set.of("prototype")
        );

        ReagentStack decoded = ReagentStack.CODEC.encodeStart(JsonOps.INSTANCE, stack)
                .flatMap(json -> ReagentStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json.toString())))
                .result()
                .orElseThrow();

        assertThat(decoded.shape()).isEqualTo(ReagentShape.ELBOW);
        assertThat(decoded.categories()).containsExactlyInAnyOrder("zen_atelier:conductive", "zen_atelier:abrasive");
    }

    @Test
    void rotatesAndNormalizesShapeCells() {
        assertThat(ReagentShape.ELBOW.rotated(1))
                .containsExactly(
                        new ReagentShape.Cell(1, 0),
                        new ReagentShape.Cell(0, 1),
                        new ReagentShape.Cell(1, 1)
                );
    }
}
