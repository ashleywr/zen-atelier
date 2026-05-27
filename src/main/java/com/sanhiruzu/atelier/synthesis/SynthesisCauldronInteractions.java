package com.sanhiruzu.atelier.synthesis;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

public final class SynthesisCauldronInteractions {
    private SynthesisCauldronInteractions() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ItemStack held = player.getItemInHand(event.getHand());

        if (tryUpgradeCauldron(level, pos, state, player, event.getHand(), held)) {
            complete(event, level);
            return;
        }

        if (!state.is(ZenAtelier.SYNTHESIS_CAULDRON.get())) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof SynthesisCauldronBlockEntity cauldron) {
            if (handleSynthesisCauldron(level, pos, player, held, cauldron)) {
                complete(event, level);
            }
        }
    }

    private static boolean tryUpgradeCauldron(Level level, BlockPos pos, BlockState state, Player player,
                                              InteractionHand hand, ItemStack held) {
        if (!state.is(Blocks.WATER_CAULDRON) || !isUpgradeCatalyst(held)) {
            return false;
        }

        int waterLevel = state.getValue(LayeredCauldronBlock.LEVEL);
        if (waterLevel < 3) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.zen_atelier.cauldron_needs_full_water"), true);
            }
            return true;
        }

        if (!level.isClientSide) {
            level.setBlock(pos, ZenAtelier.SYNTHESIS_CAULDRON.get().defaultBlockState(), 3);
            if (!player.getAbilities().instabuild) {
                player.getItemInHand(hand).shrink(1);
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
            player.displayClientMessage(Component.translatable("message.zen_atelier.synthesis_cauldron_created"), true);
        }
        return true;
    }

    private static boolean handleSynthesisCauldron(Level level, BlockPos pos, Player player, ItemStack held,
                                                   SynthesisCauldronBlockEntity cauldron) {
        if (held.getItem() instanceof AlchemyWandItem wand) {
            if (!level.isClientSide) {
                stir(level, pos, player, cauldron, wand);
            }
            return true;
        }

        if (held.isEmpty() && player.isShiftKeyDown()) {
            if (!level.isClientSide && cauldron.clearToPlayer(player)) {
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f, 1.6f);
                player.displayClientMessage(Component.translatable("message.zen_atelier.synthesis_cauldron_cleared"), true);
            }
            return true;
        }

        if (!held.isEmpty()) {
            if (!level.isClientSide) {
                if (cauldron.addIngredient(held)) {
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.7f, 1.3f);
                    player.displayClientMessage(Component.translatable("message.zen_atelier.synthesis_ingredient_added", cauldron.ingredientCount()), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.zen_atelier.synthesis_cauldron_full"), true);
                }
            }
            return true;
        }

        return false;
    }

    private static void stir(Level level, BlockPos pos, Player player, SynthesisCauldronBlockEntity cauldron,
                             AlchemyWandItem wand) {
        SynthesisRoomContext context = SynthesisRoomContext.at(level, pos);
        if (!context.inAtelier()) {
            player.displayClientMessage(Component.translatable("message.zen_atelier.synthesis_requires_atelier"), true);
            return;
        }

        Optional<SynthesisRecipe> recipe = SynthesisRecipe.find(cauldron.ingredients(), wand.tier(), context.quality());
        if (recipe.isEmpty()) {
            Optional<SynthesisRecipe> blocked = SynthesisRecipe.firstBlockedByTierOrQuality(cauldron.ingredients());
            if (blocked.isPresent()) {
                SynthesisRecipe blockedRecipe = blocked.get();
                if (!wand.tier().atLeast(blockedRecipe.minimumTier())) {
                    player.displayClientMessage(Component.translatable("message.zen_atelier.synthesis_requires_wand", blockedRecipe.minimumTier().name().toLowerCase()), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.zen_atelier.synthesis_requires_quality", blockedRecipe.minimumAtelierQuality()), true);
                }
            } else {
                player.displayClientMessage(Component.translatable("message.zen_atelier.synthesis_no_recipe"), true);
            }
            return;
        }

        ItemStack output = recipe.get().assemble(wand.tier(), context.quality());
        if (!player.getInventory().add(output.copy())) {
            player.drop(output.copy(), false);
        }
        cauldron.clear();
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.9f, 1.1f);
        player.displayClientMessage(Component.translatable("message.zen_atelier.synthesis_complete", output.getHoverName()), true);
    }

    private static boolean isUpgradeCatalyst(ItemStack stack) {
        if (stack.is(Items.AMETHYST_SHARD) || stack.is(Items.QUARTZ)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.toString().equals("ae2:certus_quartz_crystal") || id.toString().equals("ae2:certus_quartz_dust");
    }

    private static void complete(PlayerInteractEvent.RightClickBlock event, Level level) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
