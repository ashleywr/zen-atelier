package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.ui.patchouli.PatchouliDiscoveryFlags;
import com.sanhiruzu.atelier.ui.network.DiscoveryDataSyncPayload;
import com.sanhiruzu.atelier.ui.network.RoomCatalogSyncPayload;
import net.minecraft.client.Minecraft;

public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void handleDebugToggle(boolean enabled) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getPersistentData().putBoolean("spaceregion_debug", enabled);
            ClientZoneData.setDebugMode(enabled);
            ZenAtelier.LOGGER.info("Client received debug toggle: {}", enabled);
        }
    }

    public static void handleDiscoveryDataSync(DiscoveryDataSyncPayload payload) {
        ClientDiscoveryData.update(payload.discoveredRooms());
        PatchouliDiscoveryFlags.syncDiscoveredFlags(payload.discoveredRooms().keySet());
    }

    public static void handleRoomCatalogSync(RoomCatalogSyncPayload payload) {
        ClientRoomCatalogData.update(payload.entries());
    }

}
