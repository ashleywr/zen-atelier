package com.sanhiruzu.atelier.space;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Result of an off-thread chunk flood-fill classification.
 * Produced by {@link ChunkClassifier#classifyAsync(ChunkSnapshot)}.
 * Can be applied to chunk data via
 * {@link ChunkClassificationData#copyFromOutput(ClassificationOutput)}.
 */
public record ClassificationOutput(
        long[] bitfield,
        List<ClassifiedRegion> regions,
        Map<UUID, Set<BlockPos>> regionBlocks
) {}
