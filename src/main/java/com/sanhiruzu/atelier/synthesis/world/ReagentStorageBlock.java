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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReagentStorageBlock extends Block {
    public static final MapCodec<ReagentStorageBlock> CODEC = simpleCodec(ReagentStorageBlock::new);

    /** Transient per-player record of the most recent click on any cabinet, for double-click detection. */
    private static final Map<UUID, ReagentDumpLogic.Click> LAST_CLICK = new ConcurrentHashMap<>();

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

            if (tryDumpAll((ServerLevel) level, pos, player)) {
                return ItemInteractionResult.CONSUME;
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

        if (tryDumpAll((ServerLevel) level, pos, player)) {
            return ItemInteractionResult.CONSUME;
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
        if (tryDumpAll((ServerLevel) level, pos, player)) {
            return InteractionResult.CONSUME;
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

    /**
     * Records this click and reports whether it completes a double-click. On a double-click the
     * player's record is cleared and all reagents in their inventory are dumped into the cabinet.
     */
    private static boolean tryDumpAll(ServerLevel level, BlockPos pos, Player player) {
        UUID id = player.getUUID();
        long now = level.getGameTime();
        if (ReagentDumpLogic.isDoubleClick(LAST_CLICK.get(id), pos, now)) {
            LAST_CLICK.remove(id);
            dumpAll(level, pos, player);
            return true;
        }
        LAST_CLICK.put(id, new ReagentDumpLogic.Click(pos.immutable(), now));
        return false;
    }

    private static void dumpAll(ServerLevel level, BlockPos pos, Player player) {
        ReagentCabinetSavedData data = ReagentCabinetSavedData.get(level);
        ReagentContainer container = data.getContainer(pos);
        List<ReagentStack> deposited = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            ReagentStack reagent = ReagentItem.getReagent(stack);
            if (reagent == null) {
                continue;
            }
            container.insert(reagent);
            deposited.add(reagent);
            if (!player.getAbilities().instabuild) {
                stack.setCount(0);
            }
        }
        if (deposited.isEmpty()) {
            return;
        }
        data.putContainer(pos, container);
        ReagentDumpLogic.DumpSummary summary = ReagentDumpLogic.summarize(deposited);
        player.displayClientMessage(Component.translatable(
                "message.zen_atelier.reagent_storage.dumped",
                summary.stacks(),
                summary.units()
        ), true);
    }

    private static String reagentPath(String id) {
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }
}
