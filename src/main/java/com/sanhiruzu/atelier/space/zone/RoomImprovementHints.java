package com.sanhiruzu.atelier.space.zone;

import com.sanhiruzu.atelier.data.RequiredFeature;
import com.sanhiruzu.atelier.data.RequiredSurface;
import com.sanhiruzu.atelier.data.RoomProfile;
import com.sanhiruzu.atelier.data.RoomProfileRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RoomImprovementHints {
    private static final int MAX_HINTS = 4;

    private RoomImprovementHints() {
    }

    public static List<String> forRoom(RoomData room) {
        List<String> hints = new ArrayList<>();

        if (room.isDegraded() || room.getEnclosureScore() < 0.55f) {
            hints.add("Close gaps, add a roof, or make the entry clearer so the room feels enclosed.");
        } else if (room.getEnclosureScore() < 0.75f) {
            hints.add("Patch extra openings to improve enclosure.");
        }

        if (room.getVolume() < 24) {
            hints.add("Give the room more floor and head space.");
        }

        RoomProfile profile = room.getZoneTypeId() != null ? RoomProfileRegistry.get(room.getZoneTypeId()) : null;
        if (profile != null) {
            addRequiredFeatureHints(room, profile, hints);
            addRequiredSurfaceHints(room, profile, hints);
            addPenaltyHints(room, profile, hints);
            addQualitySignalHints(room, profile, hints);
        } else if (room.getSignalCounts().isEmpty()) {
            hints.add("Add a clearer anchor such as a bed, workstation, storage, plants, or bookshelves.");
        }

        if (room.getFurnitureCounts().isEmpty() && !hasAnySignal(room, "plant", "water_coverage")) {
            hints.add("Add furniture or useful blocks so the room has a stronger purpose.");
        }

        QualityEvaluator.QualityBreakdown breakdown = room.getQualityBreakdown();
        if (breakdown != null) {
            if (breakdown.themeScore < 0.08f) {
                hints.add("Use more blocks that match the room's purpose.");
            }
            if (breakdown.furnitureScore < 0.06f) {
                hints.add("Add a few more functional or decorative blocks.");
            }
        }

        if (hints.isEmpty()) {
            hints.add("This room is reading clearly. Improve it with more matching detail and careful layout.");
        }

        return hints.stream().distinct().limit(MAX_HINTS).toList();
    }

    private static void addRequiredFeatureHints(RoomData room, RoomProfile profile, List<String> hints) {
        for (RequiredFeature feature : profile.requiredFeatures()) {
            int count = room.getSignalCounts().getOrDefault(feature.signal(), 0);
            if (count < feature.minimum()) {
                int missing = feature.minimum() - count;
                hints.add("Add " + missing + " more " + describeSignal(feature.signal(), missing) + ".");
            }
        }
    }

    private static void addRequiredSurfaceHints(RoomData room, RoomProfile profile, List<String> hints) {
        for (RequiredSurface surface : profile.requiredSurfaces()) {
            int count = countSurface(room, surface);
            if (count < surface.minimum()) {
                int missing = surface.minimum() - count;
                hints.add("Add " + missing + " more " + describeBlockSpec(surface.block(), missing) + " near the room.");
            }
        }
    }

    private static void addPenaltyHints(RoomData room, RoomProfile profile, List<String> hints) {
        for (String penalty : profile.penaltySignals()) {
            if (room.getSignalCounts().getOrDefault(penalty, 0) > 0) {
                hints.add(describePenalty(penalty));
            }
        }
    }

    private static void addQualitySignalHints(RoomData room, RoomProfile profile, List<String> hints) {
        for (String quality : profile.qualitySignals()) {
            if (isQualityAlreadyPresent(room, quality)) {
                continue;
            }
            String hint = describeQualitySignal(quality);
            if (hint != null) {
                hints.add(hint);
            }
        }
    }

    private static int countSurface(RoomData room, RequiredSurface surface) {
        int count = 0;
        String spec = surface.block();
        for (Map.Entry<String, Integer> entry : room.getSurfaceCounts().entrySet()) {
            if (spec.startsWith("#")) {
                TagKey<Block> tag = TagKey.create(Registries.BLOCK, ResourceLocation.parse(spec.substring(1)));
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getKey()));
                if (block != null && block.defaultBlockState().is(tag)) {
                    count += entry.getValue();
                }
            } else if (entry.getKey().equals(spec)) {
                count += entry.getValue();
            }
        }
        return count;
    }

    private static boolean isQualityAlreadyPresent(RoomData room, String quality) {
        String normalized = quality.toLowerCase(Locale.ROOT);
        return room.getSignalCounts().containsKey(normalized)
                || (normalized.contains("storage") && hasAnySignal(room, "storage"))
                || (normalized.contains("water") && hasAnySignal(room, "water_coverage"))
                || (normalized.contains("glass") && hasAnySignal(room, "glass"))
                || (normalized.contains("plant") && hasAnySignal(room, "plant", "frog_plant"))
                || (normalized.contains("tool") && hasAnySignal(room, "tool_blocks"))
                || (normalized.contains("metal") && hasAnySignal(room, "stone_or_metal_materials"));
    }

    private static boolean hasAnySignal(RoomData room, String... signals) {
        for (String signal : signals) {
            if (room.getSignalCounts().getOrDefault(signal, 0) > 0) {
                return true;
            }
        }
        return false;
    }

    private static String describeSignal(String signal, int count) {
        return switch (signal) {
            case "bed" -> plural("bed", count);
            case "bookshelf" -> count == 1 ? "bookshelf" : "bookshelves";
            case "brewing_stand" -> plural("brewing stand", count);
            case "cartography_table" -> plural("cartography table", count);
            case "cauldron" -> plural("cauldron", count);
            case "composter" -> plural("composter", count);
            case "cooking_block" -> plural("cooking block", count);
            case "crafting_table" -> count == 1 ? "crafting table or workbench" : "crafting tables or workbenches";
            case "enchanting_table" -> plural("enchanting table", count);
            case "fletching_table" -> plural("fletching table", count);
            case "frog_plant" -> "frog-friendly plants";
            case "glass" -> count == 1 ? "glass block or pane" : "glass blocks or panes";
            case "loom" -> plural("loom", count);
            case "plant" -> "plants";
            case "smithing_or_repair_block" -> "smithing or repair blocks";
            case "stonecutter" -> plural("stonecutter", count);
            case "storage" -> "storage blocks";
            case "villager_workstation" -> "workstations";
            case "water_coverage" -> "source water touching the room";
            default -> signal.replace('_', ' ');
        };
    }

    private static String describeBlockSpec(String spec, int count) {
        String clean = spec.startsWith("#") ? spec.substring(1) : spec;
        int slash = clean.lastIndexOf('/');
        int colon = clean.lastIndexOf(':');
        int start = Math.max(slash, colon) + 1;
        String label = clean.substring(start).replace('_', ' ');
        return count == 1 || label.endsWith("s") ? label : label + "s";
    }

    private static String plural(String label, int count) {
        return count == 1 ? label : label + "s";
    }

    private static String describePenalty(String penalty) {
        return switch (penalty) {
            case "industrial_blocks" ->
                    "Move machinery, concrete, chains, iron bars, or factory clutter away from this room.";
            case "sleep_blocks" -> "Move beds out so the room keeps a work-focused identity.";
            case "plant_overgrowth" -> "Trim back plants so they do not overwhelm the room's purpose.";
            case "low_glass" -> "Add more glass so the room feels protected and bright.";
            case "low_light", "low_lighting" -> "Add more light.";
            case "low_storage" -> "Add more storage.";
            case "flammable_density" -> "Move flammable blocks away from heat and metalwork.";
            default -> "Reduce " + penalty.replace('_', ' ') + ".";
        };
    }

    private static String describeQualitySignal(String quality) {
        return switch (quality) {
            case "storage", "storage_count", "tool_storage" -> "Add storage that belongs with the room's work.";
            case "tool_blocks" -> "Add more useful work blocks or tools.";
            case "work_surfaces", "clear_workspace" -> "Leave a clear work surface and enough room to move.";
            case "clear_floor_space", "walkable_access" -> "Clear the floor so the room is easy to walk through.";
            case "lighting", "sunlight" -> "Add more light.";
            case "water_access", "water_coverage", "humidity" -> "Add nearby source water or damp features.";
            case "glass_count" -> "Add more glass blocks or panes.";
            case "plant_diversity" -> "Mix in more kinds of plants.";
            case "stone_or_metal_materials" -> "Use more stone or metal materials.";
            case "ventilation" -> "Add openings or details that imply ventilation.";
            case "heat_source" -> "Add controlled heat, such as a furnace or campfire.";
            case "material_order", "labeled_blocks" -> "Organize and label storage.";
            default -> null;
        };
    }
}
