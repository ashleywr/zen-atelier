package com.sanhiruzu.atelier.client;

import com.sanhiruzu.atelier.space.ChunkClassificationData;
import com.sanhiruzu.atelier.space.ClassificationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DebugRendererTest {
    private ChunkClassificationData data;

    @BeforeEach
    void setUp() {
        data = new ChunkClassificationData();
    }

    @Test
    void testChunkClassificationDataStoresAndRetrievesData() {
        // Verify the underlying data structure works
        data.setBlockState(5, 10, 5, ClassificationState.INSIDE);
        data.setBlockState(6, 10, 5, ClassificationState.PARTIAL);
        data.setBlockState(7, 10, 5, ClassificationState.OUTSIDE);

        assertEquals(ClassificationState.INSIDE, data.getBlockState(5, 10, 5));
        assertEquals(ClassificationState.PARTIAL, data.getBlockState(6, 10, 5));
        assertEquals(ClassificationState.OUTSIDE, data.getBlockState(7, 10, 5));
    }
}
