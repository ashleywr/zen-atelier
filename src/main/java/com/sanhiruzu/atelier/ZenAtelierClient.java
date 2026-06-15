package com.sanhiruzu.atelier;

import com.sanhiruzu.atelier.client.particle.AdditiveParticleRenderType;
import com.sanhiruzu.atelier.client.particle.ScalingBillboardParticle;
import com.sanhiruzu.atelier.client.particle.ShatterOnDeathParticle;
import com.sanhiruzu.atelier.synthesis.gathering.client.GatheringPointRenderer;
import com.sanhiruzu.atelier.synthesis.vfx.ScaledParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
        // Crystal: grows, then shatters into ice_shatter + vanilla snowflakes.
        event.registerSpriteSet(ZenAtelier.ICE_CRYSTAL.get(),
                sprites -> (ScaledParticleOptions options, ClientLevel level,
                            double x, double y, double z, double xd, double yd, double zd) ->
                        new ShatterOnDeathParticle(level, x, y, z, sprites,
                                options.peakScale(), options.lifetime(),
                                ZenAtelier.ICE_SHATTER.get(), ParticleTypes.SNOWFLAKE, 4));

        // Burst: quick scale-up flash (long grow, short fade). Additive glow so the
        // full-frame energy sprite reads as light, not a translucent square.
        event.registerSpriteSet(ZenAtelier.ICE_BURST.get(),
                sprites -> (ScaledParticleOptions options, ClientLevel level,
                            double x, double y, double z, double xd, double yd, double zd) ->
                        new ScalingBillboardParticle(level, x, y, z, sprites,
                                options.peakScale(), options.lifetime(), 4, 6,
                                AdditiveParticleRenderType.INSTANCE));

        // Shatter: fixed-size quick fade, additive glow.
        event.registerSpriteSet(ZenAtelier.ICE_SHATTER.get(),
                sprites -> (SimpleParticleType type, ClientLevel level,
                            double x, double y, double z, double xd, double yd, double zd) ->
                        new ScalingBillboardParticle(level, x, y, z, sprites, 1.4F, 8, 2, 4,
                                AdditiveParticleRenderType.INSTANCE));

        // Spark: small animated twinkle, additive glow.
        event.registerSpriteSet(ZenAtelier.ICE_SPARK.get(),
                sprites -> (SimpleParticleType type, ClientLevel level,
                            double x, double y, double z, double xd, double yd, double zd) ->
                        new ScalingBillboardParticle(level, x, y, z, sprites, 0.6F, 12, 3, 5,
                                AdditiveParticleRenderType.INSTANCE));
    }
}
