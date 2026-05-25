package com.sanhiruzu.atelier.zone.naming;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record RoomEpithetCatalog(List<RoomEpithet> roomEpithets) {
    public static final Codec<RoomEpithetCatalog> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RoomEpithet.CODEC.listOf().fieldOf("room_epithets").forGetter(RoomEpithetCatalog::roomEpithets)
    ).apply(instance, RoomEpithetCatalog::new));
}
