package com.sanhiruzu.atelier.space.zone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ZoneAtmosphere.fromSignals")
class ZoneAtmosphereTest {

    // ---- temperature offset ----

    @Test
    @DisplayName("No signals, zero enclosure → temperature offset is 0")
    void noSignalsNoEnclosureIsNeutral() {
        ZoneAtmosphere atm = ZoneAtmosphere.fromSignals(Map.of(), 0f);
        assertThat(atm.temperatureOffset()).isEqualTo(0);
    }

    @Test
    @DisplayName("Enclosure alone provides indoor baseline warmth")
    void enclosureAddsIndoorBaseline() {
        ZoneAtmosphere atm = ZoneAtmosphere.fromSignals(Map.of(), 1.0f);
        assertThat(atm.temperatureOffset()).isEqualTo(12);
    }

    @Test
    @DisplayName("Partial enclosure gives proportional baseline")
    void partialEnclosureGivesPartialBaseline() {
        ZoneAtmosphere half = ZoneAtmosphere.fromSignals(Map.of(), 0.5f);
        ZoneAtmosphere full = ZoneAtmosphere.fromSignals(Map.of(), 1.0f);
        assertThat(half.temperatureOffset()).isLessThan(full.temperatureOffset());
        assertThat(half.temperatureOffset()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Single heat_source adds positive offset")
    void singleHeatSourceWarmsRoom() {
        ZoneAtmosphere without = ZoneAtmosphere.fromSignals(Map.of(), 0f);
        ZoneAtmosphere with    = ZoneAtmosphere.fromSignals(Map.of("heat_source", 1), 0f);
        assertThat(with.temperatureOffset()).isGreaterThan(without.temperatureOffset());
    }

    @Test
    @DisplayName("Single ice_block adds negative offset")
    void singleIceBlockCoolsRoom() {
        ZoneAtmosphere without = ZoneAtmosphere.fromSignals(Map.of(), 0f);
        ZoneAtmosphere with    = ZoneAtmosphere.fromSignals(Map.of("ice_block", 1), 0f);
        assertThat(with.temperatureOffset()).isLessThan(without.temperatureOffset());
    }

    @Test
    @DisplayName("Heat cap: 20 heat_source blocks do not exceed +60 heat contribution")
    void heatContributionIsCapped() {
        // 20 heat sources × 6 = 120 raw, capped at 60; plus enclosure baseline = 0
        ZoneAtmosphere atm = ZoneAtmosphere.fromSignals(Map.of("heat_source", 20), 0f);
        assertThat(atm.temperatureOffset()).isEqualTo(60);
    }

    @Test
    @DisplayName("Cold cap: 20 ice blocks do not exceed -50 cold contribution")
    void coldContributionIsCapped() {
        // 20 ice blocks × 8 = 160 raw, capped at 50; minus enclosure = 0
        ZoneAtmosphere atm = ZoneAtmosphere.fromSignals(Map.of("ice_block", 20), 0f);
        assertThat(atm.temperatureOffset()).isEqualTo(-50);
    }

    @Test
    @DisplayName("smithing_or_repair_block contributes more heat than cooking_block per block")
    void smithingContributesMoreThanCooking() {
        ZoneAtmosphere smithy   = ZoneAtmosphere.fromSignals(Map.of("smithing_or_repair_block", 1), 0f);
        ZoneAtmosphere kitchen  = ZoneAtmosphere.fromSignals(Map.of("cooking_block", 1), 0f);
        assertThat(smithy.temperatureOffset()).isGreaterThan(kitchen.temperatureOffset());
    }

    @Test
    @DisplayName("Ice and heat cancel when balanced")
    void heatAndIceCancelWhenBalanced() {
        // 1 smithing block (+10 raw) vs enough ice to cancel: ceiling(10/8)=2 ice blocks (-16) → net -6
        ZoneAtmosphere atm = ZoneAtmosphere.fromSignals(
                Map.of("smithing_or_repair_block", 1, "ice_block", 2), 0f);
        assertThat(atm.temperatureOffset()).isLessThan(0);
    }

    @Test
    @DisplayName("Realistic smithy: blast furnace + enclosure produces warm room")
    void realisticSmithyIsWarm() {
        ZoneAtmosphere smithy = ZoneAtmosphere.fromSignals(
                Map.of("smithing_or_repair_block", 2, "heat_source", 2, "cooking_block", 1),
                0.85f);
        assertThat(smithy.temperatureOffset()).isGreaterThan(20);
    }

    @Test
    @DisplayName("Realistic ice storage: many ice blocks produce cold room despite enclosure")
    void realisticIceStorageIsCold() {
        ZoneAtmosphere icebox = ZoneAtmosphere.fromSignals(
                Map.of("ice_block", 10),
                0.9f);
        assertThat(icebox.temperatureOffset()).isLessThan(0);
    }

    // ---- humidity ----

    @Test
    @DisplayName("No signals produces baseline humidity (around 0.3)")
    void noSignalsHasBaselineHumidity() {
        ZoneAtmosphere atm = ZoneAtmosphere.fromSignals(Map.of(), 0f);
        assertThat(atm.humidity()).isCloseTo(0.3f, within(0.001f));
    }

    @Test
    @DisplayName("Water coverage increases humidity above baseline")
    void waterCoverageIncreasesHumidity() {
        ZoneAtmosphere dry = ZoneAtmosphere.fromSignals(Map.of(), 0f);
        ZoneAtmosphere wet = ZoneAtmosphere.fromSignals(Map.of("water_coverage", 10), 0f);
        assertThat(wet.humidity()).isGreaterThan(dry.humidity());
    }

    @Test
    @DisplayName("Plants increase humidity above baseline")
    void plantsIncreaseHumidity() {
        ZoneAtmosphere dry    = ZoneAtmosphere.fromSignals(Map.of(), 0f);
        ZoneAtmosphere plants = ZoneAtmosphere.fromSignals(Map.of("plant", 5), 0f);
        assertThat(plants.humidity()).isGreaterThan(dry.humidity());
    }

    @Test
    @DisplayName("Humidity never exceeds 1.0 regardless of signal counts")
    void humidityIsCappedAtOne() {
        ZoneAtmosphere atm = ZoneAtmosphere.fromSignals(
                Map.of("water_coverage", 500, "plant", 500), 1.0f);
        assertThat(atm.humidity()).isLessThanOrEqualTo(1.0f);
    }

    @Test
    @DisplayName("Humidity is always at least the baseline (0.3)")
    void humidityIsAtLeastBaseline() {
        // Even a very cold dry room should have the indoor baseline humidity
        ZoneAtmosphere atm = ZoneAtmosphere.fromSignals(Map.of("ice_block", 20), 0f);
        assertThat(atm.humidity()).isGreaterThanOrEqualTo(0.3f);
    }

    // ---- air quality ----

    @Test
    @DisplayName("Sealed room with no fire sources has high air quality")
    void sealedRoomWithNoSourcesHasHighAirQuality() {
        ZoneAtmosphere atm = ZoneAtmosphere.fromSignals(Map.of(), 1.0f);
        assertThat(atm.airQuality()).isGreaterThan(0.9f);
    }

    @Test
    @DisplayName("Many heat sources reduce air quality (smoke)")
    void manyHeatSourcesReduceAirQuality() {
        ZoneAtmosphere clean = ZoneAtmosphere.fromSignals(Map.of(), 0.9f);
        ZoneAtmosphere smoky = ZoneAtmosphere.fromSignals(
                Map.of("heat_source", 10, "cooking_block", 5), 0.9f);
        assertThat(smoky.airQuality()).isLessThan(clean.airQuality());
    }

    @Test
    @DisplayName("Air quality never drops below 0.1")
    void airQualityFloorIsRespected() {
        ZoneAtmosphere atm = ZoneAtmosphere.fromSignals(
                Map.of("heat_source", 100, "cooking_block", 100, "smithing_or_repair_block", 100),
                0f);
        assertThat(atm.airQuality()).isGreaterThanOrEqualTo(0.1f);
    }

    @Test
    @DisplayName("Air quality never exceeds 1.0")
    void airQualityCapIsRespected() {
        ZoneAtmosphere atm = ZoneAtmosphere.fromSignals(Map.of(), 1.0f);
        assertThat(atm.airQuality()).isLessThanOrEqualTo(1.0f);
    }

    // ---- NEUTRAL constant ----

    @Test
    @DisplayName("NEUTRAL has zero temperature offset, mid humidity, perfect air quality")
    void neutralConstantValues() {
        assertThat(ZoneAtmosphere.NEUTRAL.temperatureOffset()).isEqualTo(0);
        assertThat(ZoneAtmosphere.NEUTRAL.humidity()).isCloseTo(0.5f, within(0.001f));
        assertThat(ZoneAtmosphere.NEUTRAL.airQuality()).isCloseTo(1.0f, within(0.001f));
    }
}
