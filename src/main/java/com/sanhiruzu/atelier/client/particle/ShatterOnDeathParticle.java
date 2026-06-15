package com.sanhiruzu.atelier.client.particle;

import com.sanhiruzu.atelier.synthesis.vfx.data.Anchor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;

/**
 * A growing billboard that, on its final tick, spawns a shatter particle plus a few
 * accent particles at its base. Behavior (size/lifetime/grow/fade/anchor/render) is
 * supplied by the caller, keeping this element-agnostic.
 */
public class ShatterOnDeathParticle extends ScalingBillboardParticle {
    private final ParticleOptions shatter;
    private final ParticleOptions accent;
    private final int accentCount;
    private boolean shattered;

    public ShatterOnDeathParticle(ClientLevel level, double x, double y, double z,
                                  SpriteSet sprites, float peakScale, int lifetime,
                                  int growTicks, int fadeTicks, ParticleRenderType renderType, Anchor anchor,
                                  ParticleOptions shatter, ParticleOptions accent, int accentCount) {
        super(level, x, y, z, sprites, peakScale, lifetime, growTicks, fadeTicks, renderType, anchor);
        this.shatter = shatter;
        this.accent = accent;
        this.accentCount = accentCount;
    }

    @Override
    public void tick() {
        if (!shattered && this.age >= this.lifetime - 1) {
            shattered = true;
            this.level.addParticle(shatter, this.x, this.baseY, this.z, 0.0, 0.0, 0.0);
            for (int i = 0; i < accentCount; i++) {
                double vx = (this.random.nextDouble() - 0.5) * 0.18;
                double vy = this.random.nextDouble() * 0.12;
                double vz = (this.random.nextDouble() - 0.5) * 0.18;
                this.level.addParticle(accent, this.x, this.y, this.z, vx, vy, vz);
            }
        }
        super.tick();
    }
}
