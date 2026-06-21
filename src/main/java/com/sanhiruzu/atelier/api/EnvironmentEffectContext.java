package com.sanhiruzu.atelier.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record EnvironmentEffectContext(String kind, String id, BlockState blockState) {
    public static EnvironmentEffectContext action(String id) {
        return new EnvironmentEffectContext("action", id, null);
    }

    public static EnvironmentEffectContext block(BlockState state) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return new EnvironmentEffectContext("block", blockId.toString(), state);
    }

    public EnvironmentEffectContext {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("kind must not be blank");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if ("block".equals(kind) && blockState == null) {
            throw new IllegalArgumentException("blockState must not be null for block contexts");
        }
    }

    public boolean matchesAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            throw new IllegalArgumentException("actionId must not be blank");
        }
        return "action".equals(kind) && id.equals(actionId);
    }

    public boolean matchesBlock(Block block) {
        if (block == null) {
            throw new IllegalArgumentException("block must not be null");
        }
        return "block".equals(kind) && blockState != null && blockState.is(block);
    }
}
