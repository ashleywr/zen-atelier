package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Collection;
import java.util.List;

public final class StarterIngredientEvents {
    private static final float UNI_LEAF_CHANCE = 0.025F;
    private static final float TAUN_GRASS_CHANCE = 0.08F;
    private static final float TAUN_FOREST_GRASS_CHANCE = 0.14F;
    private static final float PHLOGISTON_NETHERRACK_CHANCE = 0.18F;
    private static final float PHLOGISTON_LAVA_STONE_CHANCE = 0.08F;
    private static final float SLIME_GEL_BASE_CHANCE = 0.35F;
    private static final float SLIME_GEL_SIZE_BONUS = 0.10F;

    private StarterIngredientEvents() {
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        maybeAddBlockDrop(event.getLevel(), event.getPos(), event.getState(), event.getDrops(), event.getLevel().getRandom());
    }

    public static void maybeAddBlockDrop(Level level, BlockPos pos, BlockState state, List<ItemEntity> drops, RandomSource random) {
        if (state.is(Blocks.OAK_LEAVES) || state.is(Blocks.BIRCH_LEAVES)) {
            addChanceDrop(level, pos, drops, new ItemStack(ZenAtelier.UNI.get()), UNI_LEAF_CHANCE, random);
            return;
        }

        if (isTaunForage(state)) {
            float chance = level.getBiome(pos).is(BiomeTags.IS_FOREST) ? TAUN_FOREST_GRASS_CHANCE : TAUN_GRASS_CHANCE;
            addChanceDrop(level, pos, drops, new ItemStack(ZenAtelier.TAUN_HERB.get()), chance, random);
            return;
        }

        if (state.is(Blocks.NETHERRACK)) {
            addChanceDrop(level, pos, drops, new ItemStack(ZenAtelier.PHLOGISTON_PEBBLE.get()), PHLOGISTON_NETHERRACK_CHANCE, random);
            return;
        }

        if (state.is(BlockTags.BASE_STONE_OVERWORLD) && hasAdjacentLava(level, pos)) {
            addChanceDrop(level, pos, drops, new ItemStack(ZenAtelier.PHLOGISTON_PEBBLE.get()), PHLOGISTON_LAVA_STONE_CHANCE, random);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Slime slime)) {
            return;
        }

        maybeAddSlimeDrop(slime, event.getDrops(), slime.level().getRandom());
    }

    public static void maybeAddSlimeDrop(Slime slime, Collection<ItemEntity> drops, RandomSource random) {
        float chance = Math.min(0.85F, SLIME_GEL_BASE_CHANCE + slime.getSize() * SLIME_GEL_SIZE_BONUS);
        if (random.nextFloat() >= chance) {
            return;
        }

        ItemStack gel = new ItemStack(isHotSlime(slime) ? ZenAtelier.EMBER_GEL.get() : ZenAtelier.AQUA_GEL.get());
        drops.add(new ItemEntity(slime.level(), slime.getX(), slime.getY() + 0.25D, slime.getZ(), gel));
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Snowball snowball)
                || !snowball.getItem().is(ZenAtelier.UNI.get())
                || !(event.getRayTraceResult() instanceof EntityHitResult entityHit)) {
            return;
        }

        applyUniImpact(snowball, entityHit.getEntity());
    }

    public static boolean applyUniImpact(Snowball snowball, Entity target) {
        if (!snowball.getItem().is(ZenAtelier.UNI.get())) {
            return false;
        }
        Entity owner = snowball.getOwner();
        Entity attacker = owner != null && owner.isAddedToLevel() ? owner : snowball;
        return target.hurt(target.damageSources().thrown(snowball, attacker), 2.0F);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity) || itemEntity.level().isClientSide) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if ((!stack.is(ZenAtelier.AQUA_GEL.get()) && !stack.is(ZenAtelier.EMBER_GEL.get()))
                || !itemEntity.onGround()
                || itemEntity.getAge() < 5) {
            return;
        }

        List<Entity> nearby = itemEntity.level().getEntities(
                itemEntity,
                itemEntity.getBoundingBox().inflate(0.45D, 0.35D, 0.45D),
                entity -> entity instanceof Player player && !player.isSpectator() && player.onGround()
        );
        if (nearby.isEmpty()) {
            return;
        }

        bouncePlayerFromGel(itemEntity, (Player) nearby.getFirst());
    }

    public static boolean bouncePlayerFromGel(ItemEntity itemEntity, Player player) {
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty() || (!stack.is(ZenAtelier.AQUA_GEL.get()) && !stack.is(ZenAtelier.EMBER_GEL.get()))) {
            return false;
        }

        bounceFromGel(player, stack.is(ZenAtelier.EMBER_GEL.get()));
        stack.shrink(1);
        if (stack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(stack);
        }
        return true;
    }

    private static void addChanceDrop(Level level, BlockPos pos, List<ItemEntity> drops, ItemStack stack, float chance, RandomSource random) {
        if (random.nextFloat() < chance) {
            drops.add(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack));
        }
    }

    private static boolean isTaunForage(BlockState state) {
        return state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN);
    }

    private static boolean hasAdjacentLava(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHotSlime(Slime slime) {
        return slime.level().getBiome(slime.blockPosition()).is(BiomeTags.IS_NETHER)
                || slime.level().getBiome(slime.blockPosition()).value().getBaseTemperature() >= 1.0F;
    }

    private static void bounceFromGel(Player player, boolean ember) {
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, Math.max(0.85D, movement.y + 0.85D), movement.z);
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 0));
        if (ember) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0));
        }
    }
}
