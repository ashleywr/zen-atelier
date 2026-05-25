package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.ui.network.DiscoveryDataSyncPayload;
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
        trySetPatchouliFlags(payload);
    }

    private static void trySetPatchouliFlags(DiscoveryDataSyncPayload payload) {
        try {
            Class<?> patchouliApi = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = patchouliApi.getMethod("get").invoke(null);
            Class<?> apiInterface = Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI");
            java.lang.reflect.Method setFlag = apiInterface.getMethod("setConfigFlag", String.class, boolean.class);

            for (String profileId : payload.discoveredRooms().keySet()) {
                String roomType = profileId.contains(":") ? profileId.substring(profileId.indexOf(":") + 1) : profileId;
                setFlag.invoke(api, ZenAtelier.MODID + ".discovered." + roomType, true);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
