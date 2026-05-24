package com.sanhiruzu.atelier.space;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Chunk Classification Serialization Tests")
class ChunkClassificationSerializationTest {
    private ChunkClassificationData original;
    private ChunkClassificationData restored;

    @BeforeEach
    void setUp() {
        original = new ChunkClassificationData();
        restored = new ChunkClassificationData();
    }

    @Test
    @DisplayName("Should serialize and deserialize empty data")
    void testSerializeDeserializeEmpty() {
        CompoundTag tag = new CompoundTag();
        original.serializeNBT(tag);

        restored.deserializeNBT(tag);

        assertThat(restored.isDirty()).isEqualTo(original.isDirty());
        assertThat(restored.getRegions()).isEmpty();
    }

    @Test
    @DisplayName("Should preserve all block states after serialization")
    void testSerializeDeserializeBlockStates() {
        // Set various blocks to different states
        original.setBlockState(0, 0, 0, ClassificationState.OUTSIDE);
        original.setBlockState(5, 10, 7, ClassificationState.INSIDE);
        original.setBlockState(15, 319, 15, ClassificationState.PARTIAL);
        original.setBlockState(8, 128, 8, ClassificationState.SOLID);

        CompoundTag tag = new CompoundTag();
        original.serializeNBT(tag);
        restored.deserializeNBT(tag);

        assertThat(restored.getBlockState(0, 0, 0)).isEqualTo(ClassificationState.OUTSIDE);
        assertThat(restored.getBlockState(5, 10, 7)).isEqualTo(ClassificationState.INSIDE);
        assertThat(restored.getBlockState(15, 319, 15)).isEqualTo(ClassificationState.PARTIAL);
        assertThat(restored.getBlockState(8, 128, 8)).isEqualTo(ClassificationState.SOLID);
    }

    @Test
    @DisplayName("Should preserve dirty flag during serialization")
    void testSerializeDeserializeDirtyFlag() {
        original.setDirty(false);

        CompoundTag tag = new CompoundTag();
        original.serializeNBT(tag);
        restored.deserializeNBT(tag);

        assertThat(restored.isDirty()).isFalse();
    }

    @Test
    @DisplayName("Should preserve regions during serialization")
    void testSerializeDeserializeRegions() {
        var region1 = new ClassifiedRegion(100, 50);
        var region2 = new ClassifiedRegion(200, 75);

        original.addRegion(region1);
        original.addRegion(region2);

        CompoundTag tag = new CompoundTag();
        original.serializeNBT(tag);
        restored.deserializeNBT(tag);

        assertThat(restored.getRegions()).hasSize(2);
        // Note: ClassifiedRegion comparison depends on equals() implementation
    }

    @Test
    @DisplayName("Should handle large scale serialization")
    void testSerializeDeserializeLargeScale() {
        // Fill entire Y level with alternating states
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                ClassificationState state = ((x + z) % 2) == 0
                        ? ClassificationState.OUTSIDE
                        : ClassificationState.INSIDE;
                original.setBlockState(x, 64, z, state);
            }
        }

        CompoundTag tag = new CompoundTag();
        original.serializeNBT(tag);
        restored.deserializeNBT(tag);

        // Verify all blocks match
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                ClassificationState expected = ((x + z) % 2) == 0
                        ? ClassificationState.OUTSIDE
                        : ClassificationState.INSIDE;
                assertThat(restored.getBlockState(x, 64, z))
                        .as("Block at (%d, 64, %d)", x, z)
                        .isEqualTo(expected);
            }
        }
    }

    @Test
    @DisplayName("Should not corrupt data with multiple serialization cycles")
    void testMultipleSerialization() {
        original.setBlockState(5, 100, 5, ClassificationState.OUTSIDE);
        original.setDirty(false);

        // First cycle
        CompoundTag tag1 = new CompoundTag();
        original.serializeNBT(tag1);
        restored.deserializeNBT(tag1);

        // Second cycle
        CompoundTag tag2 = new CompoundTag();
        restored.serializeNBT(tag2);
        original.deserializeNBT(tag2);

        // Data should still be correct
        assertThat(original.getBlockState(5, 100, 5)).isEqualTo(ClassificationState.OUTSIDE);
        assertThat(original.isDirty()).isFalse();
    }

    @Test
    @DisplayName("Should preserve data at minimum Y coordinate")
    void testMinimumYCoordinate() {
        original.setBlockState(0, -64, 0, ClassificationState.INSIDE);
        original.setBlockState(15, -64, 15, ClassificationState.PARTIAL);

        CompoundTag tag = new CompoundTag();
        original.serializeNBT(tag);
        restored.deserializeNBT(tag);

        assertThat(restored.getBlockState(0, -64, 0)).isEqualTo(ClassificationState.INSIDE);
        assertThat(restored.getBlockState(15, -64, 15)).isEqualTo(ClassificationState.PARTIAL);
    }

    @Test
    @DisplayName("Should preserve data at maximum Y coordinate")
    void testMaximumYCoordinate() {
        original.setBlockState(0, 319, 0, ClassificationState.INSIDE);
        original.setBlockState(15, 319, 15, ClassificationState.PARTIAL);

        CompoundTag tag = new CompoundTag();
        original.serializeNBT(tag);
        restored.deserializeNBT(tag);

        assertThat(restored.getBlockState(0, 319, 0)).isEqualTo(ClassificationState.INSIDE);
        assertThat(restored.getBlockState(15, 319, 15)).isEqualTo(ClassificationState.PARTIAL);
    }

    @Test
    @DisplayName("Should handle partial bitfield restoration")
    void testPartialBitfieldRestoration() {
        // Set many blocks
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                original.setBlockState(x, 100, z, ClassificationState.OUTSIDE);
            }
        }

        CompoundTag tag = new CompoundTag();
        original.serializeNBT(tag);

        // Create a new instance and restore
        var newData = new ChunkClassificationData();
        newData.deserializeNBT(tag);

        // Verify restoration
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertThat(newData.getBlockState(x, 100, z))
                        .as("Block at (%d, 100, %d)", x, z)
                        .isEqualTo(ClassificationState.OUTSIDE);
            }
        }
    }

    @Test
    @DisplayName("Should handle serialization with mixed data")
    void testSerializeDeserializeMixedData() {
        // Mix of blocks, regions, and dirty state
        original.setBlockState(0, 0, 0, ClassificationState.OUTSIDE);
        original.setBlockState(15, 319, 15, ClassificationState.INSIDE);

        var region = new ClassifiedRegion(500, 250);
        original.addRegion(region);

        original.setDirty(true);

        CompoundTag tag = new CompoundTag();
        original.serializeNBT(tag);
        restored.deserializeNBT(tag);

        assertThat(restored.getBlockState(0, 0, 0)).isEqualTo(ClassificationState.OUTSIDE);
        assertThat(restored.getBlockState(15, 319, 15)).isEqualTo(ClassificationState.INSIDE);
        assertThat(restored.isDirty()).isTrue();
        assertThat(restored.getRegions()).hasSize(1);
    }
}
