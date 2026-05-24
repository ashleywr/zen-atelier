package com.sanhiruzu.atelier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sanhiruzu.atelier.client.DebugRenderer;
import com.sanhiruzu.atelier.client.ZoneVfxManager;
import com.sanhiruzu.atelier.space.zone.ZoneAttachment;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.ui.client.ClientZoneData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = ZenAtelier.MODID, value = Dist.CLIENT)
public class ZenAtelierRenderEvents {
    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = mc.renderBuffers().bufferSource();

        if (ClientZoneData.isDebugMode()) {
            DebugRenderer.renderDebug(poseStack, buffers, mc);
        }

        // Render zone bounds if keybind is active
        if (ClientZoneData.isDebugMode() || ZoneVfxManager.isBoundsVisible()) {
            var zone = mc.player.getData(ZoneAttachment.ZONE.get()).getCurrentZone();
            if (zone != null) {
                ZoneData zoneData = com.sanhiruzu.atelier.client.ClientZoneCache.getZone(zone.getId());
                if (zoneData != null && zoneData.hasSpatialExtent()) {
                    float alpha = ClientZoneData.isDebugMode() ? 1.0f : ZoneVfxManager.getBoundsAlpha();
                    int color = ClientZoneData.isDebugMode()
                            ? DebugRenderer.colorForZone(zone.getId())
                            : zoneData instanceof com.sanhiruzu.atelier.space.zone.RoomData room && room.isDegraded()
                              ? 0xFFAA44  // Amber for degraded
                              : 0x88CCFF; // Light blue for normal
                    renderBoundsBox(poseStack, buffers, zoneData, color, alpha);
                }
            }
        }
    }

    private static void renderBoundsBox(PoseStack poseStack, MultiBufferSource buffers, ZoneData zone, int color, float alpha) {
        double minX = zone.getMinX();
        double minY = zone.getMinY();
        double minZ = zone.getMinZ();
        double maxX = zone.getMaxX() + 1;
        double maxY = zone.getMaxY() + 1;
        double maxZ = zone.getMaxZ() + 1;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        AABB aabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()),
                aabb, r / 255f, g / 255f, b / 255f, alpha);
    }
}
