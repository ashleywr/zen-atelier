package com.sanhiruzu.atelier.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

/**
 * Additive-blend particle render type for glow VFX. Mirrors vanilla
 * {@code PARTICLE_SHEET_TRANSLUCENT} but uses additive blending
 * ({@code SRC_ALPHA, ONE}) and does not write depth, so dark/low-alpha pixels add
 * nothing and bright pixels glow and stack. Ideal for full-frame energy sprites
 * (bursts, shatters, sparks) where a plain alpha blend would show the square
 * background.
 */
public final class AdditiveParticleRenderType {
    private AdditiveParticleRenderType() {}

    /** Same location as the vanilla particle atlas; inlined to avoid the deprecated TextureAtlas constant. */
    private static final ResourceLocation PARTICLE_ATLAS =
            ResourceLocation.withDefaultNamespace("textures/atlas/particles.png");

    public static final ParticleRenderType INSTANCE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, PARTICLE_ATLAS);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "ZEN_ADDITIVE";
        }
    };
}
