package com.sanhiruzu.atelier.synthesis.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AlchemyCauldronSavedData extends SavedData {
    private static final String DATA_NAME = "atelier_alchemy_cauldrons";
    private static final String SOLVENT_CAULDRONS_KEY = "solvent_cauldrons";

    private final Set<BlockPos> solventCauldrons = new HashSet<>();

    private static AlchemyCauldronSavedData create() {
        return new AlchemyCauldronSavedData();
    }

    static AlchemyCauldronSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        AlchemyCauldronSavedData data = new AlchemyCauldronSavedData();
        for (long pos : tag.getLongArray(SOLVENT_CAULDRONS_KEY)) {
            data.solventCauldrons.add(BlockPos.of(pos));
        }
        return data;
    }

    public static AlchemyCauldronSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AlchemyCauldronSavedData::create, AlchemyCauldronSavedData::load, null),
                DATA_NAME
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLongArray(SOLVENT_CAULDRONS_KEY, solventCauldrons.stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }

    public boolean hasWeakSolvent(BlockPos pos) {
        return solventCauldrons.contains(pos);
    }

    public void setWeakSolvent(BlockPos pos) {
        if (solventCauldrons.add(pos.immutable())) {
            setDirty();
        }
    }

    public void clear(BlockPos pos) {
        if (solventCauldrons.remove(pos)) {
            setDirty();
        }
    }

    public Set<BlockPos> solventCauldrons() {
        return Collections.unmodifiableSet(solventCauldrons);
    }
}
