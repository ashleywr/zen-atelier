package com.sanhiruzu.atelier.client;

import com.sanhiruzu.atelier.space.ChunkClassificationData;
import com.sanhiruzu.atelier.space.ClassificationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class ChunkClassificationClientDataTest {
    private ChunkClassificationData testData;

    @BeforeEach
    void setUp() throws Exception {
        testData = new ChunkClassificationData();
        testData.setBlockState(5, 10, 5, ClassificationState.INSIDE);
        testData.setBlockState(6, 10, 5, ClassificationState.PARTIAL);

        // Clear any previous test data using reflection
        Method clearMethod = ChunkClassificationClientData.class.getDeclaredMethod("clear");
        clearMethod.setAccessible(true);
        clearMethod.invoke(null);
    }

    @Test
    void testStoreAndRetrieveChunkData() throws Exception {
        int chunkX = 42;
        int chunkZ = 99;

        ChunkClassificationClientData.storeChunkData(chunkX, chunkZ, testData);
        ChunkClassificationData retrieved = ChunkClassificationClientData.getChunkData(chunkX, chunkZ);

        assertNotNull(retrieved, "Stored data should be retrievable");
        assertEquals(ClassificationState.INSIDE, retrieved.getBlockState(5, 10, 5), "INSIDE state should be preserved");
        assertEquals(ClassificationState.PARTIAL, retrieved.getBlockState(6, 10, 5), "PARTIAL state should be preserved");
    }

    @Test
    void testHasChunkData() throws Exception {
        int chunkX = 10;
        int chunkZ = 20;

        assertFalse(ChunkClassificationClientData.hasChunkData(chunkX, chunkZ), "Should not have data before storing");

        ChunkClassificationClientData.storeChunkData(chunkX, chunkZ, testData);

        assertTrue(ChunkClassificationClientData.hasChunkData(chunkX, chunkZ), "Should have data after storing");
    }

    @Test
    void testMultipleChunkStorage() throws Exception {
        int chunkX1 = 10, chunkZ1 = 20;
        int chunkX2 = 30, chunkZ2 = 40;

        ChunkClassificationData data1 = new ChunkClassificationData();
        data1.setBlockState(5, 10, 5, ClassificationState.INSIDE);

        ChunkClassificationData data2 = new ChunkClassificationData();
        data2.setBlockState(7, 15, 7, ClassificationState.PARTIAL);

        ChunkClassificationClientData.storeChunkData(chunkX1, chunkZ1, data1);
        ChunkClassificationClientData.storeChunkData(chunkX2, chunkZ2, data2);

        ChunkClassificationData retrieved1 = ChunkClassificationClientData.getChunkData(chunkX1, chunkZ1);
        ChunkClassificationData retrieved2 = ChunkClassificationClientData.getChunkData(chunkX2, chunkZ2);

        assertNotNull(retrieved1);
        assertNotNull(retrieved2);
        assertEquals(ClassificationState.INSIDE, retrieved1.getBlockState(5, 10, 5));
        assertEquals(ClassificationState.PARTIAL, retrieved2.getBlockState(7, 15, 7));
    }

    @Test
    void testNegativeChunkCoordinates() throws Exception {
        int chunkX = -42;
        int chunkZ = -99;

        ChunkClassificationClientData.storeChunkData(chunkX, chunkZ, testData);
        ChunkClassificationData retrieved = ChunkClassificationClientData.getChunkData(chunkX, chunkZ);

        assertNotNull(retrieved, "Negative coordinates should work");
        assertEquals(ClassificationState.INSIDE, retrieved.getBlockState(5, 10, 5));
    }

    @Test
    void testRetrieveNonexistentChunk() {
        ChunkClassificationData retrieved = ChunkClassificationClientData.getChunkData(999, 999);
        assertNull(retrieved, "Nonexistent chunk should return null");
    }

    @Test
    void testClear() throws Exception {
        int chunkX = 42;
        int chunkZ = 99;

        ChunkClassificationClientData.storeChunkData(chunkX, chunkZ, testData);
        assertTrue(ChunkClassificationClientData.hasChunkData(chunkX, chunkZ), "Data should exist before clear");

        Method clearMethod = ChunkClassificationClientData.class.getDeclaredMethod("clear");
        clearMethod.setAccessible(true);
        clearMethod.invoke(null);

        assertFalse(ChunkClassificationClientData.hasChunkData(chunkX, chunkZ), "Data should be cleared");
    }

    @Test
    void testOverwriteExistingChunkData() throws Exception {
        int chunkX = 42;
        int chunkZ = 99;

        // Store initial data
        ChunkClassificationClientData.storeChunkData(chunkX, chunkZ, testData);
        ChunkClassificationData retrieved1 = ChunkClassificationClientData.getChunkData(chunkX, chunkZ);
        assertEquals(ClassificationState.INSIDE, retrieved1.getBlockState(5, 10, 5));

        // Create and store new data
        ChunkClassificationData newData = new ChunkClassificationData();
        newData.setBlockState(5, 10, 5, ClassificationState.OUTSIDE);

        ChunkClassificationClientData.storeChunkData(chunkX, chunkZ, newData);
        ChunkClassificationData retrieved2 = ChunkClassificationClientData.getChunkData(chunkX, chunkZ);

        assertEquals(ClassificationState.OUTSIDE, retrieved2.getBlockState(5, 10, 5), "New data should overwrite old data");
    }

    @Test
    void testLargeNumberOfChunks() throws Exception {
        // Store data for a 5x5 grid of chunks (25 chunks)
        for (int cx = 0; cx < 5; cx++) {
            for (int cz = 0; cz < 5; cz++) {
                ChunkClassificationData data = new ChunkClassificationData();
                data.setBlockState(5, 10, 5, ClassificationState.INSIDE);
                ChunkClassificationClientData.storeChunkData(cx, cz, data);
            }
        }

        // Verify all chunks are stored
        for (int cx = 0; cx < 5; cx++) {
            for (int cz = 0; cz < 5; cz++) {
                assertTrue(ChunkClassificationClientData.hasChunkData(cx, cz), "All 25 chunks should be stored");
                assertNotNull(ChunkClassificationClientData.getChunkData(cx, cz), "All 25 chunks should be retrievable");
            }
        }
    }
}
