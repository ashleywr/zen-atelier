package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

/**
 * Directed edge in the room-air traversal graph.
 *
 * @param from           air cell being expanded from
 * @param to             air cell that can be reached
 * @param kind           geometric reason the cells are connected
 * @param connectorBlock non-air block that creates the edge, such as a stair
 *                       or slab; {@code null} for direct air-to-air faces
 */
public record RoomTraversalEdge(BlockPos from,
                                BlockPos to,
                                RoomTransitionKind kind,
                                @Nullable BlockPos connectorBlock) {
    public RoomTraversalEdge(BlockPos from, BlockPos to, RoomTransitionKind kind) {
        this(from, to, kind, null);
    }
}
