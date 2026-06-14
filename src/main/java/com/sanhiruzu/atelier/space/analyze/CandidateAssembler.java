package com.sanhiruzu.atelier.space.analyze;

import net.minecraft.world.level.ChunkPos;

import java.util.*;

public final class CandidateAssembler {

    private CandidateAssembler() {}

    public static List<ZoneCandidate> assemble(Map<ChunkPos, List<MicroRegion>> analysisResults) {
        // 1. Flat map of all regions by key
        Map<Long, MicroRegion> allRegions = new HashMap<>();
        for (List<MicroRegion> regions : analysisResults.values()) {
            for (MicroRegion r : regions) {
                allRegions.put(r.key(), r);
            }
        }

        // 2. Initialize union-find
        Map<Long, Long> parent = new HashMap<>();
        for (Long key : allRegions.keySet()) {
            parent.put(key, key);
        }

        // 3. Stitch with EAST and SOUTH neighbors
        for (ChunkPos pos : analysisResults.keySet()) {
            ChunkPos eastNeighbor = new ChunkPos(pos.x + 1, pos.z);
            ChunkPos southNeighbor = new ChunkPos(pos.x, pos.z + 1);
            stitch(pos, eastNeighbor, BoundaryContact.Face.EAST, BoundaryContact.Face.WEST, analysisResults, parent);
            stitch(pos, southNeighbor, BoundaryContact.Face.SOUTH, BoundaryContact.Face.NORTH, analysisResults, parent);
        }

        // 4. Group region keys by their union-find root
        Map<Long, Set<Long>> groups = new HashMap<>();
        for (Long key : allRegions.keySet()) {
            Long root = find(parent, key);
            groups.computeIfAbsent(root, k -> new HashSet<>()).add(key);
        }

        // 5. Build a ZoneCandidate for each group
        List<ZoneCandidate> candidates = new ArrayList<>();
        for (Set<Long> memberKeys : groups.values()) {
            Set<ChunkPos> chunkPositions = new HashSet<>();
            int totalWalkable = 0;
            int totalFurniture = 0;
            int totalShelter = 0;
            int totalNatural = 0;
            int totalPlayerBuilt = 0;
            boolean hasPortalAccess = false;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            for (Long key : memberKeys) {
                MicroRegion r = allRegions.get(key);
                chunkPositions.add(new ChunkPos(r.chunkX(), r.chunkZ()));
                totalWalkable += r.walkableCount();
                totalFurniture += r.furnitureCount();
                totalShelter += r.shelterCount();
                totalNatural += r.naturalBlockCount();
                totalPlayerBuilt += r.playerBuiltCount();
                if (r.minX() < minX) minX = r.minX();
                if (r.minY() < minY) minY = r.minY();
                if (r.minZ() < minZ) minZ = r.minZ();
                if (r.maxX() > maxX) maxX = r.maxX();
                if (r.maxY() > maxY) maxY = r.maxY();
                if (r.maxZ() > maxZ) maxZ = r.maxZ();
                for (BoundaryContact bc : r.boundaryContacts()) {
                    if (bc.isPortal()) {
                        hasPortalAccess = true;
                    }
                }
            }

            double shelterFraction = totalWalkable == 0 ? 0.0 : (double) totalShelter / totalWalkable;
            double totalNaturalAndBuilt = totalNatural + totalPlayerBuilt;
            double naturalFraction = totalNaturalAndBuilt == 0.0 ? 0.0 : totalNatural / totalNaturalAndBuilt;
            double playerBuiltFraction = totalNaturalAndBuilt == 0.0 ? 0.0 : totalPlayerBuilt / totalNaturalAndBuilt;

            // Stable hash: sort keys then XOR-fold
            List<Long> sortedKeys = new ArrayList<>(memberKeys);
            Collections.sort(sortedKeys);
            long hash = 0L;
            for (long k : sortedKeys) {
                hash ^= k * 0x9e3779b97f4a7c15L;
            }

            candidates.add(new ZoneCandidate(
                    hash,
                    Collections.unmodifiableSet(memberKeys),
                    Collections.unmodifiableSet(chunkPositions),
                    totalWalkable,
                    totalFurniture,
                    shelterFraction,
                    naturalFraction,
                    playerBuiltFraction,
                    hasPortalAccess,
                    minX, minY, minZ,
                    maxX, maxY, maxZ
            ));
        }

        return candidates;
    }

    private static Long find(Map<Long, Long> parent, Long key) {
        while (!parent.get(key).equals(key)) {
            Long gp = parent.get(parent.get(key));
            parent.put(key, gp);  // path compression
            key = gp;
        }
        return key;
    }

    private static void union(Map<Long, Long> parent, Long a, Long b) {
        Long ra = find(parent, a), rb = find(parent, b);
        if (!ra.equals(rb)) parent.put(rb, ra);
    }

    private static Map<Long, BoundaryContact> contactIndex(List<MicroRegion> regions, BoundaryContact.Face face) {
        Map<Long, BoundaryContact> index = new HashMap<>();
        for (MicroRegion r : regions) {
            for (BoundaryContact c : r.boundaryContacts()) {
                if (c.face() != face) continue;
                long coord = ((long) c.axisCoord() << 16) | (c.y() + 64);
                // Store a contact with microRegionKey = r.key()
                index.put(coord, new BoundaryContact(c.face(), c.axisCoord(), c.y(), c.isPortal(), r.key()));
            }
        }
        return index;
    }

    private static void stitch(ChunkPos posA, ChunkPos posB,
                                BoundaryContact.Face faceA, BoundaryContact.Face faceB,
                                Map<ChunkPos, List<MicroRegion>> results,
                                Map<Long, Long> parent) {
        List<MicroRegion> regionsA = results.get(posA);
        List<MicroRegion> regionsB = results.get(posB);
        if (regionsA == null || regionsB == null) return;
        Map<Long, BoundaryContact> cA = contactIndex(regionsA, faceA);
        Map<Long, BoundaryContact> cB = contactIndex(regionsB, faceB);
        for (Map.Entry<Long, BoundaryContact> e : cA.entrySet()) {
            BoundaryContact cb = cB.get(e.getKey());
            if (cb == null) continue;
            if (e.getValue().isPortal() || cb.isPortal()) continue;
            union(parent, e.getValue().microRegionKey(), cb.microRegionKey());
        }
    }
}
