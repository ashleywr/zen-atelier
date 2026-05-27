package com.sanhiruzu.atelier.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record RoomProfile(
        ResourceLocation id,
        String displayName,
        ResourceLocation zoneDefinition,
        String icon,
        List<String> requiredMods,
        List<String> anchors,
        List<RequiredFeature> requiredFeatures,
        List<String> qualitySignals,
        List<String> penaltySignals,
        List<RequiredSurface> requiredSurfaces
) {
    public static final Codec<RoomProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(RoomProfile::id),
            Codec.STRING.fieldOf("display_name").forGetter(RoomProfile::displayName),
            ResourceLocation.CODEC.fieldOf("zone_definition").forGetter(RoomProfile::zoneDefinition),
            Codec.STRING.optionalFieldOf("icon", "").forGetter(RoomProfile::icon),
            Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(RoomProfile::requiredMods),
            Codec.STRING.listOf().optionalFieldOf("anchors", List.of()).forGetter(RoomProfile::anchors),
            RequiredFeature.CODEC.listOf().fieldOf("required_features").forGetter(RoomProfile::requiredFeatures),
            Codec.STRING.listOf().optionalFieldOf("quality_signals", List.of()).forGetter(RoomProfile::qualitySignals),
            Codec.STRING.listOf().optionalFieldOf("penalty_signals", List.of()).forGetter(RoomProfile::penaltySignals),
            RequiredSurface.CODEC.listOf().optionalFieldOf("required_surfaces", new ArrayList<>()).forGetter(RoomProfile::requiredSurfaces)
    ).apply(instance, RoomProfile::new));

    public RoomProfile(ResourceLocation id,
                       String displayName,
                       ResourceLocation zoneDefinition,
                       List<String> anchors,
                       List<RequiredFeature> requiredFeatures,
                       List<String> qualitySignals,
                       List<String> penaltySignals,
                       List<RequiredSurface> requiredSurfaces) {
        this(id, displayName, zoneDefinition, "", List.of(), anchors, requiredFeatures, qualitySignals, penaltySignals, requiredSurfaces);
    }

    public RoomProfile(ResourceLocation id,
                       String displayName,
                       ResourceLocation zoneDefinition,
                       String icon,
                       List<String> anchors,
                       List<RequiredFeature> requiredFeatures,
                       List<String> qualitySignals,
                       List<String> penaltySignals,
                       List<RequiredSurface> requiredSurfaces) {
        this(id, displayName, zoneDefinition, icon, List.of(), anchors, requiredFeatures, qualitySignals, penaltySignals, requiredSurfaces);
    }
}
