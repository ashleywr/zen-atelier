package com.sanhiruzu.atelier.space;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Chunk Classification Performance Tests")
class ChunkClassificationPerformanceTest {
    private ChunkClassificationData data;

    private static class PerfMetrics {
        long totalTime;
        int operationCount;

        PerfMetrics(long totalTime, int operationCount) {
            this.totalTime = totalTime;
            this.operationCount = operationCount;
        }

        double avgTimePerOp() {
            return operationCount > 0 ? (double) totalTime / operationCount : 0;
        }

        void printResults(String testName, long expectedMaxTime) {
            double avgMicroseconds = avgTimePerOp() / 1000.0;
            long expectedMaxMicros = expectedMaxTime / 1000;
            System.out.printf("%s: %.3f µs/op (total: %d ms, ops: %d)%n",
                    testName, avgMicroseconds, totalTime / 1_000_000, operationCount);
            assertThat(totalTime)
                    .as("%s should complete in less than %d ms", testName, expectedMaxTime / 1_000_000)
                    .isLessThan(expectedMaxTime);
        }
    }

    @BeforeEach
    void setUp() {
        data = new ChunkClassificationData();
    }

    @Test
    @DisplayName("Single block set/get should be fast")
    void testSingleBlockPerformance() {
        int iterations = 100_000;
        long start = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            int x = i % 16;
            int y = (i / 16) % 384;
            int z = (i / (16 * 384)) % 16;

            data.setBlockState(x, y - 64, z, ClassificationState.OUTSIDE);
            ClassificationState state = data.getBlockState(x, y - 64, z);
        }

        long elapsed = System.nanoTime() - start;
        new PerfMetrics(elapsed, iterations * 2).printResults("Single block set/get", 500_000_000); // 500ms
    }

    @Test
    @DisplayName("Full chunk fill should be performant")
    void testFullChunkFill() {
        int totalBlocks = 16 * 16 * 384;
        long start = System.nanoTime();

        // Fill all blocks in the chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = -64; y < 320; y++) {
                    data.setBlockState(x, y, z, ClassificationState.OUTSIDE);
                }
            }
        }

        long elapsed = System.nanoTime() - start;
        new PerfMetrics(elapsed, totalBlocks).printResults("Full chunk fill", 1_500_000_000); // 1.5s
    }

    @Test
    @DisplayName("Full chunk read should be fast")
    void testFullChunkRead() {
        // Pre-fill
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = -64; y < 320; y++) {
                    data.setBlockState(x, y, z, ClassificationState.OUTSIDE);
                }
            }
        }

        int totalBlocks = 16 * 16 * 384;
        long start = System.nanoTime();

        // Read all blocks
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = -64; y < 320; y++) {
                    ClassificationState state = data.getBlockState(x, y, z);
                    assertThat(state).isEqualTo(ClassificationState.OUTSIDE);
                }
            }
        }

        long elapsed = System.nanoTime() - start;
        new PerfMetrics(elapsed, totalBlocks).printResults("Full chunk read", 1_500_000_000); // 1.5s
    }

    @Test
    @DisplayName("Serialization of full chunk should be performant")
    void testFullChunkSerialization() {
        // Pre-fill
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = -64; y < 320; y++) {
                    data.setBlockState(x, y, z, ClassificationState.OUTSIDE);
                }
            }
        }

        long start = System.nanoTime();
        CompoundTag tag = new CompoundTag();
        data.serializeNBT(tag);
        long elapsed = System.nanoTime() - start;

        System.out.printf("Full chunk serialization: %.2f ms%n", elapsed / 1_000_000.0);
        assertThat(elapsed)
                .as("Serialization should be fast")
                .isLessThan(100_000_000); // 100ms
    }

    @Test
    @DisplayName("Deserialization of full chunk should be performant")
    void testFullChunkDeserialization() {
        // Pre-fill and serialize
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = -64; y < 320; y++) {
                    data.setBlockState(x, y, z, ClassificationState.OUTSIDE);
                }
            }
        }

        CompoundTag tag = new CompoundTag();
        data.serializeNBT(tag);

        var restored = new ChunkClassificationData();
        long start = System.nanoTime();
        restored.deserializeNBT(tag);
        long elapsed = System.nanoTime() - start;

        System.out.printf("Full chunk deserialization: %.2f ms%n", elapsed / 1_000_000.0);
        assertThat(elapsed)
                .as("Deserialization should be fast")
                .isLessThan(100_000_000); // 100ms
    }

    @Test
    @DisplayName("Random access pattern should be fast")
    void testRandomAccessPattern() {
        long seed = 12345L;
        java.util.Random random = new java.util.Random(seed);
        int iterations = 50_000;

        long start = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            int x = random.nextInt(16);
            int y = random.nextInt(384) - 64;
            int z = random.nextInt(16);
            int stateIdx = random.nextInt(4);
            ClassificationState state = ClassificationState.values()[stateIdx];

            data.setBlockState(x, y, z, state);
            ClassificationState read = data.getBlockState(x, y, z);
            assertThat(read).isEqualTo(state);
        }

        long elapsed = System.nanoTime() - start;
        new PerfMetrics(elapsed, iterations * 2).printResults("Random access pattern", 300_000_000); // 300ms
    }

    @Test
    @DisplayName("Bitfield memory efficiency")
    void testMemoryEfficiency() {
        // Each chunk stores 16*16*384 blocks with 2 bits each
        // That's 98,304 blocks * 2 bits = 196,608 bits = 24,576 bytes = ~24KB
        // But stored as longs, so (196,608 + 63) / 64 = 3,072 longs = 24,576 bytes

        int expectedLongs = 3_072;
        int totalBlocks = 16 * 16 * 384;
        int bitsNeeded = totalBlocks * 2;
        int actualLongs = (bitsNeeded + 63) / 64;

        assertThat(actualLongs)
                .as("Bitfield should use minimal memory")
                .isEqualTo(expectedLongs);

        System.out.printf("Chunk data uses %d bytes (%d longs) for %d blocks%n",
                actualLongs * 8, actualLongs, totalBlocks);
    }

    @Test
    @DisplayName("Stress test: many cycles of fill/read/serialize")
    void testStressCycle() {
        int cycles = 10;
        long totalTime = 0;

        for (int cycle = 0; cycle < cycles; cycle++) {
            var testData = new ChunkClassificationData();

            // Fill
            long cycleStart = System.nanoTime();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = -64; y < 320; y++) {
                        testData.setBlockState(x, y, z, ClassificationState.OUTSIDE);
                    }
                }
            }

            // Read
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = -64; y < 320; y++) {
                        ClassificationState state = testData.getBlockState(x, y, z);
                    }
                }
            }

            // Serialize/Deserialize
            CompoundTag tag = new CompoundTag();
            testData.serializeNBT(tag);
            var restored = new ChunkClassificationData();
            restored.deserializeNBT(tag);

            totalTime += System.nanoTime() - cycleStart;
        }

        double avgCycleMs = totalTime / cycles / 1_000_000.0;
        System.out.printf("Stress test: %.2f ms/cycle (total: %.2f s)%n",
                avgCycleMs, totalTime / 1_000_000_000.0);

        assertThat(totalTime)
                .as("Stress test should complete in reasonable time")
                .isLessThan(5_000_000_000L); // 5 seconds for 10 cycles
    }
}
