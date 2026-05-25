package com.sanhiruzu.atelier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.sanhiruzu.atelier.space.ChunkClassificationAttachment;
import com.sanhiruzu.atelier.space.SpaceRegionRegistry;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Developer-only world fixtures for validating room discovery by eye.
 *
 * <p>These fixtures intentionally use ordinary block placement, then force the
 * same chunk bootstrap path used by normal world loading. That gives us a fast
 * feedback loop: spawn the shape, walk it in-game, inspect/debug the resulting
 * zones, and turn any bad behavior into a GameTest.</p>
 */
@SuppressWarnings("SameReturnValue")
public class RoomFixtureCommand {
    private static final int CLEAR_X = 18;
    private static final int CLEAR_Y_BELOW = 7;
    private static final int CLEAR_Y_ABOVE = 8;
    private static final int CLEAR_Z = 18;
    private static final int FIXTURE_GROUND_FALLBACK_Y = -60;
    private static final int GROUND_SEARCH_RADIUS = 12;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("atelier")
                .then(Commands.literal("fixture")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("normal")
                                .executes(ctx -> spawn(ctx, Fixture.NORMAL)))
                        .then(Commands.literal("sunken")
                                .executes(ctx -> spawn(ctx, Fixture.SUNKEN)))
                        .then(Commands.literal("basement")
                                .executes(ctx -> spawn(ctx, Fixture.BASEMENT)))
                        .then(Commands.literal("stair_edge")
                                .executes(ctx -> spawn(ctx, Fixture.STAIR_EDGE)))));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, Fixture fixture) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos player = BlockPos.containing(source.getPosition());
        int originX = player.getX() + 4;
        int originZ = player.getZ() + 4;
        int groundY = findGroundY(level, new BlockPos(originX, player.getY(), originZ));
        BlockPos origin = new BlockPos(originX, groundY, originZ);
        BlockPos clearMin = origin.offset(-2, -CLEAR_Y_BELOW, -2);
        BlockPos clearMax = origin.offset(CLEAR_X, CLEAR_Y_ABOVE, CLEAR_Z);

        // Fixture generation is destructive by design. Reset room state so stale
        // mappings from an earlier fixture cannot explain the new result.
        ZoneRegistry.get(level).invalidateAll();
        SpaceRegionRegistry.get(level).clear();
        clearBox(level, clearMin, clearMax);
        restoreGrassPad(level, clearMin, clearMax, origin.getY());

        switch (fixture) {
            case NORMAL -> buildNormalRoom(level, origin);
            case SUNKEN -> buildSunkenRoom(level, origin);
            case BASEMENT -> buildBasementRoom(level, origin);
            case STAIR_EDGE -> buildStairEdgeRoom(level, origin);
        }

        int bootstrapped = bootstrapTouchedChunks(level, clearMin, clearMax);
        int rooms = ZoneRegistry.get(level).getAllRoomIds().size();
        source.sendSuccess(() -> Component.literal("Spawned " + fixture.commandName
                + " fixture at " + origin.toShortString()
                + "; bootstrapped " + bootstrapped + " chunk(s), rooms=" + rooms), true);
        return 1;
    }

    private static void buildNormalRoom(ServerLevel level, BlockPos origin) {
        buildShellRoom(level, origin, 7, 7, 4, DoorSide.SOUTH, 3);
        placeDoor(level, origin.offset(3, 1, 0), Direction.SOUTH);
        level.setBlockAndUpdate(origin.offset(3, 1, 3), Blocks.CRAFTING_TABLE.defaultBlockState());
    }

    private static void buildSunkenRoom(ServerLevel level, BlockPos origin) {
        buildShellRoom(level, origin, 9, 9, 5, DoorSide.SOUTH, 4);
        placeDoor(level, origin.offset(4, 1, 0), Direction.SOUTH);

        // A broad 3x3 lowered area in the same shell should feel like one room,
        // unlike a narrow stairwell to a basement.
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
        for (int x = 3; x <= 5; x++) {
            placeStair(level, origin.offset(x, 0, 2), Direction.NORTH);
            placeStair(level, origin.offset(x, 0, 6), Direction.SOUTH);
        }
        for (int z = 3; z <= 5; z++) {
            placeStair(level, origin.offset(2, 0, z), Direction.WEST);
            placeStair(level, origin.offset(6, 0, z), Direction.EAST);
        }
        level.setBlockAndUpdate(origin.offset(4, 0, 4), Blocks.CAMPFIRE.defaultBlockState());
    }

    private static void buildBasementRoom(ServerLevel level, BlockPos origin) {
        buildShellRoom(level, origin, 9, 9, 5, DoorSide.SOUTH, 4);
        placeDoor(level, origin.offset(4, 1, 0), Direction.SOUTH);

        BlockPos basement = origin.offset(0, -5, 0);
        buildShellRoom(level, basement, 9, 9, 4, DoorSide.NONE, 0);

        // One-block-wide stairwell through the floor/ceiling. Carve the whole
        // stair tunnel, not just the vertical shaft, so the player has headroom
        // while walking down it.
        for (int z = 2; z <= 5; z++) {
            for (int y = -4; y <= 2; y++) {
                level.setBlockAndUpdate(origin.offset(4, y, z), Blocks.AIR.defaultBlockState());
            }
        }
        placeStair(level, origin.offset(4, -4, 5), Direction.NORTH);
        placeStair(level, origin.offset(4, -3, 4), Direction.NORTH);
        placeStair(level, origin.offset(4, -2, 3), Direction.NORTH);
        placeStair(level, origin.offset(4, -1, 2), Direction.NORTH);

        // Attached ladder on the basement wall for inspecting vertical connector
        // behavior without obstructing the stair path.
        for (int y = 1; y <= 3; y++) {
            level.setBlockAndUpdate(basement.offset(7, y, 4), Blocks.LADDER.defaultBlockState()
                    .setValue(LadderBlock.FACING, Direction.WEST));
        }
        level.setBlockAndUpdate(basement.offset(2, 1, 2), Blocks.FURNACE.defaultBlockState());
    }

    private static void buildStairEdgeRoom(ServerLevel level, BlockPos origin) {
        buildShellRoom(level, origin, 8, 8, 5, DoorSide.SOUTH, 3);
        placeDoor(level, origin.offset(3, 1, 0), Direction.SOUTH);

        // A small loft on one edge reproduces the original "stairs at the edge"
        // case: the upper air should not be missed just because it is diagonal
        // through stair geometry rather than face-adjacent air.
        for (int x = 4; x <= 6; x++) {
            for (int z = 4; z <= 6; z++) {
                level.setBlockAndUpdate(origin.offset(x, 2, z), Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
        placeStair(level, origin.offset(3, 1, 4), Direction.EAST);
        placeStair(level, origin.offset(4, 2, 4), Direction.EAST);
    }

    private static void buildShellRoom(ServerLevel level, BlockPos origin,
                                       int width, int depth, int height,
                                       DoorSide doorSide, int doorOffset) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = 0; z < depth; z++) {
                    boolean floorOrCeiling = y == 0 || y == height;
                    boolean wall = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                    boolean door = doorSide.isDoorCell(x, z, width, depth, doorOffset) && (y == 1 || y == 2);
                    if ((floorOrCeiling || wall) && !door) {
                        level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.STONE_BRICKS.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void placeDoor(ServerLevel level, BlockPos lower, Direction facing) {
        level.setBlockAndUpdate(lower, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        level.setBlockAndUpdate(lower.above(), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static void placeStair(ServerLevel level, BlockPos pos, Direction facing) {
        BlockState state = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, facing);
        level.setBlockAndUpdate(pos, state);
    }

    private static void clearBox(ServerLevel level, BlockPos min, BlockPos max) {
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static int findGroundY(ServerLevel level, BlockPos target) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        for (int radius = 0; radius <= GROUND_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int y = findTopSolidY(level, target.getX() + dx, target.getZ() + dz, minY, maxY);
                    if (y != Integer.MIN_VALUE) {
                        return y;
                    }
                }
            }
        }
        return Math.max(minY, FIXTURE_GROUND_FALLBACK_Y);
    }

    private static int findTopSolidY(ServerLevel level, int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static void restoreGrassPad(ServerLevel level, BlockPos min, BlockPos max, int groundY) {
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                level.setBlockAndUpdate(new BlockPos(x, groundY - 1, z), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(x, groundY, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
    }

    private static int bootstrapTouchedChunks(ServerLevel level, BlockPos min, BlockPos max) {
        ZoneRegistry registry = ZoneRegistry.get(level);
        int count = 0;
        int minChunkX = min.getX() >> 4;
        int maxChunkX = max.getX() >> 4;
        int minChunkZ = min.getZ() >> 4;
        int maxChunkZ = max.getZ() >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz);
                ChunkClassificationAttachment.get(chunk).setDirty(true);
                registry.clearRestoredDataForChunk(cx, cz);
                registry.bootstrapChunk(chunk, level);
                count++;
            }
        }
        return count;
    }

    private enum Fixture {
        NORMAL("normal"),
        SUNKEN("sunken"),
        BASEMENT("basement"),
        STAIR_EDGE("stair_edge");

        private final String commandName;

        Fixture(String commandName) {
            this.commandName = commandName;
        }
    }

    private enum DoorSide {
        NONE,
        SOUTH;

        private boolean isDoorCell(int x, int z, int width, int depth, int doorOffset) {
            return this == SOUTH && z == 0 && x == doorOffset;
        }
    }
}
