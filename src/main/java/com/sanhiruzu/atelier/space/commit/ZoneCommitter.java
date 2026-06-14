package com.sanhiruzu.atelier.space.commit;

import com.sanhiruzu.atelier.space.ChunkClassificationData;
import com.sanhiruzu.atelier.space.ClassificationState;
import com.sanhiruzu.atelier.space.analyze.CandidateDecision;
import com.sanhiruzu.atelier.space.analyze.ZoneCandidate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class ZoneCommitter {

    private final ZoneStore store;
    private final SpaceRegionIndex index;
    private final BiConsumer<ChunkPos, ChunkClassificationData> syncFn;

    public ZoneCommitter(ZoneStore store, SpaceRegionIndex index,
                         BiConsumer<ChunkPos, ChunkClassificationData> syncFn) {
        this.store = store;
        this.index = index;
        this.syncFn = syncFn;
    }

    /**
     * Commits an accepted ZoneCandidate to the store and marks INSIDE cells in chunkData.
     *
     * @return the UUID of the committed zone, or null if decision is not an ACCEPT variant
     */
    @Nullable
    public UUID commitAccepted(ZoneCandidate candidate,
                               CandidateDecision decision,
                               @Nullable UUID existingId,
                               long[] walkablePositions,
                               Map<ChunkPos, ChunkClassificationData> chunkData) {
        if (decision != CandidateDecision.ACCEPT_INDOOR
                && decision != CandidateDecision.ACCEPT_SHELTERED
                && decision != CandidateDecision.ACCEPT_OUTDOOR_FUNCTIONAL) {
            return null;
        }

        // Determine UUID: reuse existingId only when candidateHash matches
        UUID uuid;
        @Nullable String customName = null;
        if (existingId != null) {
            CommittedZone existing = store.get(existingId);
            if (existing != null && existing.candidateHash() == candidate.candidateHash()) {
                uuid = existingId;
                customName = existing.customName();
            } else {
                uuid = UUID.randomUUID();
            }
        } else {
            uuid = UUID.randomUUID();
        }

        CommittedZone zone = new CommittedZone(
                uuid, decision, candidate.candidateHash(),
                candidate.memberKeys(), candidate.chunkPositions(),
                walkablePositions,
                candidate.minX(), candidate.minY(), candidate.minZ(),
                candidate.maxX(), candidate.maxY(), candidate.maxZ(),
                customName
        );

        store.commit(zone);
        index.register(zone);

        // Mark INSIDE in each chunk's classification data
        for (long packed : walkablePositions) {
            BlockPos pos = BlockPos.of(packed);
            int worldX = pos.getX();
            int worldY = pos.getY();
            int worldZ = pos.getZ();

            int chunkX = Math.floorDiv(worldX, 16);
            int chunkZ = Math.floorDiv(worldZ, 16);
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

            ChunkClassificationData data = chunkData.get(chunkPos);
            if (data != null) {
                int localX = worldX & 15;
                int localZ = worldZ & 15;
                data.setBlockState(localX, worldY, localZ, ClassificationState.INSIDE);
            }
        }

        // Sync all chunks that are in both candidate.chunkPositions and chunkData
        for (ChunkPos pos : candidate.chunkPositions()) {
            ChunkClassificationData data = chunkData.get(pos);
            if (data != null) {
                syncFn.accept(pos, data);
            }
        }

        return uuid;
    }

    /**
     * Dissolves a committed zone: clears INSIDE cells back to SOLID, removes from store and index.
     */
    public void dissolve(UUID id, Set<ChunkPos> chunkPositions,
                         Map<ChunkPos, ChunkClassificationData> chunkData) {
        CommittedZone zone = store.get(id);
        if (zone == null) {
            return;
        }

        // Clear INSIDE -> SOLID for each walkable position
        for (long packed : zone.walkablePositions()) {
            BlockPos pos = BlockPos.of(packed);
            int worldX = pos.getX();
            int worldY = pos.getY();
            int worldZ = pos.getZ();

            int chunkX = Math.floorDiv(worldX, 16);
            int chunkZ = Math.floorDiv(worldZ, 16);
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

            ChunkClassificationData data = chunkData.get(chunkPos);
            if (data != null) {
                int localX = worldX & 15;
                int localZ = worldZ & 15;
                data.setBlockState(localX, worldY, localZ, ClassificationState.SOLID);
            }
        }

        store.remove(id);
        index.remove(id, chunkPositions);

        // Sync all affected chunks
        for (ChunkPos pos : chunkPositions) {
            ChunkClassificationData data = chunkData.get(pos);
            if (data != null) {
                syncFn.accept(pos, data);
            }
        }
    }
}
