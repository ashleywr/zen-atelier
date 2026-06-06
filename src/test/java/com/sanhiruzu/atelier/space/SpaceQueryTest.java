package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceQueryTest {
    @Test
    void containingRoomNeighborResolvesSolidObjectThroughAdjacentRoomAir() {
        BlockPos workstation = new BlockPos(10, 64, 10);
        BlockPos roomAir = workstation.above();

        String roomId = SpaceQuery.firstContainingRoomNeighbor(
                workstation,
                Set.of(roomAir)::contains,
                pos -> pos.equals(roomAir) ? "room" : null
        );

        assertThat(roomId).isEqualTo("room");
    }

    @Test
    void containingRoomNeighborUsesStableProbeOrder() {
        BlockPos block = new BlockPos(0, 64, 0);
        Set<BlockPos> roomAir = Set.of(block.above(), block.north());

        BlockPos resolved = SpaceQuery.firstContainingRoomNeighbor(
                block,
                roomAir::contains,
                pos -> pos
        );

        assertThat(resolved).isEqualTo(block.above());
    }

    @Test
    void containingRoomNeighborSkipsBlockedOrUnmappedNeighbors() {
        BlockPos block = new BlockPos(0, 64, 0);
        Set<BlockPos> usableAir = Set.of(block.above(), block.south());
        Set<BlockPos> mappedRoomAir = Set.of(block.south());

        BlockPos resolved = SpaceQuery.firstContainingRoomNeighbor(
                block,
                usableAir::contains,
                pos -> mappedRoomAir.contains(pos) ? pos : null
        );

        assertThat(resolved).isEqualTo(block.south());
    }

    @Test
    void containingRoomNeighborIgnoresPositionsOutsideBuildHeight() {
        BlockPos block = new BlockPos(0, 319, 0);
        Set<BlockPos> probed = new HashSet<>();

        BlockPos resolved = SpaceQuery.firstContainingRoomNeighbor(
                block,
                pos -> {
                    probed.add(pos);
                    return true;
                },
                pos -> pos
        );

        assertThat(resolved).isNotEqualTo(block.above());
        assertThat(probed).doesNotContain(block.above());
    }
}
