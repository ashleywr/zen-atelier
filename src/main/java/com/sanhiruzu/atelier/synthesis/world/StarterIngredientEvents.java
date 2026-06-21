package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class StarterIngredientEvents {
    private StarterIngredientEvents() {
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
