package com.sanhiruzu.atelier.synthesis.gathering.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.gathering.GatheringBasketItem;
import com.sanhiruzu.atelier.synthesis.gathering.GatheringMarkerType;
import com.sanhiruzu.atelier.synthesis.gathering.GatheringPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class GatheringPointRenderer extends EntityRenderer<GatheringPoint> {
    private static final double ICON_FULL_SCALE_DISTANCE = 48.0D;
    private static final float ICON_MIN_SCALE = 0.13F;
    private static final float ICON_MAX_SCALE = 0.30F;
    private static final float STEM_MIN_HEIGHT = 0.82F;
    private static final float STEM_MAX_HEIGHT = 1.05F;
    private static final ResourceLocation HAND_TEXTURE = gatheringTexture("hand");
    private static final ResourceLocation PICK_TEXTURE = gatheringTexture("pick");
    private static final ResourceLocation HAMMER_TEXTURE = gatheringTexture("hammer");

    public GatheringPointRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            GatheringPoint entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null
                || (!GatheringBasketItem.isBasket(mc.player.getMainHandItem())
                && !GatheringBasketItem.isBasket(mc.player.getOffhandItem()))) {
            return;
        }

        Vec3 cameraPosition = entityRenderDispatcher.camera.getPosition();
        double distance = cameraPosition.distanceTo(entity.position());
        float iconScale = gatheringIconScale(distance);
        float stemHeight = gatheringStemHeight(distance);

        renderStem(poseStack, bufferSource.getBuffer(RenderType.lines()), stemHeight, iconScale);

        poseStack.pushPose();
        poseStack.translate(0.0D, stemHeight, 0.0D);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        renderTexturedQuad(
                poseStack,
                bufferSource.getBuffer(RenderType.entityTranslucent(gatheringIcon(entity.markerType()))),
                iconScale,
                -0.003F,
                1.0F
        );
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(GatheringPoint entity) {
        return gatheringIcon(entity.markerType());
    }

    public static float gatheringIconScale(double distance) {
        return Math.clamp(
                ICON_MIN_SCALE + (float) (distance / ICON_FULL_SCALE_DISTANCE) * (ICON_MAX_SCALE - ICON_MIN_SCALE),
                ICON_MIN_SCALE,
                ICON_MAX_SCALE
        );
    }

    public static float gatheringStemHeight(double distance) {
        return Math.clamp(
                STEM_MIN_HEIGHT + (float) (distance / ICON_FULL_SCALE_DISTANCE) * (STEM_MAX_HEIGHT - STEM_MIN_HEIGHT),
                STEM_MIN_HEIGHT,
                STEM_MAX_HEIGHT
        );
    }

    private static ResourceLocation gatheringIcon(GatheringMarkerType markerType) {
        return switch (markerType) {
            case ORE -> PICK_TEXTURE;
            case STRIKE -> HAMMER_TEXTURE;
            case FORAGE -> HAND_TEXTURE;
        };
    }

    private static ResourceLocation gatheringTexture(String path) {
        return ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "textures/gui/gathering/" + path + ".png");
    }

    private static void renderStem(PoseStack poseStack, VertexConsumer consumer, float height, float iconScale) {
        consumer.addVertex(poseStack.last().pose(), 0.0F, 0.03F, 0.0F)
                .setColor(1.0F, 1.0F, 1.0F, 0.72F)
                .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
        consumer.addVertex(poseStack.last().pose(), 0.0F, height - iconScale * 1.18F, 0.0F)
                .setColor(1.0F, 1.0F, 1.0F, 0.72F)
                .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
    }

    private static void renderTexturedQuad(PoseStack poseStack, VertexConsumer consumer, float size, float z, float alpha) {
        consumer.addVertex(poseStack.last().pose(), -size, -size, z)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);
        consumer.addVertex(poseStack.last().pose(), size, -size, z)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);
        consumer.addVertex(poseStack.last().pose(), size, size, z)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);
        consumer.addVertex(poseStack.last().pose(), -size, size, z)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);
    }
}
