package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AlchemicalThrowable extends ThrowableItemProjectile {

    public AlchemicalThrowable(EntityType<? extends AlchemicalThrowable> type, Level level) {
        super(type, level);
    }

    public AlchemicalThrowable(Level level, LivingEntity thrower) {
        super(ZenAtelier.ALCHEMICAL_THROWABLE.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ZenAtelier.FROST_GLOBE.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            Vec3 pos = result.getLocation();
            ItemStack stack = getItem();
            SynthesisOutputData data = stack.get(ZenAtelier.SYNTHESIS_OUTPUT_DATA.get());
            List<String> affixes = data != null ? data.affixes() : List.of();
            int qt = data != null ? data.qualityTier() : 0;

            Item item = stack.getItem();
            if (item == ZenAtelier.FROST_GLOBE.get()) {
                applyFrost(pos, affixes, qt);
            } else if (item == ZenAtelier.SPARK_CORE.get()) {
                applySpark(pos, affixes, qt);
            } else if (item == ZenAtelier.VOLATILE_BOMB_CORE.get()) {
                applyVolatile(pos, affixes, qt);
            } else if (item == ZenAtelier.RESONANT_BOMB_CORE.get()) {
                applyResonant(pos, affixes, qt);
            }
            this.discard();
        }
    }

    // --- frost_globe ---
    // zen_atelier:freezing  → Slowness (quality raises tier/duration/radius)
    // zen_atelier:wide_chill → expand radius (+1.5)
    // zen_atelier:low_damage → freeze damage (scales with quality)

    private static final int[]    FROST_DURATION  = {80,  120, 140, 200};
    private static final int[]    FROST_AMPLIFIER = {1,   1,   2,   2};
    private static final double[] FROST_RADIUS    = {3.0, 3.3, 3.7, 4.0};
    private static final float[]  FROST_DAMAGE    = {2.0f, 3.0f, 4.0f, 6.0f};

    private void applyFrost(Vec3 pos, List<String> affixes, int qt) {
        boolean freezing  = affixes.contains("zen_atelier:freezing");
        boolean wideChill = affixes.contains("zen_atelier:wide_chill");
        boolean lowDamage = affixes.contains("zen_atelier:low_damage");

        int duration    = freezing ? FROST_DURATION[qt]  : 40;
        int amplifier   = freezing ? FROST_AMPLIFIER[qt] : 0;
        double radius   = (freezing ? FROST_RADIUS[qt] : 2.0) + (wideChill ? 1.5 : 0.0);
        double radiusSq = radius * radius;

        AABB box = AABB.ofSize(pos, radius * 2, radius * 2, radius * 2);
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (entity.distanceToSqr(pos.x, pos.y, pos.z) > radiusSq) continue;
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier));
            if (lowDamage) {
                entity.hurt(level().damageSources().freeze(), FROST_DAMAGE[qt]);
            }
        }
    }

    // --- spark_core ---
    // zen_atelier:flash     → Blindness burst (quality extends duration/radius)
    // zen_atelier:weakening → Weakness (quality extends duration)
    // zen_atelier:aggro_break → reset mob target

    private static final int[]    SPARK_BLINDNESS = {40,  60,  80,  100};
    private static final int[]    SPARK_FIRE      = {3,   4,   6,   8};
    private static final double[] SPARK_RADIUS    = {2.5, 3.0, 3.5, 4.0};
    private static final int[]    SPARK_WEAKNESS  = {80,  80, 120, 120};

    private void applySpark(Vec3 pos, List<String> affixes, int qt) {
        boolean flash      = affixes.contains("zen_atelier:flash");
        boolean weakening  = affixes.contains("zen_atelier:weakening");
        boolean aggroBreak = affixes.contains("zen_atelier:aggro_break");

        double radius   = flash ? SPARK_RADIUS[qt] : 2.0;
        double radiusSq = radius * radius;

        AABB box = AABB.ofSize(pos, radius * 2, radius * 2, radius * 2);
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (entity.distanceToSqr(pos.x, pos.y, pos.z) > radiusSq) continue;
            if (flash) {
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, SPARK_BLINDNESS[qt], 0));
            }
            entity.igniteForSeconds(SPARK_FIRE[qt]);
            if (weakening) {
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SPARK_WEAKNESS[qt], 0));
            }
            if (aggroBreak && entity instanceof Mob mob) {
                mob.setTarget(null);
            }
        }
    }

    // --- volatile_bomb_core ---
    // zen_atelier:volatile  → explosion strength bonus (scales with quality)
    // zen_atelier:explosive → additional strength bonus (scales with quality)

    private static final float[] VOLATILE_MOD  = {0.50f, 0.65f, 0.80f, 1.00f};
    private static final float[] EXPLOSIVE_MOD = {1.00f, 1.20f, 1.40f, 1.70f};

    private void applyVolatile(Vec3 pos, List<String> affixes, int qt) {
        float strength = 2.0f;
        if (affixes.contains("zen_atelier:volatile"))  strength += VOLATILE_MOD[qt];
        if (affixes.contains("zen_atelier:explosive")) strength += EXPLOSIVE_MOD[qt];
        level().explode(this, pos.x, pos.y, pos.z, strength, Level.ExplosionInteraction.TNT);
    }

    // --- resonant_bomb_core ---
    // zen_atelier:explosive → stronger knockback (scales with quality)
    // zen_atelier:resonant  → wider radius (scales with quality)
    // zen_atelier:sparking  → ignite displaced entities
    // zen_atelier:luminous  → apply Glowing

    private static final double[] RESONANT_BASE_RADIUS    = {3.5, 4.0, 4.5, 5.0};
    private static final double[] RESONANT_BASE_STRENGTH  = {1.4, 1.6, 1.9, 2.2};
    private static final double[] RESONANT_EXTRA_RADIUS   = {1.5, 1.7, 2.0, 2.5};
    private static final double[] RESONANT_EXTRA_STRENGTH = {0.5, 0.6, 0.7, 0.8};

    private void applyResonant(Vec3 pos, List<String> affixes, int qt) {
        boolean explosive = affixes.contains("zen_atelier:explosive");
        boolean resonant  = affixes.contains("zen_atelier:resonant");
        boolean sparking  = affixes.contains("zen_atelier:sparking");
        boolean luminous  = affixes.contains("zen_atelier:luminous");

        double radius      = RESONANT_BASE_RADIUS[qt]   + (resonant  ? RESONANT_EXTRA_RADIUS[qt]   : 0.0);
        double baseStrength= RESONANT_BASE_STRENGTH[qt] + (explosive ? RESONANT_EXTRA_STRENGTH[qt] : 0.0);
        double radiusSq    = radius * radius;

        AABB box = AABB.ofSize(pos, radius * 2, radius * 2, radius * 2);
        Entity owner = this.getOwner();
        for (Entity entity : level().getEntities(this, box)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == owner) continue;
            Vec3 center = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
            Vec3 diff = center.subtract(pos);
            double distSq = diff.lengthSqr();
            if (distSq > radiusSq || distSq < 0.001) continue;
            double dist = Math.sqrt(distSq);
            double strength = (1.0 - dist / radius) * baseStrength;
            Vec3 push = diff.normalize().scale(strength);
            entity.setDeltaMovement(push.x, Math.max(push.y, strength * 0.35), push.z);
            entity.hurtMarked = true;
            if (sparking) {
                living.igniteForSeconds(3);
            }
            if (luminous) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            }
        }
    }
}
