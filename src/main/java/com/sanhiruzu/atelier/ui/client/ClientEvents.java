package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.client.AtelierKeys;
import com.sanhiruzu.atelier.client.ClientZoneCache;
import com.sanhiruzu.atelier.client.ZoneVfxManager;
import com.sanhiruzu.atelier.ui.adapter.ZoneHudAdapter;
import com.sanhiruzu.atelier.ui.network.RoomInspectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public final class ClientEvents {
    private static boolean lastRightClickState = false;

    private ClientEvents() {
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) return;
        ClientZoneCache.clear();
        ClientZoneData.clear();
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        // Poll keybind for bounds display
        if (AtelierKeys.SHOW_ZONE_BOUNDS.consumeClick()) {
            ZoneVfxManager.showBoundsTemporarily();
            ClientZoneData.revealRoomHud();
        }

        // Drain particle queue
        ZoneVfxManager.drainParticleQueue();

        ClientZoneData.tick();

        Minecraft mc = Minecraft.getInstance();
        ZoneHudAdapter.getCurrentSnapshot(mc).ifPresentOrElse(
                snapshot -> ClientZoneData.update(
                        snapshot.id(),
                        snapshot.name(),
                        snapshot.generatedName(),
                        snapshot.uniqueName(),
                        snapshot.customName(),
                        snapshot.activeProfiles(),
                        snapshot.score(),
                        snapshot.uniqueName()  // Quality breakdown is stored in uniqueName field
                ),
                () -> {
                    UUID lastId = ClientZoneData.getCurrentZoneId();
                    // Only flash "Room lost" if the zone itself is gone; if the player just
                    // walked out the zone still exists in the cache so we fade silently.
                    if (lastId != null && ClientZoneCache.getZone(lastId) != null) {
                        ClientZoneData.clearSilently();
                    } else {
                        ClientZoneData.clear();
                        if (ClientZoneData.isDebugMode()) {
                            ClientZoneData.forgetRoomSnapshot();
                        }
                    }
                }
        );
    }

    public static void onRightClickBlock(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        boolean isRightClickPressed = mc.options.keyUse.isDown();
        boolean isNewClick = isRightClickPressed && !lastRightClickState;
        lastRightClickState = isRightClickPressed;

        if (!isNewClick || !player.isShiftKeyDown()) {
            return;
        }

        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK || !isHoldingPatchouliBook(player)) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
        PacketDistributor.sendToServer(new RoomInspectPayload(blockHit.getBlockPos()));
    }

    private static boolean isHoldingPatchouliBook(LocalPlayer player) {
        return isPatchouliBook(player.getMainHandItem().getItem().getClass().getName())
                || isPatchouliBook(player.getOffhandItem().getItem().getClass().getName());
    }

    private static boolean isPatchouliBook(String itemClassName) {
        return itemClassName.contains("ItemModBook");
    }

    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(
                    ResourceLocation.withDefaultNamespace("experience_bar"),
                    ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "zen_meter"),
                    new ZenMeterOverlay()
            );
        }
    }
}
