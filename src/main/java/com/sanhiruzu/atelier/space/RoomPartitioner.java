package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import java.util.*;

/**
 * Splits a reachable air component into room-sized partitions.
 *
 * <p>A plain flood-fill answers reachability, which is too broad for rooms: a
 * basement stair and a wide sunken living area can both be reachable. Plain
 * vertical air is kept as room volume; labeled connector transitions such as
 * stairs/slabs are where threshold decisions happen. That keeps ordinary room
 * height from being mistaken for a separate floor.</p>
 */
public final class RoomPartitioner {
    private static final int BROAD_VERTICAL_TRANSITION_AREA = 4;

    private RoomPartitioner() {
    }

    public static List<RoomComponent> partition(RoomComponent component) {
        if (component.isEmpty()) return List.of(component);

        List<Set<BlockPos>> floorPlates = findFloorPlates(component);
        if (floorPlates.size() <= 1) return List.of(component);

        Map<BlockPos, Integer> plateByBlock = indexPlates(floorPlates);
        UnionFind unions = new UnionFind(floorPlates.size());
        Map<PlatePair, Set<BlockPos>> transitionFootprints = transitionFootprints(component, plateByBlock);

        for (Map.Entry<PlatePair, Set<BlockPos>> entry : transitionFootprints.entrySet()) {
            if (entry.getValue().size() >= BROAD_VERTICAL_TRANSITION_AREA) {
                PlatePair pair = entry.getKey();
                unions.union(pair.first(), pair.second());
            }
        }

        Map<Integer, Set<BlockPos>> partitions = new HashMap<>();
        for (int plate = 0; plate < floorPlates.size(); plate++) {
            int root = unions.find(plate);
            partitions.computeIfAbsent(root, ignored -> new HashSet<>()).addAll(floorPlates.get(plate));
        }

        if (partitions.size() <= 1) return List.of(component);

        List<RoomComponent> result = new ArrayList<>();
        for (Set<BlockPos> blocks : partitions.values()) {
            Set<RoomTraversalEdge> internalEdges = new HashSet<>();
            for (RoomTraversalEdge edge : component.traversalEdges()) {
                if (blocks.contains(edge.from()) && blocks.contains(edge.to())) {
                    internalEdges.add(edge);
                }
            }
            result.add(new RoomComponent(blocks, Set.of(), internalEdges));
        }
        result.sort(RoomPartitioner::comparePartitions);
        return result;
    }

    public static RoomComponent partitionContaining(RoomComponent component, BlockPos seed) {
        for (RoomComponent partition : partition(component)) {
            if (partition.interiorBlocks().contains(seed)) {
                return partition;
            }
        }
        return component;
    }

    public static List<Set<BlockPos>> partitionBlocks(Set<BlockPos> blocks,
                                                      RoomConnectivity.BlockLookup lookup) {
        if (blocks.isEmpty()) return List.of();

        Set<RoomTraversalEdge> edges = new HashSet<>();
        for (BlockPos pos : blocks) {
            for (RoomTraversalEdge edge : RoomConnectivity.allEdges(lookup, pos)) {
                if (blocks.contains(edge.to())) {
                    edges.add(edge);
                }
            }
        }

        List<Set<BlockPos>> out = new ArrayList<>();
        for (RoomComponent partition : partition(new RoomComponent(blocks, Set.of(), edges))) {
            out.add(partition.interiorBlocks());
        }
        return out;
    }

    private static List<Set<BlockPos>> findFloorPlates(RoomComponent component) {
        Map<BlockPos, Set<BlockPos>> sameLevelNeighbors = new HashMap<>();
        for (BlockPos pos : component.interiorBlocks()) {
            sameLevelNeighbors.put(pos, new HashSet<>());
        }

        for (RoomTraversalEdge edge : component.traversalEdges()) {
            if (!component.interiorBlocks().contains(edge.from())
                    || !component.interiorBlocks().contains(edge.to())) {
                continue;
            }
            if (!isIntraVolume(edge)) continue;

            sameLevelNeighbors.get(edge.from()).add(edge.to());
            sameLevelNeighbors.get(edge.to()).add(edge.from());
        }

        Set<BlockPos> remaining = new HashSet<>(component.interiorBlocks());
        List<Set<BlockPos>> plates = new ArrayList<>();
        while (!remaining.isEmpty()) {
            BlockPos seed = remaining.iterator().next();
            Set<BlockPos> plate = new HashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>();
            remaining.remove(seed);
            plate.add(seed);
            queue.add(seed);

            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                for (BlockPos next : sameLevelNeighbors.getOrDefault(pos, Set.of())) {
                    if (remaining.remove(next)) {
                        plate.add(next);
                        queue.add(next);
                    }
                }
            }

            plates.add(plate);
        }
        return plates;
    }

    private static Map<BlockPos, Integer> indexPlates(List<Set<BlockPos>> floorPlates) {
        Map<BlockPos, Integer> plateByBlock = new HashMap<>();
        for (int i = 0; i < floorPlates.size(); i++) {
            for (BlockPos pos : floorPlates.get(i)) {
                plateByBlock.put(pos, i);
            }
        }
        return plateByBlock;
    }

    private static Map<PlatePair, Set<BlockPos>> transitionFootprints(RoomComponent component,
                                                                      Map<BlockPos, Integer> plateByBlock) {
        Map<PlatePair, Set<BlockPos>> footprints = new HashMap<>();
        for (RoomTraversalEdge edge : component.traversalEdges()) {
            Integer fromPlate = plateByBlock.get(edge.from());
            Integer toPlate = plateByBlock.get(edge.to());
            if (fromPlate == null || toPlate == null || fromPlate.equals(toPlate)) continue;
            if (isIntraVolume(edge)) continue;

            PlatePair pair = PlatePair.of(fromPlate, toPlate);
            footprints.computeIfAbsent(pair, ignored -> new HashSet<>()).add(footprintCell(edge));
        }
        return footprints;
    }

    private static boolean isIntraVolume(RoomTraversalEdge edge) {
        return edge.kind() == RoomTransitionKind.FLAT
                || edge.kind() == RoomTransitionKind.VERTICAL_OPENING
                || edge.kind() == RoomTransitionKind.STEP_LEVEL;
    }

    private static BlockPos footprintCell(RoomTraversalEdge edge) {
        BlockPos basis = edge.connectorBlock() != null ? edge.connectorBlock() : edge.to();
        return new BlockPos(basis.getX(), 0, basis.getZ());
    }

    private static int comparePartitions(RoomComponent first, RoomComponent second) {
        BlockPos a = minPos(first.interiorBlocks());
        BlockPos b = minPos(second.interiorBlocks());
        int byY = Integer.compare(a.getY(), b.getY());
        if (byY != 0) return byY;
        int byX = Integer.compare(a.getX(), b.getX());
        if (byX != 0) return byX;
        return Integer.compare(a.getZ(), b.getZ());
    }

    private static BlockPos minPos(Set<BlockPos> blocks) {
        return blocks.stream()
                .min(Comparator.comparingInt((BlockPos pos) -> pos.getY())
                        .thenComparingInt(Vec3i::getX)
                        .thenComparingInt(Vec3i::getZ))
                .orElse(BlockPos.ZERO);
    }

    private record PlatePair(int first, int second) {
        static PlatePair of(int a, int b) {
            return a < b ? new PlatePair(a, b) : new PlatePair(b, a);
        }
    }

    private record UnionFind(int[] parent) {
        private UnionFind(int size) {
            this(createParent(size));
        }

        private static int[] createParent(int size) {
            int[] parent = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
            return parent;
        }

        private int find(int value) {
            if (parent[value] != value) {
                parent[value] = find(parent[value]);
            }
            return parent[value];
        }

        private void union(int a, int b) {
            int rootA = find(a);
            int rootB = find(b);
            if (rootA != rootB) {
                parent[rootB] = rootA;
            }
        }
    }
}
