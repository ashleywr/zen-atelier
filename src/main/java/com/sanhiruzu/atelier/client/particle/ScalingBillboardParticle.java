package com.sanhiruzu.atelier.client.particle;

import com.sanhiruzu.atelier.synthesis.vfx.data.Anchor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/**
 * A camera-facing particle that scales from 0 up to a peak over its grow phase, holds,
 * then fades alpha to 0 over its fade phase. Advances through its sprite set by age so
 * multi-frame definitions animate (e.g. crystal growth, spark twinkle).
 */
public class ScalingBillboardParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float peakScale;
    private final int growTicks;
    private final int fadeTicks;
    private final ParticleRenderType renderType;
    private final Anchor anchor;
    /** Spawn-point Y; the ground line for GROUND-anchored sprites. Visible to subclasses. */
    protected final double baseY;

    public ScalingBillboardParticle(ClientLevel level, double x, double y, double z,
                                    SpriteSet sprites, float peakScale, int lifetime,
                                    int growTicks, int fadeTicks) {
        this(level, x, y, z, sprites, peakScale, lifetime, growTicks, fadeTicks,
                ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT, Anchor.CENTER);
    }

    public ScalingBillboardParticle(ClientLevel level, double x, double y, double z,
                                    SpriteSet sprites, float peakScale, int lifetime,
                                    int growTicks, int fadeTicks, ParticleRenderType renderType) {
        this(level, x, y, z, sprites, peakScale, lifetime, growTicks, fadeTicks, renderType, Anchor.CENTER);
    }

    public ScalingBillboardParticle(ClientLevel level, double x, double y, double z,
                                    SpriteSet sprites, float peakScale, int lifetime,
                                    int growTicks, int fadeTicks, ParticleRenderType renderType, Anchor anchor) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.peakScale = peakScale;
        this.lifetime = Math.max(1, lifetime);
        this.growTicks = Math.max(1, growTicks);
        this.fadeTicks = Math.max(1, fadeTicks);
        this.renderType = renderType;
        this.anchor = anchor;
        this.baseY = y;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.quadSize = 0.01F;
        this.setSpriteFromAge(sprites);
        if (anchor == Anchor.GROUND) {
            this.y = baseY + halfHeightAt(0.0F);
        }
    }

    /** Half-height (= quad half-extent, in blocks) at a given age. */
    private float halfHeightAt(float t) {
        return Math.max(0.01F, (t < growTicks) ? peakScale * (t / growTicks) : peakScale);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(sprites);
            if (anchor == Anchor.GROUND) {
                // Keep the base planted on the spawn point as the sprite grows.
                this.y = baseY + halfHeightAt(this.age);
            }
        }
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float t = (this.age + partialTicks);
        int fadeStart = this.lifetime - fadeTicks;
        if (t >= fadeStart) {
            float f = 1.0F - Math.min(1.0F, (t - fadeStart) / fadeTicks);
            this.setAlpha(Math.max(0.0F, f));
        } else {
            this.setAlpha(1.0F);
        }
        return halfHeightAt(t);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return renderType;
    }
}
