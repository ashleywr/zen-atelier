package com.sanhiruzu.atelier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.sanhiruzu.atelier.space.SpaceQuery;
import com.sanhiruzu.atelier.space.zone.QualityEvaluator;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

public class InspectZoneCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("atelier")
                .then(Commands.literal("inspectzone")
                        .executes(InspectZoneCommand::inspect)));
    }

    private static int inspect(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        ZoneData zone = SpaceQuery.getZoneAt(level, pos);
        if (zone == null) {
            source.sendFailure(Component.literal("Not standing in a classified zone."));
            return 0;
        }

        String json = buildJson(zone);

        try {
            Path dir = Paths.get("zone_inspections");
            Files.createDirectories(dir);
            String filename = "zone_" + zone.getRegionId().toString().substring(0, 8) + ".json";
            Path file = dir.resolve(filename);
            Files.writeString(file, json);

            source.sendSuccess(() -> Component.literal(
                    "§aZone inspection saved to: §r" + file.toAbsolutePath()), false);
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to write JSON: " + e.getMessage()));
            return 0;
        }
    }

    private static String buildJson(ZoneData zone) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // Identity
        sb.append("  \"id\": \"").append(zone.getRegionId()).append("\",\n");
        sb.append("  \"type\": \"").append(zone.isOutdoor() ? "outdoor" : "room").append("\",\n");

        if (zone instanceof RoomData room) {
            // Type and name
            String typeId = room.getZoneTypeId() != null ? room.getZoneTypeId().toString() : null;
            appendNullableString(sb, "zone_type", typeId);
            appendNullableString(sb, "epithet", room.getEpithetName());
            appendNullableString(sb, "generated_name", room.getGeneratedName());
            sb.append("  \"degraded\": ").append(room.isDegraded()).append(",\n");

            // Spatial
            sb.append("  \"dimensions\": {\n");
            sb.append("    \"min\": [").append(zone.getMinX()).append(", ").append(zone.getMinY()).append(", ").append(zone.getMinZ()).append("],\n");
            sb.append("    \"max\": [").append(zone.getMaxX()).append(", ").append(zone.getMaxY()).append(", ").append(zone.getMaxZ()).append("],\n");
            sb.append("    \"size\": [")
                    .append(zone.getMaxX() - zone.getMinX() + 1).append(", ")
                    .append(zone.getMaxY() - zone.getMinY() + 1).append(", ")
                    .append(zone.getMaxZ() - zone.getMinZ() + 1).append("]\n");
            sb.append("  },\n");
            sb.append("  \"volume\": ").append(room.getVolume()).append(",\n");
            sb.append("  \"floor_area\": ").append(Math.round((float) room.getVolume() / room.getSpaciousness())).append(",\n");
            sb.append("  \"spaciousness\": ").append(String.format("%.2f", room.getSpaciousness())).append(",\n");
            sb.append("  \"enclosure_score\": ").append(String.format("%.3f", room.getEnclosureScore())).append(",\n");

            // Quality
            sb.append("  \"quality\": ").append(String.format("%.3f", room.getQuality())).append(",\n");
            QualityEvaluator.QualityBreakdown bd = room.getQualityBreakdown();
            if (bd != null) {
                sb.append("  \"quality_breakdown\": {\n");
                sb.append("    \"size\": ").append(String.format("%.3f", bd.sizeScore)).append(",\n");
                sb.append("    \"enclosure\": ").append(String.format("%.3f", bd.enclosureScore)).append(",\n");
                sb.append("    \"furniture\": ").append(String.format("%.3f", bd.furnitureScore)).append(",\n");
                sb.append("    \"theme\": ").append(String.format("%.3f", bd.themeScore)).append(",\n");
                sb.append("    \"total\": ").append(String.format("%.3f", bd.totalQuality)).append("\n");
                sb.append("  },\n");
            }

            // Blocks
            appendBlockMap(sb, "furniture_blocks", room.getFurnitureCounts());
            appendBlockMap(sb, "surface_blocks", room.getSurfaceCounts());
            appendBlockMap(sb, "signals", room.getSignalCounts());
        } else {
            sb.append("  \"volume\": ").append(zone.getVolume()).append(",\n");
            sb.append("  \"enclosure_score\": ").append(String.format("%.3f", zone.getEnclosureScore())).append(",\n");
            sb.append("  \"dimensions\": {\n");
            sb.append("    \"min\": [").append(zone.getMinX()).append(", ").append(zone.getMinY()).append(", ").append(zone.getMinZ()).append("],\n");
            sb.append("    \"max\": [").append(zone.getMaxX()).append(", ").append(zone.getMaxY()).append(", ").append(zone.getMaxZ()).append("]\n");
            sb.append("  }\n");
        }

        // Trim trailing comma from last field if needed
        int lastComma = sb.lastIndexOf(",\n");
        if (lastComma == sb.length() - 2) {
            sb.replace(lastComma, lastComma + 2, "\n");
        }

        sb.append("}");
        return sb.toString();
    }

    private static void appendNullableString(StringBuilder sb, String key, String value) {
        if (value != null) {
            sb.append("  \"").append(key).append("\": \"").append(value).append("\",\n");
        } else {
            sb.append("  \"").append(key).append("\": null,\n");
        }
    }

    private static void appendBlockMap(StringBuilder sb, String key, Map<String, Integer> map) {
        sb.append("  \"").append(key).append("\": {\n");
        Map<String, Integer> sorted = new TreeMap<>(map);
        int i = 0;
        for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
            sb.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue());
            if (++i < sorted.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("  },\n");
    }
}
