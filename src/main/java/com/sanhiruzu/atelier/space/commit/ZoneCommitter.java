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

    private static final int CHUNK_SIZE = 16;
    private static final int CHUNK_COORD_MASK = CHUNK_SIZE - 1;

    private final ZoneStore store;
    private final SpaceRegionIndex index;
    private final BiConsumer<ChunkPos, ChunkClassificationData> syncFn;

    public ZoneCommitter(ZoneStore store, SpaceRegionIndex index,
                         BiConsumer<ChunkPos, ChunkClassificationData> syncFn) {
        this.store = store;
        this.index = index;
        this.syncFn = syncFn;
    }

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

        applyStateToChunks(walkablePositions, ClassificationState.INSIDE, chunkData);

        // Sync all chunks that are in both candidate.chunkPositions and chunkData
        for (ChunkPos pos : candidate.chunkPositions()) {
            ChunkClassificationData data = chunkData.get(pos);
            if (data != null) {
                syncFn.accept(pos, data);
            }
        }

        return uuid;
    }

    public void dissolve(UUID id, Set<ChunkPos> chunkPositions,
                         Map<ChunkPos, ChunkClassificationData> chunkData) {
        CommittedZone zone = store.get(id);
        if (zone == null) {
            return;
        }

        applyStateToChunks(zone.walkablePositions(), ClassificationState.SOLID, chunkData);

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

    private void applyStateToChunks(long[] positions, ClassificationState state,
                                    Map<ChunkPos, ChunkClassificationData> chunkData) {
        for (long packed : positions) {
            BlockPos pos = BlockPos.of(packed);
            int worldX = pos.getX();
            int worldY = pos.getY();
            int worldZ = pos.getZ();

            int chunkX = Math.floorDiv(worldX, CHUNK_SIZE);
            int chunkZ = Math.floorDiv(worldZ, CHUNK_SIZE);
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

            ChunkClassificationData data = chunkData.get(chunkPos);
            if (data != null) {
                int localX = worldX & CHUNK_COORD_MASK;
                int localZ = worldZ & CHUNK_COORD_MASK;
                data.setBlockState(localX, worldY, localZ, state);
            }
        }
    }
}
