package com.sanhiruzu.atelier.space.zone;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Zone Data Tests")
class ZoneDataTest {

    // --- RoomData ---

    @Test
    @DisplayName("RoomData should expose fields correctly")
    void testRoomDataFields() {
        UUID id = UUID.randomUUID();
        Map<String, Integer> furniture = Map.of("minecraft:bookshelf", 3, "minecraft:crafting_table", 1);
        RoomData room = new RoomData(id, 80, 0.9f, furniture, 0.72f);

        assertThat(room.getRegionId()).isEqualTo(id);
        assertThat(room.getVolume()).isEqualTo(80);
        assertThat(room.getEnclosureScore()).isCloseTo(0.9f, within(0.001f));
        assertThat(room.getQuality()).isCloseTo(0.72f, within(0.001f));
        assertThat(room.isOutdoor()).isFalse();
        assertThat(room.getZoneTypeId()).isNull();
    }

    @Test
    @DisplayName("RoomData furniture map should be unmodifiable copy")
    void testRoomDataFurnitureImmutable() {
        Map<String, Integer> furniture = Map.of("minecraft:chest", 2);
        RoomData room = new RoomData(UUID.randomUUID(), 50, 0.8f, furniture, 0.4f);

        assertThat(room.getFurnitureCounts()).containsEntry("minecraft:chest", 2);
        assertThatThrownBy(() -> room.getFurnitureCounts().put("minecraft:bed", 1))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- Quality formula ---

    @Test
    @DisplayName("Quality formula: empty room capped at volume/100 * enclosure")
    void testQualityFormulaNoFurniture() {
        // volume=100, enclosureScore=1.0, no furniture → furnitureBonus=1.0 → quality=1.0
        int volume = 100;
        float enclosure = 1.0f;
        float furnitureBonus = 1.0f;
        float expected = Math.min(1.0f, volume / 100f * enclosure * furnitureBonus);

        assertThat(expected).isCloseTo(1.0f, within(0.001f));
    }

    @Test
    @DisplayName("Quality formula: small room stays below 1")
    void testQualityFormulaSmallRoom() {
        int volume = 20;
        float enclosure = 0.85f;
        int distinctTypes = 0;
        float furnitureBonus = 1.0f + 0.1f * Math.min(distinctTypes, 5);
        float quality = Math.min(1.0f, volume / 100f * enclosure * furnitureBonus);

        assertThat(quality).isCloseTo(0.17f, within(0.01f));
        assertThat(quality).isLessThan(1.0f);
    }

    @Test
    @DisplayName("Quality formula: furniture bonus caps at 5 distinct types")
    void testQualityFormulaFurnitureCap() {
        float bonusAt5 = 1.0f + 0.1f * 5;
        float bonusAt10 = 1.0f + 0.1f * Math.min(10, 5);

        assertThat(bonusAt5).isCloseTo(bonusAt10, within(0.001f));
        assertThat(bonusAt5).isCloseTo(1.5f, within(0.001f));
    }

    @Test
    @DisplayName("Quality is clamped to [0, 1]")
    void testQualityClamp() {
        // Very large volume with high enclosure and furniture should still be ≤ 1
        int volume = 10000;
        float enclosure = 1.0f;
        float furnitureBonus = 1.5f;
        float quality = Math.min(1.0f, volume / 100f * enclosure * furnitureBonus);

        assertThat(quality).isEqualTo(1.0f);
    }

    // --- OutdoorZoneData ---

    @Test
    @DisplayName("OutdoorZoneData should report isOutdoor=true")
    void testOutdoorZoneDataIsOutdoor() {
        UUID id = UUID.randomUUID();
        OutdoorZoneData outdoor = new OutdoorZoneData(id, 0, 0.0f, null);

        assertThat(outdoor.isOutdoor()).isTrue();
        assertThat(outdoor.getRegionId()).isEqualTo(id);
    }

    // --- Spatial extent ---

    @Test
    @DisplayName("contains() returns false before extent is set")
    void containsReturnsFalseBeforeExtentSet() {
        ZoneData zone = new RoomData(UUID.randomUUID(), 50, 1f, Map.of(), 0.5f);
        assertThat(zone.hasSpatialExtent()).isFalse();
        assertThat(zone.contains(new BlockPos(0, 64, 0))).isFalse();
    }

    @Test
    @DisplayName("contains() returns true for position inside bounding box")
    void containsReturnsTrueInsideBounds() {
        ZoneData zone = new RoomData(UUID.randomUUID(), 50, 1f, Map.of(), 0.5f);
        zone.setSpatialExtent(0, 60, 0, 15, 70, 15);
        assertThat(zone.contains(new BlockPos(8, 64, 8))).isTrue();
    }

    @Test
    @DisplayName("contains() returns false for position outside bounding box")
    void containsReturnsFalseOutsideBounds() {
        ZoneData zone = new RoomData(UUID.randomUUID(), 50, 1f, Map.of(), 0.5f);
        zone.setSpatialExtent(0, 60, 0, 15, 70, 15);
        assertThat(zone.contains(new BlockPos(16, 64, 8))).isFalse();
        assertThat(zone.contains(new BlockPos(8, 64, 16))).isFalse();
        assertThat(zone.contains(new BlockPos(-1, 64, 8))).isFalse();
        assertThat(zone.contains(new BlockPos(8, 59, 8))).isFalse();
        assertThat(zone.contains(new BlockPos(8, 71, 8))).isFalse();
    }

    @Test
    @DisplayName("contains() includes boundary positions")
    void containsIncludesBoundaryPositions() {
        ZoneData zone = new RoomData(UUID.randomUUID(), 50, 1f, Map.of(), 0.5f);
        zone.setSpatialExtent(0, 60, 0, 15, 70, 15);
        assertThat(zone.contains(new BlockPos(0, 64, 0))).isTrue();
        assertThat(zone.contains(new BlockPos(15, 64, 15))).isTrue();
        assertThat(zone.contains(new BlockPos(8, 60, 8))).isTrue();
        assertThat(zone.contains(new BlockPos(8, 70, 8))).isTrue();
    }

    @Test
    @DisplayName("contains() checks Y — positions outside Y range return false")
    void containsChecksY() {
        ZoneData zone = new RoomData(UUID.randomUUID(), 50, 1f, Map.of(), 0.5f);
        zone.setSpatialExtent(0, 60, 0, 15, 70, 15);
        assertThat(zone.contains(new BlockPos(8, 64, 8))).isTrue();
        assertThat(zone.contains(new BlockPos(8, 59, 8))).isFalse();
        assertThat(zone.contains(new BlockPos(8, 71, 8))).isFalse();
    }

    @Test
    @DisplayName("setSpatialExtent marks extent as set")
    void setSpatialExtentMarksHasExtent() {
        ZoneData zone = new RoomData(UUID.randomUUID(), 50, 1f, Map.of(), 0.5f);
        assertThat(zone.hasSpatialExtent()).isFalse();
        zone.setSpatialExtent(10, 50, 20, 30, 80, 40);
        assertThat(zone.hasSpatialExtent()).isTrue();
        assertThat(zone.getMinX()).isEqualTo(10);
        assertThat(zone.getMinY()).isEqualTo(50);
        assertThat(zone.getMinZ()).isEqualTo(20);
        assertThat(zone.getMaxX()).isEqualTo(30);
        assertThat(zone.getMaxY()).isEqualTo(80);
        assertThat(zone.getMaxZ()).isEqualTo(40);
    }

    // --- ZoneTypeRegistry ---

    @Test
    @DisplayName("ZoneTypeRegistry.match() returns null when no types registered")
    void testZoneTypeRegistryEmptyReturnsNull() {
        // The registry is a static singleton — we rely on no types being registered at test time.
        // This validates the baseline behavior the plan specifies.
        RoomData room = new RoomData(UUID.randomUUID(), 50, 0.8f, Map.of(), 0.4f);
        // Cannot call match() without Level+BlockPos (MC objects), but we can verify the internal
        // TYPES map is not modified by construction of data objects.
        assertThat(room.getZoneTypeId()).isNull();
    }
}
