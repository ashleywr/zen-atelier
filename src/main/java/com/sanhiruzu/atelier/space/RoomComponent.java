package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * One connected room-space component produced by {@link RoomComponentScanner}.
 *
 * <p>The interior set is the air volume a zone may own. The boundary set is
 * observational metadata: it records non-interior positions touched by the scan
 * so future reconciliation/debug tools can explain which walls, doors, stairs,
 * or already-owned cells stopped expansion. Boundary blocks are intentionally
 * not counted as room volume.</p>
 */
public final class RoomComponent {
    private final Set<BlockPos> interiorBlocks;
    private final Set<BlockPos> boundaryBlocks;
    private final Set<RoomTraversalEdge> traversalEdges;

    RoomComponent(Set<BlockPos> interiorBlocks, Set<BlockPos> boundaryBlocks) {
        this(interiorBlocks, boundaryBlocks, Set.of());
    }

    RoomComponent(Set<BlockPos> interiorBlocks,
                  Set<BlockPos> boundaryBlocks,
                  Set<RoomTraversalEdge> traversalEdges) {
        this.interiorBlocks = Set.copyOf(interiorBlocks);
        this.boundaryBlocks = Set.copyOf(boundaryBlocks);
        this.traversalEdges = Set.copyOf(traversalEdges);
    }

    public Set<BlockPos> interiorBlocks() {
        return interiorBlocks;
    }

    public Set<BlockPos> boundaryBlocks() {
        return boundaryBlocks;
    }

    public Set<RoomTraversalEdge> traversalEdges() {
        return traversalEdges;
    }

    public int volume() {
        return interiorBlocks.size();
    }

    public boolean isEmpty() {
        return interiorBlocks.isEmpty();
    }
}
