package com.sanhiruzu.atelier.space.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

public final class ZoneBlockPredicates {
    private ZoneBlockPredicates() {
    }

    public static boolean isEntryBlock(BlockState state) {
        return state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.STAIRS)
                || state.is(BlockTags.SLABS);
    }

    public static boolean isConnectorBlock(BlockState state) {
        return state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.STAIRS)
                || state.is(BlockTags.SLABS)
                || state.is(BlockTags.CLIMBABLE);
    }

    public static boolean hasNearbyMatchingBlock(BlockPos first,
                                                 BlockPos second,
                                                 Predicate<BlockPos> predicate) {
        for (BlockPos origin : List.of(first, second)) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (predicate.test(origin.offset(dx, dy, dz))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
