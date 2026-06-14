package com.sanhiruzu.atelier.space.commit;

import net.minecraft.world.level.ChunkPos;

import java.util.*;

public final class SpaceRegionIndex {
    private final Map<Long, Set<UUID>> chunkToZones = new HashMap<>();

    public void register(CommittedZone zone) {
        for (ChunkPos pos : zone.chunkPositions()) {
            chunkToZones.computeIfAbsent(pos.toLong(), k -> new HashSet<>()).add(zone.uuid());
        }
    }

    public void remove(UUID id, Set<ChunkPos> chunkPositions) {
        for (ChunkPos pos : chunkPositions) {
            Set<UUID> ids = chunkToZones.get(pos.toLong());
            if (ids != null) {
                ids.remove(id);
                if (ids.isEmpty()) chunkToZones.remove(pos.toLong());
            }
        }
    }

    public Set<UUID> getZoneIds(ChunkPos pos) {
        return chunkToZones.getOrDefault(pos.toLong(), Set.of());
    }

    public void clear() {
        chunkToZones.clear();
    }
}
