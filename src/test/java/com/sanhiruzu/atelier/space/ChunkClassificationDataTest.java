package com.sanhiruzu.atelier.space;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Chunk Classification Data Tests")
class ChunkClassificationDataTest {
    private ChunkClassificationData data;

    @BeforeEach
    void setUp() {
        data = new ChunkClassificationData();
    }

    @Test
    @DisplayName("Should initialize with all blocks as SOLID")
    void testInitialization() {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = -64; y < 320; y++) {
                    assertThat(data.getBlockState(x, y, z))
                            .as("Block at (%d, %d, %d) should be SOLID", x, y, z)
                            .isEqualTo(ClassificationState.SOLID);
                }
            }
        }
    }

    @Test
    @DisplayName("Should set and get block states correctly")
    void testSetAndGetBlockStates() {
        // Test setting different states
        data.setBlockState(0, 0, 0, ClassificationState.OUTSIDE);
        assertThat(data.getBlockState(0, 0, 0)).isEqualTo(ClassificationState.OUTSIDE);

        data.setBlockState(5, 10, 7, ClassificationState.INSIDE);
        assertThat(data.getBlockState(5, 10, 7)).isEqualTo(ClassificationState.INSIDE);

        data.setBlockState(15, 319, 15, ClassificationState.PARTIAL);
        assertThat(data.getBlockState(15, 319, 15)).isEqualTo(ClassificationState.PARTIAL);
    }

    @Test
    @DisplayName("Should handle state transitions correctly")
    void testStateTransitions() {
        int x = 8, y = 100, z = 8;

        data.setBlockState(x, y, z, ClassificationState.SOLID);
        assertThat(data.getBlockState(x, y, z)).isEqualTo(ClassificationState.SOLID);

        data.setBlockState(x, y, z, ClassificationState.OUTSIDE);
        assertThat(data.getBlockState(x, y, z)).isEqualTo(ClassificationState.OUTSIDE);

        data.setBlockState(x, y, z, ClassificationState.INSIDE);
        assertThat(data.getBlockState(x, y, z)).isEqualTo(ClassificationState.INSIDE);

        data.setBlockState(x, y, z, ClassificationState.PARTIAL);
        assertThat(data.getBlockState(x, y, z)).isEqualTo(ClassificationState.PARTIAL);
    }

    @Test
    @DisplayName("Should not affect neighboring blocks")
    void testNoBlockInterference() {
        int x = 8, y = 100, z = 8;

        data.setBlockState(x, y, z, ClassificationState.OUTSIDE);

        // Check that neighbors are still SOLID
        assertThat(data.getBlockState(x + 1, y, z)).isEqualTo(ClassificationState.SOLID);
        assertThat(data.getBlockState(x - 1, y, z)).isEqualTo(ClassificationState.SOLID);
        assertThat(data.getBlockState(x, y + 1, z)).isEqualTo(ClassificationState.SOLID);
        assertThat(data.getBlockState(x, y - 1, z)).isEqualTo(ClassificationState.SOLID);
        assertThat(data.getBlockState(x, y, z + 1)).isEqualTo(ClassificationState.SOLID);
        assertThat(data.getBlockState(x, y, z - 1)).isEqualTo(ClassificationState.SOLID);
    }

    @Test
    @DisplayName("Should handle edge coordinates")
    void testEdgeCoordinates() {
        // Min corner
        data.setBlockState(0, -64, 0, ClassificationState.OUTSIDE);
        assertThat(data.getBlockState(0, -64, 0)).isEqualTo(ClassificationState.OUTSIDE);

        // Max corner
        data.setBlockState(15, 319, 15, ClassificationState.INSIDE);
        assertThat(data.getBlockState(15, 319, 15)).isEqualTo(ClassificationState.INSIDE);

        // Mid-range
        data.setBlockState(8, 128, 8, ClassificationState.PARTIAL);
        assertThat(data.getBlockState(8, 128, 8)).isEqualTo(ClassificationState.PARTIAL);
    }

    @Test
    @DisplayName("Should reject invalid coordinates")
    void testInvalidCoordinates() {
        // Out of X bounds
        data.setBlockState(-1, 0, 0, ClassificationState.OUTSIDE);
        data.setBlockState(16, 0, 0, ClassificationState.OUTSIDE);

        // Out of Y bounds
        data.setBlockState(0, -65, 0, ClassificationState.OUTSIDE);
        data.setBlockState(0, 320, 0, ClassificationState.OUTSIDE);

        // Out of Z bounds
        data.setBlockState(0, 0, -1, ClassificationState.OUTSIDE);
        data.setBlockState(0, 0, 16, ClassificationState.OUTSIDE);

        // Verify they're still SOLID (not modified)
        assertThat(data.getBlockState(0, 0, 0)).isEqualTo(ClassificationState.SOLID);
    }

    @Test
    @DisplayName("Should handle dense block updates")
    void testDenseBlockUpdates() {
        // Set many blocks to different states
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int state = ((x + z) % 4);
                ClassificationState classState = switch (state) {
                    case 0 -> ClassificationState.SOLID;
                    case 1 -> ClassificationState.OUTSIDE;
                    case 2 -> ClassificationState.INSIDE;
                    case 3 -> ClassificationState.PARTIAL;
                    default -> ClassificationState.SOLID;
                };
                data.setBlockState(x, 64, z, classState);
            }
        }

        // Verify all blocks are correct
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int state = ((x + z) % 4);
                ClassificationState expected = switch (state) {
                    case 0 -> ClassificationState.SOLID;
                    case 1 -> ClassificationState.OUTSIDE;
                    case 2 -> ClassificationState.INSIDE;
                    case 3 -> ClassificationState.PARTIAL;
                    default -> ClassificationState.SOLID;
                };
                assertThat(data.getBlockState(x, 64, z))
                        .as("Block at (%d, 64, %d) should be %s", x, z, expected)
                        .isEqualTo(expected);
            }
        }
    }

    @Test
    @DisplayName("Should track dirty state")
    void testDirtyFlag() {
        assertThat(data.isDirty()).isTrue();

        data.setDirty(false);
        assertThat(data.isDirty()).isFalse();

        data.setDirty(true);
        assertThat(data.isDirty()).isTrue();
    }

    @Test
    @DisplayName("Should manage regions list")
    void testRegionManagement() {
        assertThat(data.getRegions()).isEmpty();

        var region1 = new ClassifiedRegion(100, 50);
        var region2 = new ClassifiedRegion(200, 75);

        data.addRegion(region1);
        assertThat(data.getRegions()).hasSize(1).contains(region1);

        data.addRegion(region2);
        assertThat(data.getRegions()).hasSize(2).contains(region1, region2);

        data.clearRegions();
        assertThat(data.getRegions()).isEmpty();
    }

    @Test
    @DisplayName("Should not modify regions after retrieval")
    void testRegionImmutability() {
        var region = new ClassifiedRegion(100, 50);
        data.addRegion(region);

        var retrieved = data.getRegions();
        retrieved.clear();

        // Original should still have the region
        assertThat(data.getRegions()).hasSize(1).contains(region);
    }

    @Test
    @DisplayName("Should maintain bitfield integrity across all height levels")
    void testBitfieldIntegrityAcrossHeights() {
        ClassificationState[] states = ClassificationState.values();

        // Set different states at different heights
        for (int y = -64; y < 320; y += 10) {
            data.setBlockState(5, y, 5, states[(y + 64) % states.length]);
        }

        // Verify they all remain correct
        for (int y = -64; y < 320; y += 10) {
            ClassificationState expected = states[(y + 64) % states.length];
            assertThat(data.getBlockState(5, y, 5))
                    .as("Block at height %d should be %s", y, expected)
                    .isEqualTo(expected);
        }
    }
}
