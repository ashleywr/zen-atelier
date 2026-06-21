package com.sanhiruzu.atelier.environment;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

@FunctionalInterface
public interface EnvironmentFacetProvider {
    Set<String> facetsFor(BlockState state);
}
