package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.*;

public class SpaceRegionRegistry {
    private static final Map<String, SpaceRegionRegistry> INSTANCES = new HashMap<>();

    private final Map<UUID, SpaceRegion> regions = new HashMap<>();
    private final Map<BlockPos, UUID> blockToRegion = new HashMap<>();
    private final Map<UUID, Set<BlockPos>> regionToBlocks = new HashMap<>();

    public static SpaceRegionRegistry get(Level level) {
        String key = level.dimension().location().toString();
        return INSTANCES.computeIfAbsent(key, k -> new SpaceRegionRegistry());
    }

    /**
     * Test-only: creates a fresh registry keyed by {@code key}, bypassing the Level lookup.
     */
    public static SpaceRegionRegistry createForTest(String key) {
        SpaceRegionRegistry fresh = new SpaceRegionRegistry();
        INSTANCES.put(key, fresh);
        return fresh;
    }

    public void clear() {
        regions.clear();
        blockToRegion.clear();
        regionToBlocks.clear();
    }

    public void registerRegion(SpaceRegion region) {
        regions.put(region.getId(), region);
    }

    public void mapBlockToRegion(BlockPos pos, UUID regionId) {
        UUID oldRegionId = blockToRegion.put(pos, regionId);
        if (oldRegionId != null && !oldRegionId.equals(regionId)) {
            Set<BlockPos> oldSet = regionToBlocks.get(oldRegionId);
            if (oldSet != null && oldSet.remove(pos)) {
                SpaceRegion oldRegion = regions.get(oldRegionId);
                if (oldRegion != null) oldRegion.adjustVolume(-1);
                if (oldSet.isEmpty()) regionToBlocks.remove(oldRegionId);
            }
        }
        Set<BlockPos> newSet = regionToBlocks.computeIfAbsent(regionId, id -> new HashSet<>());
        if (newSet.add(pos)) {
            SpaceRegion newRegion = regions.get(regionId);
            if (newRegion != null) newRegion.adjustVolume(1);
        }
    }

    public SpaceRegion getRegion(UUID id) {
        return regions.get(id);
    }

    public UUID getRegionIdAt(BlockPos pos) {
        return blockToRegion.get(pos);
    }

    public SpaceRegion getRegionAt(BlockPos pos) {
        UUID regionId = blockToRegion.get(pos);
        if (regionId != null) {
            return regions.get(regionId);
        }
        return null;
    }

    public SpaceRegion getRegionAt(Level level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(pos);
        ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);

        int localX = pos.getX() & 15;
        int y = pos.getY();
        int localZ = pos.getZ() & 15;

        ClassificationState state = data.getBlockState(localX, y, localZ);
        return switch (state) {
            case OUTSIDE -> new SpaceRegion(UUID.randomUUID(), ClassificationState.OUTSIDE, 0, 0);
            case INSIDE -> {
                UUID regionId = blockToRegion.get(pos);
                if (regionId != null) {
                    yield regions.get(regionId);
                }
                yield null;
            }
            case SOLID, PARTIAL -> null;
        };
    }

    public Set<BlockPos> getBlocksInRegion(UUID regionId) {
        Set<BlockPos> blocks = regionToBlocks.get(regionId);
        return blocks != null ? Collections.unmodifiableSet(blocks) : Collections.emptySet();
    }

    /**
     * Returns all region UUIDs currently registered.
     */
    public Set<UUID> getAllRegionIds() {
        return Collections.unmodifiableSet(regions.keySet());
    }

    public void removeRegion(UUID regionId) {
        regions.remove(regionId);
        Set<BlockPos> blocks = regionToBlocks.remove(regionId);
        if (blocks != null) {
            for (BlockPos pos : blocks) {
                blockToRegion.remove(pos);
            }
        }
    }

    public void unmapBlock(BlockPos pos) {
        UUID regionId = blockToRegion.remove(pos);
        if (regionId != null) {
            Set<BlockPos> set = regionToBlocks.get(regionId);
            if (set != null && set.remove(pos)) {
                SpaceRegion region = regions.get(regionId);
                if (region != null) region.adjustVolume(-1);
                if (set.isEmpty()) regionToBlocks.remove(regionId);
            }
        }
    }

    public void mergeRegions(UUID region1Id, UUID region2Id) {
        SpaceRegion region1 = regions.get(region1Id);
        SpaceRegion region2 = regions.get(region2Id);

        if (region1 != null && region2 != null) {
            region1.mergeWith(region2);
            regions.remove(region2Id);

            Set<BlockPos> region2Blocks = regionToBlocks.remove(region2Id);
            if (region2Blocks != null) {
                for (BlockPos pos : region2Blocks) {
                    blockToRegion.put(pos, region1Id);
                }
                regionToBlocks.computeIfAbsent(region1Id, id -> new HashSet<>()).addAll(region2Blocks);
            }
        }
    }

    public List<MergeEvent> stitch(Level level, ChunkAccess chunk, ChunkAccess neighbor, int direction) {
        ChunkClassificationData chunkData = ChunkClassificationAttachment.get(chunk);
        ChunkClassificationData neighborData = ChunkClassificationAttachment.get(neighbor);

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int nChunkX = neighbor.getPos().x;
        int nChunkZ = neighbor.getPos().z;

        if (chunkX == nChunkX - 1) {
            return stitchXBoundary(chunkData, neighborData, chunk, neighbor);
        } else if (chunkX == nChunkX + 1) {
            return stitchXBoundary(neighborData, chunkData, neighbor, chunk);
        } else if (chunkZ == nChunkZ - 1) {
            return stitchZBoundary(chunkData, neighborData, chunk, neighbor);
        } else if (chunkZ == nChunkZ + 1) {
            return stitchZBoundary(neighborData, chunkData, neighbor, chunk);
        }
        return List.of();
    }

    private List<MergeEvent> stitchXBoundary(ChunkClassificationData leftData, ChunkClassificationData rightData, ChunkAccess leftChunk, ChunkAccess rightChunk) {
        return stitchBoundary(leftData, rightData, leftChunk, rightChunk, true);
    }

    private List<MergeEvent> stitchZBoundary(ChunkClassificationData frontData, ChunkClassificationData backData, ChunkAccess frontChunk, ChunkAccess backChunk) {
        return stitchBoundary(frontData, backData, frontChunk, backChunk, false);
    }

    private List<MergeEvent> stitchBoundary(ChunkClassificationData firstData,
                                            ChunkClassificationData secondData,
                                            ChunkAccess firstChunk,
                                            ChunkAccess secondChunk,
                                            boolean alongX) {
        List<MergeEvent> events = new ArrayList<>();
        for (int axis = 0; axis < 16; axis++) {
            for (int y = -64; y < 320; y++) {
                int firstLocalX = alongX ? 15 : axis;
                int firstLocalZ = alongX ? axis : 15;
                int secondLocalX = alongX ? 0 : axis;
                int secondLocalZ = alongX ? axis : 0;

                ClassificationState firstState = firstData.getBlockState(firstLocalX, y, firstLocalZ);
                ClassificationState secondState = secondData.getBlockState(secondLocalX, y, secondLocalZ);

                if (shouldMergeRegions(firstState, secondState)) {
                    UUID firstRegionId = blockToRegion.get(new BlockPos(
                            firstChunk.getPos().getMinBlockX() + firstLocalX,
                            y,
                            firstChunk.getPos().getMinBlockZ() + firstLocalZ));
                    UUID secondRegionId = blockToRegion.get(new BlockPos(
                            secondChunk.getPos().getMinBlockX() + secondLocalX,
                            y,
                            secondChunk.getPos().getMinBlockZ() + secondLocalZ));

                    if (firstRegionId != null
                            && secondRegionId != null
                            && !firstRegionId.equals(secondRegionId)
                            && regions.containsKey(secondRegionId)) {
                        mergeRegions(firstRegionId, secondRegionId);
                        events.add(new MergeEvent(firstRegionId, secondRegionId));
                    }
                }
            }
        }
        return events;
    }

    private boolean shouldMergeRegions(ClassificationState state1, ClassificationState state2) {
        boolean isAir1 = state1 == ClassificationState.INSIDE || state1 == ClassificationState.PARTIAL;
        boolean isAir2 = state2 == ClassificationState.INSIDE || state2 == ClassificationState.PARTIAL;

        // Allow merging if both are air-like (INSIDE/PARTIAL), OR if at least one is and the other
        // is OUTSIDE (handle damaged ceilings where one side is exposed to sky but still part of the zone).
        // This allows zones to stitch across boundaries even when one side's ceiling is broken.
        return (isAir1 && isAir2) || (isAir1 && state2 == ClassificationState.OUTSIDE) || (state1 == ClassificationState.OUTSIDE && isAir2);
    }

    public record MergeEvent(UUID survivingId, UUID eliminatedId) {
    }
}
