package com.sanhiruzu.atelier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sanhiruzu.atelier.space.BlockPosBounds;
import com.sanhiruzu.atelier.space.SpaceQuery;
import com.sanhiruzu.atelier.space.SpaceRegionRegistry;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

public class CaptureZoneCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("atelier")
                .then(Commands.literal("capture")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> capture(ctx, StringArgumentType.getString(ctx, "name"))))
                        .executes(ctx -> capture(ctx, "zone"))));
    }

    private static int capture(CommandContext<CommandSourceStack> ctx, String name) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();

        BlockPos playerPos = BlockPos.containing(source.getPosition());
        ZoneData zone = SpaceQuery.getRoomAt(level, playerPos);
        if (zone == null) {
            source.sendFailure(Component.literal("Not standing in a classified zone — try moving inside the building and running again."));
            return 0;
        }

        UUID regionId = zone.getRegionId();
        Set<BlockPos> regionBlocks = SpaceRegionRegistry.get(level).getBlocksInRegion(regionId);
        if (regionBlocks.isEmpty()) {
            source.sendFailure(Component.literal("Zone has no region blocks — classification may still be pending."));
            return 0;
        }

        // 1-block margin includes walls, ceiling, and floor in the capture.
        BlockPosBounds bounds = BlockPosBounds.enclosing(regionBlocks).orElseThrow().inflate(1);
        BlockPos origin = bounds.minCorner();
        Vec3i size = bounds.size();

        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, origin, size, false, null);

        try {
            Path dir = Paths.get("zone_captures");
            Files.createDirectories(dir);
            Path file = dir.resolve(name + ".nbt");
            CompoundTag nbt = template.save(new CompoundTag());
            NbtIo.writeCompressed(nbt, file);

            String absPath = file.toAbsolutePath().toString();
            source.sendSuccess(() -> Component.literal(
                    "§aSaved zone '" + name + "' (" + regionBlocks.size() + " blocks, " +
                            size.getX() + "×" + size.getY() + "×" + size.getZ() + ") to:§r\n" + absPath), false);
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to write NBT: " + e.getMessage()));
            return 0;
        }
    }
}
