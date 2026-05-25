package com.sanhiruzu.atelier.zone.naming;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanhiruzu.atelier.space.zone.RoomData;

import java.util.List;
import java.util.Map;

public record RoomEpithet(
        String id,
        String displayName,
        List<String> appliesTo,
        int minimumScore,
        int priority,
        Map<String, Integer> requiredFeatures,
        Map<String, Integer> maximumFeatures
) {
    public static final Codec<RoomEpithet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(RoomEpithet::id),
            Codec.STRING.fieldOf("display_name").forGetter(RoomEpithet::displayName),
            Codec.STRING.listOf().fieldOf("applies_to").orElse(List.of()).forGetter(RoomEpithet::appliesTo),
            Codec.INT.fieldOf("minimum_score").orElse(0).forGetter(RoomEpithet::minimumScore),
            Codec.INT.fieldOf("priority").orElse(0).forGetter(RoomEpithet::priority),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("required_features").orElse(Map.of()).forGetter(RoomEpithet::requiredFeatures),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("maximum_features").orElse(Map.of()).forGetter(RoomEpithet::maximumFeatures)
    ).apply(instance, RoomEpithet::new));

    public RoomEpithet {
        appliesTo = List.copyOf(appliesTo);
        requiredFeatures = Map.copyOf(requiredFeatures);
        maximumFeatures = Map.copyOf(maximumFeatures);
    }

    public boolean matches(RoomData room, int score) {
        if (room.getZoneTypeId() == null) {
            return false;
        }

        String profileId = room.getZoneTypeId().toString();
        if (!appliesTo.isEmpty() && !appliesTo.contains(profileId)) {
            return false;
        }
        if (score < minimumScore) {
            return false;
        }

        // Check required features against furniture counts in RoomData
        for (Map.Entry<String, Integer> entry : requiredFeatures.entrySet()) {
            if (getFeatureCount(room, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }

        // Check maximum features against furniture counts in RoomData
        for (Map.Entry<String, Integer> entry : maximumFeatures.entrySet()) {
            if (getFeatureCount(room, entry.getKey()) > entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    private static int getFeatureCount(RoomData room, String key) {
        // Map feature names to furniture counts in RoomData
        return switch (key) {
            case "beds" -> room.getFurnitureCounts().getOrDefault("bed", 0);
            case "bookshelves" -> room.getFurnitureCounts().getOrDefault("bookshelf", 0);
            case "storage" -> room.getFurnitureCounts().getOrDefault("storage", 0);
            case "lighting" -> room.getFurnitureCounts().getOrDefault("light", 0);
            case "cooking_block" -> room.getFurnitureCounts().getOrDefault("cooking_block", 0);
            case "furnace", "heat_sources" -> room.getFurnitureCounts().getOrDefault("furnace", 0);
            case "cauldrons" -> room.getFurnitureCounts().getOrDefault("cauldron", 0);
            case "glass" -> room.getFurnitureCounts().getOrDefault("glass", 0);
            case "plants" -> room.getFurnitureCounts().getOrDefault("plant", 0);
            case "smithing_table", "smithing_tables" -> room.getFurnitureCounts().getOrDefault("smithing_table", 0);
            case "enchanting_table", "enchanting_tables" ->
                    room.getFurnitureCounts().getOrDefault("enchanting_table", 0);
            case "crafting" -> room.getFurnitureCounts().getOrDefault("crafting", 0);
            case "stone" -> room.getFurnitureCounts().getOrDefault("stone", 0);
            default -> 0;
        };
    }
}
