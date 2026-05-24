package com.sanhiruzu.atelier.ui.adapter;

import com.sanhiruzu.atelier.client.ClientZoneCache;
import com.sanhiruzu.atelier.space.ClassificationState;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.Zone;
import com.sanhiruzu.atelier.space.zone.ZoneAttachment;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public final class ZoneHudAdapter {
    private ZoneHudAdapter() {
    }

    public static Optional<ZoneHudSnapshot> getCurrentSnapshot(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null) return Optional.empty();
        Zone zone = player.getData(ZoneAttachment.ZONE.get()).getCurrentZone();
        if (zone == null) return Optional.empty();
        ZoneData data = ClientZoneCache.getZone(zone.getId());
        if (data == null || data.isDisabled()) return Optional.empty();
        return Optional.of(snapshotFromZoneData(data));
    }

    public static ZoneHudSnapshot snapshotFromZoneData(ZoneData zoneData) {
        if (zoneData.isOutdoor()) {
            return new ZoneHudSnapshot(zoneData.getRegionId(), "Outdoor", "", "", "", "", 0);
        }

        RoomData room = (RoomData) zoneData;
        int score = Math.round(room.getQuality() * 100);

        // Get the generated display name (epithet or generated, with Outdoor prefix if degraded)
        String displayName = getDisplayName(room);

        // Get the room type and mixed-use info for bottom line
        String gameplayInfo = getMixedUseDisplay(room);

        // Get quality breakdown for HUD detail display
        String qualityBreakdown = formatQualityBreakdown(room);

        return new ZoneHudSnapshot(
                room.getRegionId(),
                displayName,                    // Top line: "Scarlet Oak Bedroom"
                qualityBreakdown,               // uniqueName field: quality breakdown
                "",
                "",
                gameplayInfo,                   // Bottom line: "Bedroom | Workshop | Atelier"
                score
        );
    }

    private static String formatQualityBreakdown(RoomData room) {
        var breakdown = room.getQualityBreakdown();
        if (breakdown == null) return "";

        int size = Math.round(breakdown.sizeScore * 100);
        int enclosure = Math.round(breakdown.enclosureScore * 100);
        int furniture = Math.round(breakdown.furnitureScore * 100);
        int theme = Math.round(breakdown.themeScore * 100);

        return String.format("Size:%d%% Encl:%d%% Furn:%d%% Theme:%d%%", size, enclosure, furniture, theme);
    }

    private static String getDisplayName(RoomData room) {
        String baseName;

        // Use epithet name if available, otherwise generated name, otherwise zone type, otherwise fallback
        if (room.getEpithetName() != null) {
            baseName = room.getEpithetName();
        } else if (room.getGeneratedName() != null && !room.getGeneratedName().isEmpty()) {
            baseName = room.getGeneratedName();
        } else if (room.getZoneTypeId() != null) {
            baseName = formatZoneTypeName(room.getZoneTypeId());
        } else {
            baseName = "Room";
        }

        return baseName;
    }

    private static String getMixedUseDisplay(RoomData room) {
        // Degraded status occupies the leading slot
        String degradedPrefix = "";
        if (room.isDegraded()) {
            degradedPrefix = room.getEnclosureScore() < 0.5f ? "Open-air" : "Exposed";
        }

        if (room.getZoneTypeId() == null) {
            return degradedPrefix;
        }

        String primaryType = formatZoneTypeName(room.getZoneTypeId());
        java.util.Map<String, Integer> signals = room.getSignalCounts();

        java.util.Map<String, String[]> typeSignals = java.util.Map.ofEntries(
                java.util.Map.entry("bed", new String[]{"Bedroom"}),
                java.util.Map.entry("enchanting_table", new String[]{"Enchanting"}),
                java.util.Map.entry("brewing", new String[]{"Atelier"}),
                java.util.Map.entry("cauldron", new String[]{"Atelier", "Tannery"}),
                java.util.Map.entry("furnace", new String[]{"Kitchen", "Smithy"}),
                java.util.Map.entry("bookshelf", new String[]{"Library", "Enchanting"}),
                java.util.Map.entry("chest", new String[]{"Storage"}),
                java.util.Map.entry("loom", new String[]{"Loom"}),
                java.util.Map.entry("lectern", new String[]{"Library", "Map"}),
                java.util.Map.entry("stonecutter", new String[]{"Masonry"}),
                java.util.Map.entry("cartography", new String[]{"Map"}),
                java.util.Map.entry("composter", new String[]{"Gardener"})
        );

        java.util.Set<String> detectedTypes = new java.util.LinkedHashSet<>();
        detectedTypes.add(primaryType);
        for (java.util.Map.Entry<String, Integer> signal : signals.entrySet()) {
            if (signal.getValue() >= 2) {
                for (java.util.Map.Entry<String, String[]> typeEntry : typeSignals.entrySet()) {
                    if (signal.getKey().toLowerCase().contains(typeEntry.getKey())) {
                        for (String type : typeEntry.getValue()) {
                            if (!type.equals(primaryType)) {
                                detectedTypes.add(type);
                            }
                        }
                    }
                }
            }
        }

        // Show type(s) when there's a flavor name (so the player knows what the room is)
        // or whenever there's genuine mixed-use. Single-type with no flavor: type is already
        // the name fallback, showing it again in the subtitle would repeat.
        boolean hasFlavorName = room.getEpithetName() != null
                || (room.getGeneratedName() != null && !room.getGeneratedName().isEmpty());
        String typePart = (detectedTypes.size() > 1 || hasFlavorName)
                ? String.join(" | ", detectedTypes)
                : "";

        if (degradedPrefix.isEmpty()) return typePart;
        if (typePart.isEmpty()) return degradedPrefix;
        return degradedPrefix + " · " + typePart;
    }

    private static String formatZoneTypeName(ResourceLocation id) {
        // Convention: room profiles ship a translation key "room_type.<namespace>.<path>" so
        // localized HUD names live in the lang file rather than being derived from the path.
        // Fall back to a sentence-cased path if the key is missing.
        String key = "room_type." + id.getNamespace() + "." + id.getPath();
        Component component = Component.translatable(key);
        String localized = component.getString();
        if (!localized.equals(key)) return localized;

        String path = id.getPath().replace('_', ' ');
        return path.isEmpty() ? id.toString() : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    public static Optional<ZoneHudSnapshot> snapshotForClassification(ClassificationState state, int regionCount, int chunkX, int chunkZ) {
        if (state != ClassificationState.INSIDE && state != ClassificationState.PARTIAL) {
            return Optional.empty();
        }

        int score = state == ClassificationState.INSIDE ? 60 : 30;
        String stateName = state == ClassificationState.INSIDE ? "Inside Space" : "Partial Space";
        String detail = regionCount <= 0
                ? "Zone backend pending"
                : regionCount + " detected region(s)";

        UUID stableChunkId = UUID.nameUUIDFromBytes(("chunk:" + chunkX + ":" + chunkZ).getBytes(StandardCharsets.UTF_8));
        return Optional.of(new ZoneHudSnapshot(stableChunkId, stateName, "", "", "", detail, score));
    }

    public record ZoneHudSnapshot(
            UUID id,
            String name,
            String generatedName,
            String uniqueName,
            String customName,
            String activeProfiles,
            int score
    ) {
    }
}
