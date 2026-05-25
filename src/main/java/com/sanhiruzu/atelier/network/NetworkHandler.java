package com.sanhiruzu.atelier.network;

import com.sanhiruzu.atelier.client.ChunkClassificationClientData;
import com.sanhiruzu.atelier.client.ClientZoneCache;
import com.sanhiruzu.atelier.client.ZoneVfxManager;
import com.sanhiruzu.atelier.space.zone.Zone;
import com.sanhiruzu.atelier.space.zone.ZoneAttachment;
import com.sanhiruzu.atelier.ui.network.DiscoveryDataSyncPayload;
import com.sanhiruzu.atelier.ui.network.RoomInspectPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NetworkHandler {
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar("1.0")
                .playToClient(SyncChunkClassificationPayload.TYPE, SyncChunkClassificationPayload.CODEC,
                        NetworkHandler::handleChunkClassificationSync)
                .playToClient(ToggleDebugPayload.TYPE, ToggleDebugPayload.CODEC,
                        NetworkHandler::handleToggleDebug)
                .playToClient(DiscoveryDataSyncPayload.TYPE, DiscoveryDataSyncPayload.CODEC,
                        NetworkHandler::handleDiscoveryDataSync)
                .playToClient(SyncZoneGridPayload.TYPE, SyncZoneGridPayload.CODEC,
                        NetworkHandler::handleZoneGridSync)
                .playToClient(SyncPlayerZonePayload.TYPE, SyncPlayerZonePayload.CODEC,
                        NetworkHandler::handlePlayerZoneSync)
                .playToClient(RemoveZonePayload.TYPE, RemoveZonePayload.CODEC,
                        NetworkHandler::handleRemoveZone)
                .playToClient(ZoneQualityChangePayload.TYPE, ZoneQualityChangePayload.CODEC,
                        NetworkHandler::handleZoneQualityChange)
                .playToClient(ZoneGracePeriodPayload.TYPE, ZoneGracePeriodPayload.CODEC,
                        NetworkHandler::handleZoneGracePeriod)
                .playToServer(RoomInspectPayload.TYPE, RoomInspectPayload.CODEC,
                        NetworkHandler::handleRoomInspect);
    }

    private static void handleZoneGridSync(SyncZoneGridPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientZoneCache.storeZone(payload));
    }

    private static void handleZoneQualityChange(ZoneQualityChangePayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                ZoneVfxManager.onQualityChanged(
                        payload.oldQuality(), payload.newQuality(),
                        payload.triggerX(), payload.triggerY(), payload.triggerZ())
        );
    }

    private static void handleRemoveZone(RemoveZonePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ZoneVfxManager.onZoneExpired(payload.minX(), payload.minY(), payload.minZ(), payload.maxX(), payload.maxY(), payload.maxZ());
            ClientZoneCache.remove(payload.zoneId());
            // Zone fully gone — stop showing grace period countdown if still running
            com.sanhiruzu.atelier.ui.client.ClientZoneData.cancelGracePeriod(payload.zoneId());
        });
    }

    private static void handleZoneGracePeriod(ZoneGracePeriodPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.ticksRemaining() > 0) {
                com.sanhiruzu.atelier.ui.client.ClientZoneData.startGracePeriod(payload.zoneId(), payload.ticksRemaining());
            } else {
                com.sanhiruzu.atelier.ui.client.ClientZoneData.cancelGracePeriod(payload.zoneId());
            }
        });
    }

    private static void handlePlayerZoneSync(SyncPlayerZonePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player != null) {
                Zone zone = payload.zoneId() != null ? new Zone(payload.zoneId()) : null;
                player.getData(ZoneAttachment.ZONE.get()).setCurrentZone(zone);
            }
        });
    }

    private static void handleChunkClassificationSync(SyncChunkClassificationPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                ChunkClassificationClientData.storeChunkData(payload.chunkX(), payload.chunkZ(), payload.data())
        );
    }

    private static void handleToggleDebug(ToggleDebugPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleClientDebugToggle(payload));
    }

    private static void handleDiscoveryDataSync(DiscoveryDataSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleClientDiscoveryDataSync(payload));
    }

    private static void handleRoomInspect(RoomInspectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                payload.handle(serverPlayer);
            }
        });
    }

    private static void handleClientDebugToggle(ToggleDebugPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> handlers = Class.forName("com.sanhiruzu.atelier.ui.client.ClientPayloadHandlers");
                handlers.getMethod("handleDebugToggle", boolean.class).invoke(null, payload.enabled());
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void handleClientDiscoveryDataSync(DiscoveryDataSyncPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> handlers = Class.forName("com.sanhiruzu.atelier.ui.client.ClientPayloadHandlers");
                handlers.getMethod("handleDiscoveryDataSync", DiscoveryDataSyncPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
