package com.sanhiruzu.atelier.synthesis.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class ExtractionCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<ExtractionCauldronBlock> CODEC = simpleCodec(ExtractionCauldronBlock::new);
    public static final IntegerProperty LEVEL = LayeredCauldronBlock.LEVEL;
    public static final EnumProperty<ExtractionCauldronPhase> PHASE =
            EnumProperty.create("phase", ExtractionCauldronPhase.class);

    public ExtractionCauldronBlock(BlockBehaviour.Properties properties) {
        super(properties, CauldronInteraction.WATER);
        registerDefaultState(defaultBlockState()
                .setValue(LEVEL, 3)
                .setValue(PHASE, ExtractionCauldronPhase.READY));
    }

    @Override
    public MapCodec<? extends ExtractionCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(LEVEL) == 3;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return (6.0D + state.getValue(LEVEL) * 3.0D) / 16.0D;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity.isOnFire() && isEntityInsideContent(state, pos, entity)) {
            entity.clearFire();
            if (entity.mayInteract(level, pos)) {
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            }
        }
    }

    @Override
    protected boolean canReceiveStalactiteDrip(Fluid fluid) {
        return fluid == Fluids.WATER;
    }

    @Override
    protected void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
        if (!isFull(state)) {
            BlockState nextState = state.setValue(LEVEL, state.getValue(LEVEL) + 1);
            level.setBlockAndUpdate(pos, nextState);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(nextState));
            level.levelEvent(1047, pos, 0);
        }
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, PHASE);
    }
}
