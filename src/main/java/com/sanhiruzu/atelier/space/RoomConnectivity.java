package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shared room traversal rules.
 *
 * <p>Rooms own air cells as volume. Non-air blocks such as stairs and slabs are
 * not mapped as interior volume here; they add graph edges between nearby air
 * cells when Minecraft building language says those cells are part of the same
 * reachable interior space. Keeping those rules centralized prevents each
 * flood-fill caller from growing its own incompatible definition of a room.</p>
 */
public final class RoomConnectivity {
    private static final int CHUNK_WIDTH = 16;
    private static final int MIN_Y = -64;
    private static final int MAX_Y_EXCLUSIVE = 320;
    private static final double EPSILON = 1.0E-6;
    private static final double MIN_STEP_HEIGHT = 0.25;
    private static final double MAX_PARTIAL_STEP_HEIGHT = 0.875;

    private RoomConnectivity() {
    }

    public static BlockLookup forChunk(ChunkAccess chunk) {
        return new BlockLookup() {
            @Override
            public BlockState blockState(BlockPos pos) {
                return chunk.getBlockState(pos);
            }

            @Override
            public FluidState fluidState(BlockPos pos) {
                return chunk.getFluidState(pos);
            }

            @Override
            public VoxelShape collisionShape(BlockPos pos) {
                return RoomConnectivity.collisionShape(chunk, pos);
            }

            @Override
            public boolean canRead(BlockPos pos) {
                int localX = pos.getX() - chunk.getPos().getMinBlockX();
                int localZ = pos.getZ() - chunk.getPos().getMinBlockZ();
                int y = pos.getY();
                return localX >= 0 && localX < CHUNK_WIDTH
                        && localZ >= 0 && localZ < CHUNK_WIDTH
                        && y >= MIN_Y && y < MAX_Y_EXCLUSIVE;
            }
        };
    }

    public static BlockLookup forLevel(ServerLevel level) {
        return new BlockLookup() {
            @Override
            public BlockState blockState(BlockPos pos) {
                return level.getBlockState(pos);
            }

            @Override
            public FluidState fluidState(BlockPos pos) {
                return level.getFluidState(pos);
            }

            @Override
            public VoxelShape collisionShape(BlockPos pos) {
                return RoomConnectivity.collisionShape(level, pos);
            }

            @Override
            public boolean canRead(BlockPos pos) {
                return pos.getY() >= MIN_Y && pos.getY() < MAX_Y_EXCLUSIVE;
            }
        };
    }

    public static boolean isRoomAir(BlockLookup lookup, BlockPos pos) {
        return lookup.canRead(pos)
                && lookup.blockState(pos).isAir()
                && !lookup.fluidState(pos).is(Fluids.WATER);
    }

    public static List<BlockPos> allNeighbors(BlockLookup lookup, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (RoomTraversalEdge edge : allEdges(lookup, pos)) {
            neighbors.add(edge.to());
        }
        return neighbors;
    }

    public static List<BlockPos> horizontalNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (RoomTraversalEdge edge : horizontalEdges(pos)) {
            neighbors.add(edge.to());
        }
        return neighbors;
    }

    public static List<RoomTraversalEdge> allEdges(BlockLookup lookup, BlockPos pos) {
        List<RoomTraversalEdge> edges = faceEdges(pos, Arrays.asList(Direction.values()));
        addStepEdges(lookup, pos, edges);
        return edges;
    }

    public static List<RoomTraversalEdge> horizontalEdges(BlockPos pos) {
        return faceEdges(pos, Direction.Plane.HORIZONTAL);
    }

    private static List<RoomTraversalEdge> faceEdges(BlockPos pos, Iterable<Direction> directions) {
        List<RoomTraversalEdge> edges = new ArrayList<>(6);
        for (Direction dir : directions) {
            RoomTransitionKind kind = dir.getAxis() == Direction.Axis.Y
                    ? RoomTransitionKind.VERTICAL_OPENING
                    : RoomTransitionKind.FLAT;
            edges.add(new RoomTraversalEdge(pos, pos.relative(dir), kind));
        }
        return edges;
    }

    private static void addStepEdges(BlockLookup lookup, BlockPos pos, List<RoomTraversalEdge> edges) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos horizontal = pos.relative(dir);
            if (isStepConnector(lookup, horizontal)) {
                edges.add(new RoomTraversalEdge(pos, horizontal.above(),
                        RoomTransitionKind.STEP_UP, horizontal));
            }

            BlockPos lowerStep = horizontal.below();
            if (isStepConnector(lookup, lowerStep)) {
                edges.add(new RoomTraversalEdge(pos, lowerStep.above(),
                        RoomTransitionKind.STEP_LEVEL, lowerStep));
            }

            // If the current air cell is standing above a connector, allow the
            // inverse diagonal edge back down to the adjacent lower air cell.
            // Without this, a flood-fill seeded upstairs can miss the lower room.
            if (isStepConnector(lookup, pos.below())) {
                edges.add(new RoomTraversalEdge(pos, lowerStep,
                        RoomTransitionKind.STEP_DOWN, pos.below()));
            }
        }
    }

    private static VoxelShape collisionShape(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos).getCollisionShape(world, pos);
    }

    private static boolean isStepConnector(BlockLookup lookup, BlockPos pos) {
        if (!lookup.canRead(pos)) return false;
        BlockState state = lookup.blockState(pos);
        if (state.is(BlockTags.STAIRS) || state.is(BlockTags.SLABS)) return true;
        return isPartialWalkableCollision(lookup.collisionShape(pos));
    }

    private static boolean isPartialWalkableCollision(VoxelShape shape) {
        if (shape == null || shape == Shapes.empty() || shape.isEmpty()) return false;

        // Shape-derived connectors let modded blocks participate without a new
        // tag every time. Keep the band conservative: carpets/floor trim are too
        // low to imply a room threshold, while full blocks are ordinary walls.
        double minY = shape.min(Direction.Axis.Y);
        double maxY = shape.max(Direction.Axis.Y);
        return minY <= EPSILON
                && maxY >= MIN_STEP_HEIGHT
                && maxY <= MAX_PARTIAL_STEP_HEIGHT;
    }

    public interface BlockLookup {
        BlockState blockState(BlockPos pos);

        FluidState fluidState(BlockPos pos);

        VoxelShape collisionShape(BlockPos pos);

        boolean canRead(BlockPos pos);
    }
}
