package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileRegistry;
import com.sanhiruzu.atelier.synthesis.data.SynthesisProfileRegistry;
import com.sanhiruzu.atelier.ui.patchouli.PatchouliDiscoveryFlags;
import com.sanhiruzu.atelier.ui.network.DiscoveryDataSyncPayload;
import com.sanhiruzu.atelier.ui.network.ExtractionKnowledgeSyncPayload;
import com.sanhiruzu.atelier.ui.network.ReagentVaultSyncPayload;
import com.sanhiruzu.atelier.ui.network.RoomCatalogSyncPayload;
import com.sanhiruzu.atelier.ui.network.SynthesisCatalogSyncPayload;
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

    public static void handleExtractionKnowledgeSync(ExtractionKnowledgeSyncPayload payload) {
        ClientExtractionKnowledgeData.update(
                payload.knownSourceReagents(),
                payload.testedEmptySources(),
                payload.knownSourceDetails()
        );
    }

    public static void handleRoomCatalogSync(RoomCatalogSyncPayload payload) {
        ClientRoomCatalogData.update(payload.entries());
    }

    public static void handleSynthesisCatalogSync(SynthesisCatalogSyncPayload payload) {
        ExtractionProfileRegistry.replaceAll(payload.decodeExtractionProfiles());
        SynthesisProfileRegistry.replaceAll(payload.decodeSynthesisProfiles());
    }

    public static void handleReagentVaultSync(ReagentVaultSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof SynthesisStationScreen screen) {
            screen.handleReagentVaultSync(payload);
        }
    }
}
