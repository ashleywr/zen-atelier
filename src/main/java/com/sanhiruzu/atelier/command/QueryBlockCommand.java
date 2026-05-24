package com.sanhiruzu.atelier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sanhiruzu.atelier.space.ChunkClassificationAttachment;
import com.sanhiruzu.atelier.space.ChunkClassificationData;
import com.sanhiruzu.atelier.space.ClassificationState;
import com.sanhiruzu.atelier.space.SpaceRegionRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

public class QueryBlockCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("atelier")
                .then(Commands.literal("queryblock")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> queryBlock(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "y"),
                                                        IntegerArgumentType.getInteger(ctx, "z")
                                                ))
                                        )
                                )
                        )
                        .executes(ctx -> queryBlockAtPlayer(ctx))
                )
        );
    }

    private static int queryBlockAtPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (source.getEntity() == null) {
            source.sendFailure(Component.literal("Must be executed by a player"));
            return 0;
        }
        BlockPos pos = new BlockPos(
                (int) source.getEntity().getX(),
                (int) source.getEntity().getY(),
                (int) source.getEntity().getZ()
        );
        return queryBlock(ctx, pos.getX(), pos.getY(), pos.getZ());
    }

    private static int queryBlock(CommandContext<CommandSourceStack> ctx, int x, int y, int z) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = new BlockPos(x, y, z);

        // Get chunk info
        ChunkAccess chunk = level.getChunk(pos);
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        // Get chunk data
        ChunkClassificationData data = ChunkClassificationAttachment.get(chunk);

        // Get local coordinates within chunk
        int localX = x & 15;
        int localZ = z & 15;

        // Get classification state
        ClassificationState state = data.getBlockState(localX, y, localZ);

        // Get region info
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(level);
        var region = registry.getRegionAt(pos);

        // Get zone info
        com.sanhiruzu.atelier.space.zone.ZoneRegistry zoneRegistry = com.sanhiruzu.atelier.space.zone.ZoneRegistry.get(level);
        com.sanhiruzu.atelier.space.zone.ZoneData zoneData = null;
        if (region != null) {
            zoneData = zoneRegistry.getZone(region.getId());
        }

        // Build response
        StringBuilder response = new StringBuilder();
        response.append("\n=== BLOCK QUERY ===\n");
        response.append(String.format("Position: [%d, %d, %d]\n", x, y, z));
        response.append(String.format("Chunk: [%d, %d]\n", chunkX, chunkZ));
        response.append(String.format("Local: [%d, %d, %d]\n", localX, y & 15, localZ));
        response.append(String.format("Block: %s\n", level.getBlockState(pos).getBlock().getName().getString()));
        response.append(String.format("Classification State: %s\n", state));

        if (region != null) {
            response.append(String.format("Region UUID: %s\n", region.getId()));
            response.append(String.format("Region Type: %s\n", region.getType()));
            response.append(String.format("Region Volume: %d\n", region.getVolume()));
            response.append(String.format("Region Opening Area: %d\n", region.getOpeningArea()));

            // Add zone info if available
            if (zoneData != null) {
                response.append(String.format("Zone Type: %s\n", zoneData.isOutdoor() ? "Outdoor" : (zoneData instanceof com.sanhiruzu.atelier.space.zone.RoomData ? "Room" : "Zone")));
                response.append(String.format("Zone Volume: %d\n", zoneData.getVolume()));
                response.append(String.format("Zone Enclosure Score: %.2f\n", zoneData.getEnclosureScore()));
                if (zoneData instanceof com.sanhiruzu.atelier.space.zone.RoomData room) {
                    response.append(String.format("Room Quality: %.2f\n", room.getQuality()));
                    response.append(String.format("Room Degraded: %s\n", room.isDegraded()));
                    String customName = zoneRegistry.getCustomName(region.getId());
                    if (customName != null) {
                        response.append(String.format("Custom Name: %s\n", customName));
                    }
                }
            } else {
                response.append("Zone: NOT YET EVALUATED\n");
            }
        } else {
            response.append("Region: NONE (not part of any region)\n");
        }

        source.sendSuccess(() -> Component.literal(response.toString()), false);
        return 1;
    }
}
