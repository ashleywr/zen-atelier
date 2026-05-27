package com.sanhiruzu.atelier.ui.network;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.data.RequiredFeature;
import com.sanhiruzu.atelier.data.RoomProfile;
import com.sanhiruzu.atelier.data.RoomProfileRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public record RoomCatalogSyncPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<RoomCatalogSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "room_catalog_sync"));

    public static final StreamCodec<FriendlyByteBuf, RoomCatalogSyncPayload> CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeVarInt(packet.entries.size());
                        for (Entry entry : packet.entries) {
                            buf.writeUtf(entry.profileId());
                            buf.writeUtf(entry.displayName());
                            buf.writeUtf(entry.iconItemId());
                            buf.writeVarInt(entry.hints().size());
                            for (String hint : entry.hints()) {
                                buf.writeUtf(hint);
                            }
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<Entry> entries = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            String profileId = buf.readUtf();
                            String displayName = buf.readUtf();
                            String iconItemId = buf.readUtf();
                            int hintCount = buf.readVarInt();
                            List<String> hints = new ArrayList<>(hintCount);
                            for (int h = 0; h < hintCount; h++) {
                                hints.add(buf.readUtf());
                            }
                            entries.add(new Entry(profileId, displayName, iconItemId, hints));
                        }
                        return new RoomCatalogSyncPayload(entries);
                    }
            );

    public static RoomCatalogSyncPayload current() {
        return fromProfiles(RoomProfileRegistry.all());
    }

    public static RoomCatalogSyncPayload fromProfiles(Collection<RoomProfile> profiles) {
        List<Entry> entries = profiles.stream()
                .sorted(Comparator.comparing(profile -> profile.id().toString()))
                .map(RoomCatalogSyncPayload::fromProfile)
                .toList();
        return new RoomCatalogSyncPayload(entries);
    }

    private static Entry fromProfile(RoomProfile profile) {
        return new Entry(
                profile.id().toString(),
                profile.displayName(),
                iconFor(profile),
                hintsFor(profile)
        );
    }

    private static String iconFor(RoomProfile profile) {
        String explicit = profile.icon();
        if (isValidItemId(explicit)) {
            return explicit;
        }

        for (String anchor : profile.anchors()) {
            if (anchor.startsWith("#")) {
                continue;
            }
            if (isValidItemId(anchor)) {
                return anchor;
            }
        }

        for (RequiredFeature feature : profile.requiredFeatures()) {
            String icon = iconForSignal(feature.signal());
            if (icon != null) {
                return icon;
            }
        }

        return "minecraft:paper";
    }

    private static boolean isValidItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        try {
            ResourceLocation.parse(itemId);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String iconForSignal(String signal) {
        return switch (signal) {
            case "bed" -> "minecraft:red_bed";
            case "bookshelf" -> "minecraft:bookshelf";
            case "brewing_stand" -> "minecraft:brewing_stand";
            case "cartography_table" -> "minecraft:cartography_table";
            case "cauldron" -> "minecraft:cauldron";
            case "composter" -> "minecraft:composter";
            case "cooking_block" -> "minecraft:furnace";
            case "crafting_table" -> "minecraft:crafting_table";
            case "enchanting_table" -> "minecraft:enchanting_table";
            case "fletching_table" -> "minecraft:fletching_table";
            case "frog_plant" -> "minecraft:lily_pad";
            case "glass" -> "minecraft:glass";
            case "loom" -> "minecraft:loom";
            case "plant" -> "minecraft:poppy";
            case "smithing_or_repair_block" -> "minecraft:anvil";
            case "stonecutter" -> "minecraft:stonecutter";
            case "storage" -> "minecraft:chest";
            case "villager_workstation" -> "minecraft:lectern";
            case "water_coverage" -> "minecraft:water_bucket";
            case "minecolonies_town_hall" -> "minecolonies:blockhuttownhall";
            case "minecolonies_residence" -> "minecolonies:blockhutcitizen";
            case "minecolonies_warehouse" -> "minecolonies:blockhutwarehouse";
            case "minecolonies_kitchen" -> "minecolonies:blockhutcook";
            case "minecolonies_library" -> "minecolonies:blockhutlibrary";
            case "minecolonies_smithy" -> "minecolonies:blockhutblacksmith";
            case "minecolonies_masonry" -> "minecolonies:blockhutstonemason";
            case "minecolonies_fletchery" -> "minecolonies:blockhutfletcher";
            case "minecolonies_garden" -> "minecolonies:blockhutflorist";
            case "minecolonies_farm" -> "minecolonies:blockhutfarmer";
            case "minecolonies_arcane" -> "minecolonies:blockhutenchanter";
            case "minecolonies_workshop" -> "minecolonies:blockhutbuilder";
            case "minecolonies_guard" -> "minecolonies:blockhutguardtower";
            case "minecolonies_civic" -> "minecolonies:blockhuthospital";
            default -> null;
        };
    }

    private static List<String> hintsFor(RoomProfile profile) {
        List<String> hints = new ArrayList<>();
        for (RequiredFeature feature : profile.requiredFeatures()) {
            hints.add("Look for " + describeSignal(feature.signal(), feature.minimum()) + ".");
            if (hints.size() >= 3) {
                return hints;
            }
        }
        for (String anchor : profile.anchors()) {
            hints.add("Try " + describeBlockSpec(anchor) + ".");
            if (hints.size() >= 3) {
                return hints;
            }
        }
        if (hints.isEmpty()) {
            hints.add("Build a clearer room with matching purpose blocks.");
        }
        return hints;
    }

    private static String describeSignal(String signal, int count) {
        String label = switch (signal) {
            case "bed" -> count == 1 ? "a bed" : "beds";
            case "bookshelf" -> count == 1 ? "a bookshelf" : "bookshelves";
            case "brewing_stand" -> count == 1 ? "a brewing stand" : "brewing stands";
            case "cartography_table" -> count == 1 ? "a cartography table" : "cartography tables";
            case "cauldron" -> count == 1 ? "a cauldron" : "cauldrons";
            case "composter" -> count == 1 ? "a composter" : "composters";
            case "cooking_block" -> count == 1 ? "a cooking block" : "cooking blocks";
            case "crafting_table" -> count == 1 ? "a crafting table" : "crafting tables";
            case "enchanting_table" -> count == 1 ? "an enchanting table" : "enchanting tables";
            case "fletching_table" -> count == 1 ? "a fletching table" : "fletching tables";
            case "frog_plant" -> "frog-friendly plants";
            case "glass" -> "glass";
            case "loom" -> count == 1 ? "a loom" : "looms";
            case "plant" -> "plants";
            case "smithing_or_repair_block" -> "smithing or repair blocks";
            case "stonecutter" -> count == 1 ? "a stonecutter" : "stonecutters";
            case "storage" -> "storage blocks";
            case "villager_workstation" -> "workstations";
            case "water_coverage" -> "nearby source water";
            case "minecolonies_town_hall" -> "a MineColonies town hall";
            case "minecolonies_residence" -> "a MineColonies residence or tavern";
            case "minecolonies_warehouse" -> "MineColonies storage";
            case "minecolonies_kitchen" -> "a MineColonies kitchen, cook, or baker hut";
            case "minecolonies_library" -> "a MineColonies library, school, or university";
            case "minecolonies_smithy" -> "a MineColonies smithing or smelting hut";
            case "minecolonies_masonry" -> "a MineColonies stonemason hut";
            case "minecolonies_fletchery" -> "a MineColonies fletcher or archery hut";
            case "minecolonies_garden" -> "a MineColonies garden hut";
            case "minecolonies_farm" -> "a MineColonies farm or animal hut";
            case "minecolonies_arcane" -> "a MineColonies arcane hut";
            case "minecolonies_workshop" -> "a MineColonies work hut";
            case "minecolonies_guard" -> "a MineColonies guard building";
            case "minecolonies_civic" -> "a MineColonies civic building";
            default -> signal.replace('_', ' ');
        };
        if ("glass".equals(signal) || "water_coverage".equals(signal)) {
            return label;
        }
        return count > 1 && !label.endsWith("s") ? count + " " + label : label;
    }

    private static String describeBlockSpec(String spec) {
        String clean = spec.startsWith("#") ? spec.substring(1) : spec;
        int slash = clean.lastIndexOf('/');
        int colon = clean.lastIndexOf(':');
        int start = Math.max(slash, colon) + 1;
        return clean.substring(start).replace('_', ' ');
    }

    @Override
    public Type<RoomCatalogSyncPayload> type() {
        return TYPE;
    }

    public record Entry(String profileId, String displayName, String iconItemId, List<String> hints) {
    }
}
