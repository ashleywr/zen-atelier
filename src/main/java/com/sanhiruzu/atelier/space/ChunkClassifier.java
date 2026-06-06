package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.*;

public class ChunkClassifier {
    private static final int CHUNK_WIDTH = 16;
    private static final int CHUNK_HEIGHT = 384;

    private Map<UUID, Set<BlockPos>> lastRegionBlocks = new HashMap<>();

    private static boolean isNaturalBlock(ChunkAccess chunk, BlockPos pos) {
        BlockState state = chunk.getBlockState(pos);
        return state.is(BlockTags.BASE_STONE_OVERWORLD)   // stone, deepslate, tuff, granite, diorite, andesite
                || state.is(BlockTags.BASE_STONE_NETHER)      // netherrack, basalt, blackstone
                || state.is(BlockTags.DIRT)                   // dirt, coarse dirt, podzol, grass, mycelium
                || state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(Blocks.BEDROCK)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.DRIPSTONE_BLOCK)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.AMETHYST_BLOCK)
                || state.is(Blocks.BUDDING_AMETHYST)
                || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.ANCIENT_DEBRIS)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.END_STONE);
    }

    public Map<UUID, Set<BlockPos>> getLastRegionBlocks() {
        return lastRegionBlocks;
    }

    public void classify(ChunkAccess chunk) {
        ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);
        clearStates(data);
        data.clearRegions();
        lastRegionBlocks = new HashMap<>();
        markOutside(chunk, data);
        findInsideRegions(chunk, data);
    }

    private void clearStates(ChunkClassificationData data) {
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int z = 0; z < CHUNK_WIDTH; z++) {
                for (int y = -64; y < 320; y++) {
                    // Preserve INSIDE blocks — they were pre-marked from restored zone data
                    // so that markOutside() treats them as barriers and cannot flood through.
                    if (data.getBlockState(x, y, z) != ClassificationState.INSIDE) {
                        data.setBlockState(x, y, z, ClassificationState.SOLID);
                    }
                }
            }
        }
    }

    private void markOutside(ChunkAccess chunk, ChunkClassificationData data) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        RoomConnectivity.BlockLookup lookup = RoomConnectivity.forChunk(chunk);
        Queue<BlockPos> queue = new ArrayDeque<>();
        boolean[] visited = new boolean[CHUNK_WIDTH * CHUNK_HEIGHT * CHUNK_WIDTH];

        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int z = 0; z < CHUNK_WIDTH; z++) {
                BlockPos pos = new BlockPos(minX + x, 319, minZ + z);
                if (RoomConnectivity.isRoomAir(lookup, pos)) {
                    data.setBlockState(x, 319, z, ClassificationState.OUTSIDE);
                    queue.add(pos);
                    visited[indexOf(x, 319, z)] = true;
                }
            }
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();

            for (BlockPos neighbor : RoomConnectivity.allNeighbors(lookup, pos)) {
                int nX = neighbor.getX() - minX;
                int nY = neighbor.getY();
                int nZ = neighbor.getZ() - minZ;

                if (nY < -64 || nY >= 320 || nX < 0 || nX >= CHUNK_WIDTH || nZ < 0 || nZ >= CHUNK_WIDTH) continue;
                int index = indexOf(nX, nY, nZ);
                if (visited[index]) continue;

                if (data.getBlockState(nX, nY, nZ) == ClassificationState.SOLID
                        && RoomConnectivity.isRoomAir(lookup, neighbor)) {
                    data.setBlockState(nX, nY, nZ, ClassificationState.OUTSIDE);
                    queue.add(neighbor);
                    visited[index] = true;
                }
            }
        }
    }

    private void findInsideRegions(ChunkAccess chunk, ChunkClassificationData data) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        RoomConnectivity.BlockLookup lookup = RoomConnectivity.forChunk(chunk);
        boolean[] globalVisited = new boolean[CHUNK_WIDTH * CHUNK_HEIGHT * CHUNK_WIDTH];

        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int z = 0; z < CHUNK_WIDTH; z++) {
                for (int y = -64; y < 320; y++) {
                    ClassificationState seedState = data.getBlockState(x, y, z);
                    if (seedState == ClassificationState.SOLID) {
                        BlockPos pos = new BlockPos(minX + x, y, minZ + z);
                        if (globalVisited[indexOf(x, y, z)]) continue;

                        Set<BlockPos> region = new HashSet<>();
                        FloodFillResult result = floodFillInside(chunk, lookup, data, x, y, z, region, globalVisited);

                        if (!region.isEmpty()) {
                            // Skip sealed pockets with only natural walls — natural caves, not player spaces.
                            if (result.openingArea() == 0 && !result.hasPlayerBlock()) continue;
                            // Entry validation is deferred to ZoneRegistry.isValidPlayerBuiltZone(),
                            // which checks the full stitched region and handles cross-chunk doors correctly.

                            for (BlockPos p : region) {
                                int lX = p.getX() - minX;
                                int lZ = p.getZ() - minZ;
                                data.setBlockState(lX, p.getY(), lZ, ClassificationState.INSIDE);
                            }
                            ClassifiedRegion classifiedRegion = new ClassifiedRegion(region.size(), result.openingArea());
                            data.addRegion(classifiedRegion);
                            lastRegionBlocks.put(classifiedRegion.id(), new HashSet<>(region));
                        }
                    }
                }
            }
        }
    }

    private FloodFillResult floodFillInside(ChunkAccess chunk, RoomConnectivity.BlockLookup lookup,
                                            ChunkClassificationData data, int startX, int startY, int startZ,
                                            Set<BlockPos> region, boolean[] globalVisited) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos start = new BlockPos(minX + startX, startY, minZ + startZ);

        ClassificationState startState = data.getBlockState(startX, startY, startZ);
        if (!RoomConnectivity.isRoomAir(lookup, start) || startState != ClassificationState.SOLID) {
            return new FloodFillResult(0, false);
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        region.add(start);
        globalVisited[indexOf(startX, startY, startZ)] = true;
        int openingArea = 0;
        boolean hasPlayerBlock = false;

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();

            for (BlockPos neighbor : RoomConnectivity.allNeighbors(lookup, pos)) {
                int nX = neighbor.getX() - minX;
                int nY = neighbor.getY();
                int nZ = neighbor.getZ() - minZ;

                if (nY < -64 || nY >= 320 || nX < 0 || nX >= CHUNK_WIDTH || nZ < 0 || nZ >= CHUNK_WIDTH) continue;

                int index = indexOf(nX, nY, nZ);
                if (globalVisited[index]) continue;

                ClassificationState state = data.getBlockState(nX, nY, nZ);
                if (state == ClassificationState.OUTSIDE) {
                    openingArea++;
                    continue;
                }

                if (RoomConnectivity.isRoomAir(lookup, neighbor)
                        && (state == ClassificationState.SOLID || state == ClassificationState.PARTIAL)) {
                    region.add(neighbor);
                    globalVisited[index] = true;
                    queue.add(neighbor);
                } else if (!RoomConnectivity.isRoomAir(lookup, neighbor)) {
                    if (!hasPlayerBlock && !isNaturalBlock(chunk, neighbor)) hasPlayerBlock = true;
                }
            }
        }
        return new FloodFillResult(openingArea, hasPlayerBlock);
    }

    private static int indexOf(int x, int y, int z) {
        return x + CHUNK_WIDTH * (z + CHUNK_WIDTH * (y + 64));
    }

    private record FloodFillResult(int openingArea, boolean hasPlayerBlock) {
    }

}
