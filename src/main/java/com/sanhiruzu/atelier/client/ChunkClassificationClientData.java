package com.sanhiruzu.atelier.client;

import com.sanhiruzu.atelier.space.ChunkClassificationData;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.chunk.LevelChunk;

public class ChunkClassificationClientData {
    private static final Long2ObjectMap<ChunkClassificationData> CHUNK_DATA = new Long2ObjectOpenHashMap<>();

    public static void storeChunkData(int chunkX, int chunkZ, ChunkClassificationData data) {
        CHUNK_DATA.put(getKey(chunkX, chunkZ), data);
    }

    public static ChunkClassificationData getChunkData(int chunkX, int chunkZ) {
        return CHUNK_DATA.get(getKey(chunkX, chunkZ));
    }

    public static ChunkClassificationData getChunkData(LevelChunk chunk) {
        return getChunkData(chunk.getPos().x, chunk.getPos().z);
    }

    public static boolean hasChunkData(int chunkX, int chunkZ) {
        return CHUNK_DATA.containsKey(getKey(chunkX, chunkZ));
    }

    public static void clear() {
        CHUNK_DATA.clear();
    }

    private static long getKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
