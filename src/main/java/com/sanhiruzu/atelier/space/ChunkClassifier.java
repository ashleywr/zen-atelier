package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;

import java.util.*;

public class ChunkClassifier {
    private static final int CHUNK_WIDTH = 16;
    private static final int CHUNK_HEIGHT = 384;
    private static final int[][] NEIGHBOR_OFFSETS = {
            {0, 1, 0}, {0, -1, 0}, {-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}
    };

    private Map<UUID, Set<BlockPos>> lastRegionBlocks = new HashMap<>();

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
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int z = 0; z < CHUNK_WIDTH; z++) {
                BlockPos pos = new BlockPos(minX + x, 319, minZ + z);
                if (isAir(chunk, pos)) {
                    data.setBlockState(x, 319, z, ClassificationState.OUTSIDE);
                    queue.add(pos);
                    visited.add(pos);
                }
            }
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();

            for (BlockPos neighbor : getNeighbors(pos)) {
                int nX = neighbor.getX() - minX;
                int nY = neighbor.getY();
                int nZ = neighbor.getZ() - minZ;

                if (nY < -64 || nY >= 320 || nX < 0 || nX >= CHUNK_WIDTH || nZ < 0 || nZ >= CHUNK_WIDTH) continue;
                if (visited.contains(neighbor)) continue;

                if (data.getBlockState(nX, nY, nZ) == ClassificationState.SOLID && isAir(chunk, neighbor)) {
                    data.setBlockState(nX, nY, nZ, ClassificationState.OUTSIDE);
                    queue.add(neighbor);
                    visited.add(neighbor);
                }
            }
        }
    }

    private record FloodFillResult(int openingArea, boolean hasPlayerBlock) {
    }

    private void findInsideRegions(ChunkAccess chunk, ChunkClassificationData data) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        Set<BlockPos> globalVisited = new HashSet<>();

        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int z = 0; z < CHUNK_WIDTH; z++) {
                for (int y = -64; y < 320; y++) {
                    ClassificationState seedState = data.getBlockState(x, y, z);
                    if (seedState == ClassificationState.SOLID) {
                        BlockPos pos = new BlockPos(minX + x, y, minZ + z);
                        if (globalVisited.contains(pos)) continue;

                        Set<BlockPos> region = new HashSet<>();
                        FloodFillResult result = floodFillInside(chunk, data, x, y, z, region, globalVisited);

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
                            lastRegionBlocks.put(classifiedRegion.getId(), new HashSet<>(region));
                        }
                    }
                }
            }
        }
    }

    private FloodFillResult floodFillInside(ChunkAccess chunk, ChunkClassificationData data, int startX, int startY, int startZ, Set<BlockPos> region, Set<BlockPos> globalVisited) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos start = new BlockPos(minX + startX, startY, minZ + startZ);

        ClassificationState startState = data.getBlockState(startX, startY, startZ);
        if (!isAir(chunk, start) || startState != ClassificationState.SOLID) return new FloodFillResult(0, false);

        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        region.add(start);
        globalVisited.add(start);
        int openingArea = 0;
        boolean hasPlayerBlock = false;

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();

            for (BlockPos neighbor : getNeighbors(pos)) {
                int nX = neighbor.getX() - minX;
                int nY = neighbor.getY();
                int nZ = neighbor.getZ() - minZ;

                if (nY < -64 || nY >= 320 || nX < 0 || nX >= CHUNK_WIDTH || nZ < 0 || nZ >= CHUNK_WIDTH) continue;

                if (region.contains(neighbor) || globalVisited.contains(neighbor)) continue;

                ClassificationState state = data.getBlockState(nX, nY, nZ);
                if (state == ClassificationState.OUTSIDE) {
                    openingArea++;
                    continue;
                }

                if (isAir(chunk, neighbor) && (state == ClassificationState.SOLID || state == ClassificationState.PARTIAL)) {
                    region.add(neighbor);
                    globalVisited.add(neighbor);
                    queue.add(neighbor);
                } else if (!isAir(chunk, neighbor)) {
                    if (!hasPlayerBlock && !isNaturalBlock(chunk, neighbor)) hasPlayerBlock = true;
                }
            }
        }
        return new FloodFillResult(openingArea, hasPlayerBlock);
    }

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

    private boolean isAir(ChunkAccess chunk, BlockPos pos) {
        return chunk.getBlockState(pos).isAir() && !chunk.getFluidState(pos).is(Fluids.WATER);
    }

    private List<BlockPos> getNeighbors(BlockPos pos) {
        return Arrays.asList(
                pos.above(), pos.below(),
                pos.north(), pos.south(),
                pos.east(), pos.west()
        );
    }
}
