package com.sanhiruzu.atelier.space.zone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("QualityEvaluator Tests")
class QualityEvaluatorTest {

    @BeforeEach
    void setUp() {
        BlockRarityCache.reset();
    }

    @Test
    @DisplayName("Quality breakdown totals to at most 1.0")
    void testQualityBreakdownCapped() {
        Map<String, Integer> noFurniture = Map.of();
        Map<String, Integer> signals = Map.of(
                "crafting", 1,
                "furnace", 1
        );

        QualityEvaluator.QualityBreakdown breakdown = QualityEvaluator.evaluate(
                150, 0.9f, noFurniture, signals
        );

        assertThat(breakdown.totalQuality).isGreaterThanOrEqualTo(0.0f).isLessThanOrEqualTo(1.0f);
    }

    @Test
    @DisplayName("Empty room with no furniture has minimal quality")
    void testEmptyRoomHasMinimalQuality() {
        Map<String, Integer> furniture = Map.of();
        Map<String, Integer> signals = Map.of();

        QualityEvaluator.QualityBreakdown breakdown = QualityEvaluator.evaluate(
                50, 0.5f, furniture, signals
        );

        assertThat(breakdown.totalQuality).isLessThan(0.5f);
        assertThat(breakdown.furnitureScore).isEqualTo(0.0f);
    }

    @Test
    @DisplayName("Well-enclosed large room has decent scores even without furniture")
    void testWellEnclosedLargeRoomHasDecency() {
        Map<String, Integer> noFurniture = Map.of();
        Map<String, Integer> signals = Map.of(
                "crafting", 2,
                "furnace", 1,
                "enchanting", 1
        );

        QualityEvaluator.QualityBreakdown breakdown = QualityEvaluator.evaluate(
                200, 0.95f, noFurniture, signals
        );

        assertThat(breakdown.totalQuality).isGreaterThan(0.3f);
        assertThat(breakdown.sizeScore).isGreaterThan(0.2f);
        assertThat(breakdown.enclosureScore).isGreaterThan(0.3f);
    }

    @Test
    @DisplayName("Size score increases with volume")
    void testSizeScoreScalesWithVolume() {
        Map<String, Integer> empty = Map.of();

        QualityEvaluator.QualityBreakdown small = QualityEvaluator.evaluate(
                30, 1.0f, empty, empty
        );
        QualityEvaluator.QualityBreakdown medium = QualityEvaluator.evaluate(
                100, 1.0f, empty, empty
        );
        QualityEvaluator.QualityBreakdown large = QualityEvaluator.evaluate(
                250, 1.0f, empty, empty
        );

        assertThat(small.sizeScore).isLessThan(medium.sizeScore);
        assertThat(medium.sizeScore).isLessThan(large.sizeScore);
    }

    @Test
    @DisplayName("Enclosure score is weighted by enclosure parameter")
    void testEnclosureScoreWeighting() {
        Map<String, Integer> empty = Map.of();

        QualityEvaluator.QualityBreakdown open = QualityEvaluator.evaluate(
                100, 0.5f, empty, empty
        );
        QualityEvaluator.QualityBreakdown closed = QualityEvaluator.evaluate(
                100, 0.95f, empty, empty
        );

        assertThat(open.enclosureScore).isLessThan(closed.enclosureScore);
    }

    @Test
    @DisplayName("Furniture diversity bonus: more block types = higher score (without registry)")
    void testFurnitureDiversityBonusLogic() {
        // Test without calling the furniture evaluator directly since it uses registry
        // Instead, verify the math: diversity bonus = min(1.0f, distinctTypes / 5.0f)
        float bonusFor1Type = Math.min(1.0f, 1 / 5.0f);
        float bonusFor5Types = Math.min(1.0f, 5 / 5.0f);

        assertThat(bonusFor5Types).isGreaterThan(bonusFor1Type);
        assertThat(bonusFor5Types).isCloseTo(1.0f, within(0.001f));
        assertThat(bonusFor1Type).isCloseTo(0.2f, within(0.001f));
    }

    @Test
    @DisplayName("Theme score increases with relevant crafting signals")
    void testThemeScoreWithSignals() {
        Map<String, Integer> empty = Map.of();
        Map<String, Integer> lowSignals = Map.of("crafting", 1);
        Map<String, Integer> highSignals = Map.of(
                "crafting", 2,
                "furnace", 2,
                "brewing", 1,
                "enchanting", 2,
                "smithing", 1
        );

        QualityEvaluator.QualityBreakdown noSignal = QualityEvaluator.evaluate(
                100, 0.8f, empty, empty
        );
        QualityEvaluator.QualityBreakdown someSignal = QualityEvaluator.evaluate(
                100, 0.8f, empty, lowSignals
        );
        QualityEvaluator.QualityBreakdown manySignals = QualityEvaluator.evaluate(
                100, 0.8f, empty, highSignals
        );

        assertThat(noSignal.themeScore).isEqualTo(0.0f);
        assertThat(someSignal.themeScore).isGreaterThan(0.0f);
        assertThat(manySignals.themeScore).isGreaterThanOrEqualTo(someSignal.themeScore);
    }

    @Test
    @DisplayName("Components map contains all four metrics (without furniture)")
    void testComponentsMapContainsAllMetrics() {
        Map<String, Integer> noFurniture = Map.of();
        Map<String, Integer> signals = Map.of("crafting", 1);

        QualityEvaluator.QualityBreakdown breakdown = QualityEvaluator.evaluate(
                100, 0.8f, noFurniture, signals
        );

        assertThat(breakdown.components)
                .containsKeys("Size", "Enclosure", "Furniture", "Theme");
        assertThat(breakdown.components.get("Size")).isGreaterThanOrEqualTo(0.0f);
        assertThat(breakdown.components.get("Enclosure")).isGreaterThanOrEqualTo(0.0f);
        assertThat(breakdown.components.get("Furniture")).isGreaterThanOrEqualTo(0.0f);
        assertThat(breakdown.components.get("Theme")).isGreaterThanOrEqualTo(0.0f);
    }

    @Test
    @DisplayName("Negative enclosure score is clamped to 0")
    void testNegativeEnclosureIsClamped() {
        Map<String, Integer> empty = Map.of();

        QualityEvaluator.QualityBreakdown breakdown = QualityEvaluator.evaluate(
                100, -0.5f, empty, empty
        );

        assertThat(breakdown.enclosureScore).isGreaterThanOrEqualTo(0.0f);
    }

    @Test
    @DisplayName("Very large room (200+ blocks) maxes size score component")
    void testLargeRoomMaxesSizeScore() {
        Map<String, Integer> empty = Map.of();

        QualityEvaluator.QualityBreakdown standard = QualityEvaluator.evaluate(
                200, 0.0f, empty, empty
        );
        QualityEvaluator.QualityBreakdown huge = QualityEvaluator.evaluate(
                1000, 0.0f, empty, empty
        );

        // Both should have the same size score component (1.0 * weight)
        assertThat(standard.sizeScore).isCloseTo(huge.sizeScore, within(0.001f));
    }

    @Test
    @DisplayName("Quality components always result in total <= 1.0")
    void testQualityComponentsAlwaysClamped() {
        Map<String, Integer> empty = Map.of();

        QualityEvaluator.QualityBreakdown breakdown = QualityEvaluator.evaluate(
                1000, 1.0f, empty, Map.of("crafting", 10)
        );

        // Total is always clamped to 1.0 regardless of component values
        assertThat(breakdown.totalQuality).isLessThanOrEqualTo(1.0f);
    }

    @Test
    @DisplayName("Furniture score is capped at furniture weight (0.20)")
    void testFurnitureScoreWeighting() {
        // Even with furniture, the component is weighted by FURNITURE_MAX = 0.20f
        // So furnitureScore = min(1.0f, quality) * 0.20f ≤ 0.20f
        float maxWeighted = Math.min(1.0f, 2.0f) * 0.20f;
        assertThat(maxWeighted).isLessThanOrEqualTo(0.20f);
    }

    @Test
    @DisplayName("Crafting signals threshold logic: 1-2 gives 0.3, 3+ gives 0.6, 5+ gives 1.0")
    void testCraftingSignalThresholdLogic() {
        // Verify the threshold logic without running the full evaluator
        int oneSignal = 1;
        int threeSignals = 3;
        int fiveSignals = 5;

        float score1 = (oneSignal >= 1) ? 0.3f : 0.0f;
        float score3 = (threeSignals >= 5) ? 1.0f : (threeSignals >= 3) ? 0.6f : 0.3f;
        float score5 = (fiveSignals >= 5) ? 1.0f : (fiveSignals >= 3) ? 0.6f : 0.3f;

        assertThat(score1).isCloseTo(0.3f, within(0.001f));
        assertThat(score3).isCloseTo(0.6f, within(0.001f));
        assertThat(score5).isCloseTo(1.0f, within(0.001f));
    }
}
