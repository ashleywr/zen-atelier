package com.sanhiruzu.atelier.space.analyze;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analyzes a {@link ChunkSnapshotBundle} to produce a list of {@link MicroRegion}s.
 *
 * <p>Each MicroRegion is a connected set of walkable surface cells, separated by
 * entry-connector cells (doors, fence gates, trapdoors). The analysis is pure Java
 * with no live-world access — all data comes from the immutable snapshot.
 */
public final class ChunkAnalyzer {

    private ChunkAnalyzer() {}

    /**
     * Analyzes the given chunk snapshot and returns all MicroRegions found.
     */
    public static List<MicroRegion> analyze(ChunkSnapshotBundle bundle) {
        boolean[] visited = new boolean[ChunkSnapshotBundle.SIZE];
        List<MicroRegion> results = new ArrayList<>();

        int minBlockX = bundle.chunkX * 16;
        int minBlockZ = bundle.chunkZ * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = -64; y < 320; y++) {
                    int idx = ChunkSnapshotBundle.indexOf(x, y, z);
                    if (visited[idx]) continue;
                    if (!bundle.isWalkablePosition(x, y, z)) continue;

                    // BFS flood fill from this seed
                    MicroRegion region = floodFill(bundle, visited, x, y, z, minBlockX, minBlockZ);
                    if (region != null) {
                        results.add(region);
                    }
                }
            }
        }

        return results;
    }

    private static MicroRegion floodFill(ChunkSnapshotBundle bundle, boolean[] visited,
                                          int seedX, int seedY, int seedZ,
                                          int minBlockX, int minBlockZ) {
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        List<long[]> walkablePosList = new ArrayList<>();
        List<BoundaryContact> boundaryContacts = new ArrayList<>();
        Set<Long> countedFurniture = new HashSet<>();

        int naturalBlockCount = 0;
        int playerBuiltCount = 0;
        int shelterCount = 0;
        int furnitureCount = 0;

        // Track bounds in world coordinates
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        // Track local bounds for key computation
        int localMinX = Integer.MAX_VALUE, localMinY = Integer.MAX_VALUE, localMinZ = Integer.MAX_VALUE;

        int seedIdx = ChunkSnapshotBundle.indexOf(seedX, seedY, seedZ);
        visited[seedIdx] = true;
        queue.add(new int[]{seedX, seedY, seedZ});

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int cx = cell[0], cy = cell[1], cz = cell[2];

            // World coordinates
            int wx = minBlockX + cx;
            int wz = minBlockZ + cz;

            // Add to walkable positions
            long worldPos = BlockPos.asLong(wx, cy, wz);
            walkablePosList.add(new long[]{worldPos});

            // Update bounds
            if (wx < minX) minX = wx;
            if (cy < minY) minY = cy;
            if (wz < minZ) minZ = wz;
            if (wx > maxX) maxX = wx;
            if (cy > maxY) maxY = cy;
            if (wz > maxZ) maxZ = wz;

            // Update local bounds for key
            if (cx < localMinX) localMinX = cx;
            if (cy < localMinY) localMinY = cy;
            if (cz < localMinZ) localMinZ = cz;

            // Check shelter above
            if (bundle.hasShelterAbove(cx, cy, cz, 8)) {
                shelterCount++;
            }

            // Check edge / boundary contacts
            if (cx == 0) {
                boolean isPortal = bundle.isEntryConnector(1, cy, cz);
                int axisCoord = minBlockX; // world X of west edge
                boundaryContacts.add(new BoundaryContact(BoundaryContact.Face.WEST, axisCoord, cy, isPortal, 0L));
            }
            if (cx == 15) {
                boolean isPortal = bundle.isEntryConnector(14, cy, cz);
                int axisCoord = minBlockX + 15; // world X of east edge
                boundaryContacts.add(new BoundaryContact(BoundaryContact.Face.EAST, axisCoord, cy, isPortal, 0L));
            }
            if (cz == 0) {
                boolean isPortal = bundle.isEntryConnector(cx, cy, 1);
                int axisCoord = minBlockZ; // world Z of north edge
                boundaryContacts.add(new BoundaryContact(BoundaryContact.Face.NORTH, axisCoord, cy, isPortal, 0L));
            }
            if (cz == 15) {
                boolean isPortal = bundle.isEntryConnector(cx, cy, 14);
                int axisCoord = minBlockZ + 15; // world Z of south edge
                boundaryContacts.add(new BoundaryContact(BoundaryContact.Face.SOUTH, axisCoord, cy, isPortal, 0L));
            }

            // Check 6-face neighbors for solid block classification
            int[][] neighbors = {
                {cx+1, cy, cz}, {cx-1, cy, cz},
                {cx, cy+1, cz}, {cx, cy-1, cz},
                {cx, cy, cz+1}, {cx, cy, cz-1}
            };

            for (int[] nb : neighbors) {
                int nx = nb[0], ny = nb[1], nz = nb[2];

                // Skip out-of-bounds neighbors
                if (nx < 0 || nx > 15 || ny < -64 || ny > 319 || nz < 0 || nz > 15) continue;

                int nbIdx = ChunkSnapshotBundle.indexOf(nx, ny, nz);

                // Solid neighbor classification (not air, not step, not entry)
                boolean isAir = bundle.isAir(nx, ny, nz);
                boolean isStep = bundle.isStepConnector(nx, ny, nz);
                boolean isEntry = bundle.isEntryConnector(nx, ny, nz);

                if (!isAir && !isStep && !isEntry) {
                    // It's a solid neighbor
                    if (bundle.isNaturalBlock(nx, ny, nz)) {
                        naturalBlockCount++;
                    } else if (bundle.isFurnitureSignal(nx, ny, nz)) {
                        long nbWorldPos = BlockPos.asLong(minBlockX + nx, ny, minBlockZ + nz);
                        if (countedFurniture.add(nbWorldPos)) {
                            furnitureCount++;
                        }
                    } else {
                        playerBuiltCount++;
                    }
                }

                // BFS expansion: expand to walkable neighbors, mark entry neighbors visited
                if (visited[nbIdx]) continue;

                if (isEntry) {
                    // Mark entry cells visited so they don't become seeds, but don't add to region
                    visited[nbIdx] = true;
                } else if (bundle.isWalkablePosition(nx, ny, nz)) {
                    visited[nbIdx] = true;
                    queue.add(new int[]{nx, ny, nz});
                }
            }
        }

        if (walkablePosList.isEmpty()) return null;

        // Build walkablePositions array
        long[] walkablePositions = new long[walkablePosList.size()];
        for (int i = 0; i < walkablePosList.size(); i++) {
            walkablePositions[i] = walkablePosList.get(i)[0];
        }

        long key = regionKey(bundle.chunkX, bundle.chunkZ, localMinX, localMinY, localMinZ);

        // Fill in microRegionKey for boundary contacts now that we have the key
        List<BoundaryContact> finalContacts = new ArrayList<>(boundaryContacts.size());
        for (BoundaryContact bc : boundaryContacts) {
            finalContacts.add(new BoundaryContact(bc.face(), bc.axisCoord(), bc.y(), bc.isPortal(), key));
        }

        return new MicroRegion(
                key,
                bundle.chunkX, bundle.chunkZ,
                walkablePositions,
                finalContacts,
                naturalBlockCount, playerBuiltCount, shelterCount, furnitureCount,
                minX, minY, minZ, maxX, maxY, maxZ
        );
    }

    private static long regionKey(int chunkX, int chunkZ, int localMinX, int localMinY, int localMinZ) {
        return ((long)(chunkX & 0xFFFFF)) << 40
             | ((long)(chunkZ & 0xFFFFF)) << 20
             | ((long)(localMinX & 0xF))  << 16
             | ((long)(localMinY + 64) & 0x1FF) << 7
             | ((long)(localMinZ & 0xF));
    }

    static long regionKeyForTest(int chunkX, int chunkZ, int localMinX, int localMinY, int localMinZ) {
        return regionKey(chunkX, chunkZ, localMinX, localMinY, localMinZ);
    }
}
