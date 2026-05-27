package com.sanhiruzu.atelier.synthesis;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class SynthesisCauldronBlockEntity extends BlockEntity {
    private static final int MAX_INGREDIENTS = 3;
    private final List<ItemStack> ingredients = new ArrayList<>();

    public SynthesisCauldronBlockEntity(BlockPos pos, BlockState blockState) {
        super(ZenAtelier.SYNTHESIS_CAULDRON_BLOCK_ENTITY.get(), pos, blockState);
    }

    public boolean addIngredient(ItemStack source) {
        if (source.isEmpty() || ingredients.size() >= MAX_INGREDIENTS) {
            return false;
        }

        ItemStack inserted = source.copyWithCount(1);
        ingredients.add(inserted);
        source.shrink(1);
        changed();
        return true;
    }

    public List<ItemStack> ingredients() {
        return List.copyOf(ingredients);
    }

    public int ingredientCount() {
        return ingredients.size();
    }

    public boolean clearToPlayer(net.minecraft.world.entity.player.Player player) {
        if (ingredients.isEmpty()) {
            return false;
        }
        for (ItemStack stack : ingredients) {
            if (!player.getInventory().add(stack.copy())) {
                player.drop(stack.copy(), false);
            }
        }
        ingredients.clear();
        changed();
        return true;
    }

    public void clear() {
        ingredients.clear();
        changed();
    }

    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : ingredients) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        ingredients.clear();
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveIngredients(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveIngredients(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void saveIngredients(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ItemStack stack : ingredients) {
            CompoundTag entry = new CompoundTag();
            entry.putString("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            entry.putInt("count", stack.getCount());
            list.add(entry);
        }
        tag.put("ingredients", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ingredients.clear();
        ListTag list = tag.getList("ingredients", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && ingredients.size() < MAX_INGREDIENTS; i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = ResourceLocation.parse(entry.getString("item"));
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != net.minecraft.world.item.Items.AIR) {
                ingredients.add(new ItemStack(item, Math.max(1, entry.getInt("count"))));
            }
        }
    }
}
