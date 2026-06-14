package com.sanhiruzu.atelier.space.commit;

import javax.annotation.Nullable;
import java.util.*;

public final class ZoneStore {
    private final Map<UUID, CommittedZone> zones = new HashMap<>();
    private final Map<UUID, String> customNames = new HashMap<>();
    private final Map<Long, UUID> hashToUUID = new HashMap<>();

    public void commit(CommittedZone zone) {
        // Preserve existing custom name when updating a zone that already has one
        String existingName = customNames.get(zone.uuid());
        CommittedZone toStore;
        if (existingName != null && zone.customName() == null) {
            toStore = new CommittedZone(
                zone.uuid(), zone.kind(), zone.candidateHash(), zone.memberKeys(),
                zone.chunkPositions(), zone.walkablePositions(),
                zone.minX(), zone.minY(), zone.minZ(),
                zone.maxX(), zone.maxY(), zone.maxZ(),
                existingName);
        } else {
            toStore = zone;
            if (zone.customName() != null) customNames.put(zone.uuid(), zone.customName());
        }
        zones.put(toStore.uuid(), toStore);
        hashToUUID.put(toStore.candidateHash(), toStore.uuid());
    }

    public void remove(UUID id) {
        CommittedZone existing = zones.remove(id);
        if (existing != null) hashToUUID.remove(existing.candidateHash());
        customNames.remove(id);
    }

    @Nullable
    public UUID getByHash(long candidateHash) {
        return hashToUUID.get(candidateHash);
    }

    @Nullable
    public CommittedZone get(UUID id) {
        return zones.get(id);
    }

    public Collection<CommittedZone> all() {
        return Collections.unmodifiableCollection(zones.values());
    }

    public void setCustomName(UUID id, @Nullable String name) {
        if (name == null) {
            customNames.remove(id);
        } else {
            customNames.put(id, name);
        }
    }

    @Nullable
    public String getCustomName(UUID id) {
        return customNames.get(id);
    }

    public void transferCustomName(UUID from, UUID to) {
        String name = customNames.remove(from);
        if (name != null) customNames.put(to, name);
    }

    public boolean contains(UUID id) {
        return zones.containsKey(id);
    }
}
