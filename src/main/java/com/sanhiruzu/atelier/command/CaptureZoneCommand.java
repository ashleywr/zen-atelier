package com.sanhiruzu.atelier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
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
        ZoneData zone = SpaceQuery.getZoneAt(level, playerPos);
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

        // Compute bounding box with 1-block margin so walls/ceiling/floor are included.
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : regionBlocks) {
            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getY() < minY) minY = pos.getY();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getY() > maxY) maxY = pos.getY();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();
        }
        BlockPos origin = new BlockPos(minX - 1, minY - 1, minZ - 1);
        Vec3i size = new Vec3i(maxX - minX + 3, maxY - minY + 3, maxZ - minZ + 3);

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
