package com.sanhiruzu.atelier.synthesis.world;

import com.mojang.serialization.MapCodec;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.gathering.GatheringBasketItem;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainerSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class ReagentStorageBlock extends Block {
    public static final MapCodec<ReagentStorageBlock> CODEC = simpleCodec(ReagentStorageBlock::new);

    public ReagentStorageBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ReagentStorageBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        ReagentStack reagent = ReagentItem.getReagent(stack);
        if (reagent == null) {
            if (!GatheringBasketItem.isBasket(stack)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            ReagentContainer basket = GatheringBasketItem.getContents(stack);
            List<ReagentStack> basketEntries = basket.entries();
            if (basketEntries.isEmpty()) {
                return ItemInteractionResult.CONSUME;
            }

            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }

            ReagentCabinetSavedData data = ReagentCabinetSavedData.get((ServerLevel) level);
            ReagentContainer container = data.getContainer(pos);
            for (ReagentStack entry : basketEntries) {
                container.insert(entry);
            }
            data.putContainer(pos, container);
            if (!player.getAbilities().instabuild) {
                GatheringBasketItem.setContents(stack, new ReagentContainer());
            }

            int stacks = basketEntries.size();
            int units = basketEntries.stream().mapToInt(ReagentStack::amount).sum();
            player.displayClientMessage(Component.translatable(
                    "message.zen_atelier.reagent_storage.basket_deposited",
                    stacks,
                    units
            ), true);
            return ItemInteractionResult.CONSUME;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        ReagentCabinetSavedData data = ReagentCabinetSavedData.get((ServerLevel) level);
        ReagentContainer container = data.getContainer(pos);
        container.insert(reagent);
        data.putContainer(pos, container);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.translatable(
                "message.zen_atelier.reagent_storage.deposited",
                reagent.amount(),
                Component.translatable("zen_atelier.reagent." + reagentPath(reagent.reagentId()))
        ), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ReagentContainerSnapshot snapshot = ReagentCabinetSavedData.get((ServerLevel) level).getSnapshot(pos);
        int units = snapshot.entries().stream().mapToInt(ReagentStack::amount).sum();
        player.displayClientMessage(Component.translatable(
                "message.zen_atelier.reagent_storage.summary",
                snapshot.entries().size(),
                units
        ), true);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            ReagentCabinetSavedData data = ReagentCabinetSavedData.get(serverLevel);
            for (ReagentStack reagent : data.getSnapshot(pos).entries()) {
                Containers.dropItemStack(serverLevel, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, ReagentItem.createStack(reagent));
            }
            data.clear(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static String reagentPath(String id) {
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }
}
