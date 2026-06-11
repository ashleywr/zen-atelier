package com.sanhiruzu.atelier.synthesis.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;

public class StructureCharmItem extends Item {
    private static final int USE_COOLDOWN_TICKS = 40;

    public StructureCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (level instanceof ServerLevel serverLevel) {
            ResourceLocation structureId = findStructureId(serverLevel, player.blockPosition());
            if (structureId != null) {
                player.displayClientMessage(Component.translatable(
                        "message.zen_atelier.structure_charm.found",
                        Component.literal(structureId.getPath()).withStyle(ChatFormatting.AQUA),
                        Component.literal(structureId.getNamespace()).withStyle(ChatFormatting.DARK_AQUA)
                ), false);
            } else {
                player.displayClientMessage(Component.translatable("message.zen_atelier.structure_charm.none")
                        .withStyle(ChatFormatting.GRAY), false);
            }
        }

        player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.zen_atelier.structure_charm").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.zen_atelier.structure_charm_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static ResourceLocation findStructureId(ServerLevel level, BlockPos pos) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, holder -> true);
        if (start != null && start.isValid()) {
            return registry.getKey(start.getStructure());
        }

        StructureStart closestStart = null;
        double closestDistance = Double.MAX_VALUE;
        for (StructureStart candidate : level.structureManager().startsForStructure(new ChunkPos(pos), structure -> true)) {
            if (candidate == null || !candidate.isValid()) {
                continue;
            }

            BoundingBox bounds = candidate.getBoundingBox();
            double centerX = (bounds.minX() + bounds.maxX()) / 2.0;
            double centerZ = (bounds.minZ() + bounds.maxZ()) / 2.0;
            double dx = pos.getX() + 0.5 - centerX;
            double dz = pos.getZ() + 0.5 - centerZ;
            double distance = dx * dx + dz * dz;
            if (distance < closestDistance) {
                closestDistance = distance;
                closestStart = candidate;
            }
        }

        return closestStart != null ? registry.getKey(closestStart.getStructure()) : null;
    }
}
