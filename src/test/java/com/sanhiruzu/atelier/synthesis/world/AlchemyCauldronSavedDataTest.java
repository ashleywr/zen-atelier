package com.sanhiruzu.atelier.synthesis.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlchemyCauldronSavedDataTest {
    @Test
    void roundTripsWeakSolventCauldronsByPosition() {
        AlchemyCauldronSavedData data = new AlchemyCauldronSavedData();
        BlockPos first = new BlockPos(1, 64, 1);
        BlockPos second = new BlockPos(-4, 70, 12);

        data.setWeakSolvent(first);
        data.setWeakSolvent(second);
        CompoundTag saved = data.save(new CompoundTag(), null);
        AlchemyCauldronSavedData loaded = AlchemyCauldronSavedData.load(saved, null);

        assertThat(loaded.hasWeakSolvent(first)).isTrue();
        assertThat(loaded.hasWeakSolvent(second)).isTrue();
        assertThat(loaded.hasWeakSolvent(new BlockPos(0, 64, 0))).isFalse();
    }

    @Test
    void clearRemovesWeakSolventPosition() {
        AlchemyCauldronSavedData data = new AlchemyCauldronSavedData();
        BlockPos pos = new BlockPos(2, 65, 2);

        data.setWeakSolvent(pos);
        data.clear(pos);

        assertThat(data.hasWeakSolvent(pos)).isFalse();
    }
}
