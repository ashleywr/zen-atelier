package com.sanhiruzu.atelier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.sanhiruzu.atelier.network.RemoveZonePayload;
import com.sanhiruzu.atelier.space.SpaceRegionRegistry;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

public class DeleteZoneCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("atelier")
                .then(Commands.literal("deletezone")
                        .then(Commands.literal("all")
                                .executes(DeleteZoneCommand::deleteAll))));
    }

    private static int deleteAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();

        ZoneRegistry zoneRegistry = ZoneRegistry.get(level);
        SpaceRegionRegistry regionRegistry = SpaceRegionRegistry.get(level);

        var zoneIds = zoneRegistry.getAllZoneIds();
        int zoneCount = zoneIds.size();

        // Notify clients to remove each zone before clearing server state
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("ZenAtelier/Zones");
        for (java.util.UUID id : zoneIds) {
            ZoneData zone = zoneRegistry.getZone(id);
            if (zone != null) {
                logger.info("[DELETEZONE] Zone {}: vol={}, enc={}, bounds=({},{}) to ({},{})",
                        id.toString().substring(0, 8),
                        zone.getVolume(),
                        String.format("%.2f", zone.getEnclosureScore()),
                        zone.getMinX(), zone.getMinZ(), zone.getMaxX(), zone.getMaxZ());
                if (zone.hasSpatialExtent()) {
                    RemoveZonePayload packet = new RemoveZonePayload(id,
                            zone.getMinX(), zone.getMinY(), zone.getMinZ(),
                            zone.getMaxX(), zone.getMaxY(), zone.getMaxZ());
                    PacketDistributor.sendToPlayersInDimension(level, packet);
                }
            }
        }

        // Remove all zones and regions
        zoneRegistry.invalidateAll();
        regionRegistry.clear();

        source.sendSuccess(() -> Component.literal(
                        "§aDeleted all zones (§r" + zoneCount + "§a)§r"),
                true);

        return 1;
    }
}
