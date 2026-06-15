package com.sanhiruzu.atelier.synthesis.gathering;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.vfx.AlchemyVfx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GatheringPoint extends Entity {
    private static final int LIFETIME_TICKS = 20 * 75;
    private static final String MARKER_TYPE_TAG = "marker_type";
    private static final EntityDataAccessor<String> DATA_MARKER_TYPE = SynchedEntityData.defineId(
            GatheringPoint.class,
            EntityDataSerializers.STRING
    );

    public GatheringPoint(EntityType<? extends GatheringPoint> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public GatheringPoint(Level level, double x, double y, double z) {
        this(ZenAtelier.GATHERING_POINT.get(), level);
        setPos(x, y, z);
    }

    public GatheringPoint(Level level, double x, double y, double z, GatheringMarkerType markerType) {
        this(level, x, y, z);
        setMarkerType(markerType);
    }

    @Override
    public void tick() {
        super.tick();
        noPhysics = true;
        setDeltaMovement(0, 0, 0);
        if (!level().isClientSide && tickCount > LIFETIME_TICKS) {
            discard();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack basket = GatheringPointSpawner.heldBasket(player);
        if (basket.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            ReagentStack reagent = GatheringReagentRoller.roll(level(), blockPosition(), player.getRandom());
            GatheringBasketItem.insert(basket, reagent);
            player.displayClientMessage(GatheringBasketItem.gatheredMessage(reagent), true);
            playCollectedEffects(reagent);
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void playCollectedEffects(ReagentStack reagent) {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.38F, 1.35F);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.28F, 1.65F);
        if (level() instanceof ServerLevel serverLevel) {
            AlchemyVfx.gatheringCollected(serverLevel, position(), reagent);
        }
    }

    public GatheringMarkerType markerType() {
        return GatheringMarkerType.fromSerializedName(entityData.get(DATA_MARKER_TYPE));
    }

    public void setMarkerType(GatheringMarkerType markerType) {
        entityData.set(DATA_MARKER_TYPE, markerType == null
                ? GatheringMarkerType.FORAGE.serializedName()
                : markerType.serializedName());
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DATA_MARKER_TYPE, GatheringMarkerType.FORAGE.serializedName());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        setMarkerType(GatheringMarkerType.fromSerializedName(compound.getString(MARKER_TYPE_TAG)));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putString(MARKER_TYPE_TAG, markerType().serializedName());
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0 * 96.0;
    }
}
