package com.sanhiruzu.atelier;

import com.sanhiruzu.atelier.client.particle.AdditiveParticleRenderType;
import com.sanhiruzu.atelier.client.particle.ScalingBillboardParticle;
import com.sanhiruzu.atelier.client.particle.ShatterOnDeathParticle;
import com.sanhiruzu.atelier.synthesis.gathering.client.GatheringPointRenderer;
import com.sanhiruzu.atelier.synthesis.vfx.ScaledParticleOptions;
import com.sanhiruzu.atelier.synthesis.vfx.data.Anchor;
import com.sanhiruzu.atelier.synthesis.vfx.data.Blend;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = ZenAtelier.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ZenAtelier.MODID, value = Dist.CLIENT)
public class ZenAtelierClient {
    public ZenAtelierClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ZenAtelier.LOGGER.info("HELLO FROM CLIENT SETUP");
        ZenAtelier.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ZenAtelier.ALCHEMICAL_THROWABLE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ZenAtelier.GATHERING_POINT.get(), GatheringPointRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        // Crystal: behavior from options; intrinsic shatter into ice_shatter + snowflakes.
        event.registerSpriteSet(ZenAtelier.ICE_CRYSTAL.get(),
                sprites -> (ScaledParticleOptions o, ClientLevel level,
                            double x, double y, double z, double xd, double yd, double zd) ->
                        new ShatterOnDeathParticle(level, x, y, z, sprites,
                                o.peakScale(), o.lifetime(), o.growTicks(), o.fadeTicks(),
                                renderTypeFor(o.blend()), o.anchor(),
                                ZenAtelier.ICE_SHATTER.get(), ParticleTypes.SNOWFLAKE, 4));

        // Burst + spark: plain billboards driven entirely by options.
        event.registerSpriteSet(ZenAtelier.ICE_BURST.get(),
                sprites -> (ScaledParticleOptions o, ClientLevel level,
                            double x, double y, double z, double xd, double yd, double zd) ->
                        new ScalingBillboardParticle(level, x, y, z, sprites,
                                o.peakScale(), o.lifetime(), o.growTicks(), o.fadeTicks(),
                                renderTypeFor(o.blend()), o.anchor()));
        event.registerSpriteSet(ZenAtelier.ICE_SPARK.get(),
                sprites -> (ScaledParticleOptions o, ClientLevel level,
                            double x, double y, double z, double xd, double yd, double zd) ->
                        new ScalingBillboardParticle(level, x, y, z, sprites,
                                o.peakScale(), o.lifetime(), o.growTicks(), o.fadeTicks(),
                                renderTypeFor(o.blend()), o.anchor()));

        // Shatter: simple type with fixed behavior (spawned by the crystal on death).
        event.registerSpriteSet(ZenAtelier.ICE_SHATTER.get(),
                sprites -> (SimpleParticleType type, ClientLevel level,
                            double x, double y, double z, double xd, double yd, double zd) ->
                        new ScalingBillboardParticle(level, x, y, z, sprites, 1.4F, 8, 2, 4,
                                AdditiveParticleRenderType.INSTANCE, Anchor.GROUND));
    }

    private static ParticleRenderType renderTypeFor(Blend blend) {
        return blend == Blend.ADDITIVE
                ? AdditiveParticleRenderType.INSTANCE
                : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
