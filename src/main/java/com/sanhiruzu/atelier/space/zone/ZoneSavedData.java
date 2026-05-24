package com.sanhiruzu.atelier.space.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Persists zone data (UUID → interior blocks + openingArea + customName) to disk so
 * zones survive world reloads. Stored in the dimension's data storage under "atelier_zones".
 *
 * <p>Called from Zone on create/dissolve/expand/shrink/merge, and from ZoneRegistry on
 * custom-name changes. Uses SavedData's dirty-flag mechanism so writes only happen on
 * the normal world-save cycle, not on every block event.</p>
 */
public class ZoneSavedData extends SavedData {
    private static final String DATA_NAME = "atelier_zones";

    public record ZoneRecord(Set<BlockPos> blocks, int openingArea, @Nullable String customName) {
    }

    private final Map<UUID, ZoneRecord> zones = new HashMap<>();

    private static ZoneSavedData create() {
        return new ZoneSavedData();
    }

    private static ZoneSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ZoneSavedData data = new ZoneSavedData();
        ListTag zoneList = tag.getList("zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < zoneList.size(); i++) {
            CompoundTag zoneTag = zoneList.getCompound(i);
            UUID id = zoneTag.getUUID("id");
            int openingArea = zoneTag.getInt("openingArea");
            String customName = zoneTag.contains("customName") ? zoneTag.getString("customName") : null;

            long[] blockLongs = zoneTag.getLongArray("blocks");
            Set<BlockPos> blocks = new HashSet<>(blockLongs.length);
            for (long l : blockLongs) {
                blocks.add(BlockPos.of(l));
            }
            data.zones.put(id, new ZoneRecord(blocks, openingArea, customName));
        }
        return data;
    }

    public static ZoneSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ZoneSavedData::create, ZoneSavedData::load, null),
                DATA_NAME
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag zoneList = new ListTag();
        for (Map.Entry<UUID, ZoneRecord> entry : zones.entrySet()) {
            CompoundTag zoneTag = new CompoundTag();
            zoneTag.putUUID("id", entry.getKey());
            zoneTag.putInt("openingArea", entry.getValue().openingArea());
            if (entry.getValue().customName() != null) {
                zoneTag.putString("customName", entry.getValue().customName());
            }
            long[] blockLongs = entry.getValue().blocks().stream()
                    .mapToLong(BlockPos::asLong)
                    .toArray();
            zoneTag.putLongArray("blocks", blockLongs);
            zoneList.add(zoneTag);
        }
        tag.put("zones", zoneList);
        return tag;
    }

    public void saveZone(UUID id, Set<BlockPos> blocks, int openingArea, @Nullable String customName) {
        zones.put(id, new ZoneRecord(new HashSet<>(blocks), openingArea, customName));
        setDirty();
    }

    public void removeZone(UUID id) {
        if (zones.remove(id) != null) setDirty();
    }

    public void setCustomName(UUID id, @Nullable String name) {
        ZoneRecord existing = zones.get(id);
        if (existing != null) {
            zones.put(id, new ZoneRecord(existing.blocks(), existing.openingArea(), name));
            setDirty();
        }
    }

    public Map<UUID, ZoneRecord> getZones() {
        return Collections.unmodifiableMap(zones);
    }
}
