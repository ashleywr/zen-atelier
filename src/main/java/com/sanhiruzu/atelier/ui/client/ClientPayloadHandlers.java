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
                setDiscoveryFlags(api, setFlag, profileId);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void setDiscoveryFlags(Object api, java.lang.reflect.Method setFlag, String profileId)
            throws ReflectiveOperationException {
        int separator = profileId.indexOf(':');
        String namespace = separator >= 0 ? profileId.substring(0, separator) : ZenAtelier.MODID;
        String roomType = separator >= 0 ? profileId.substring(separator + 1) : profileId;

        setFlag.invoke(api, ZenAtelier.MODID + ".discovered." + roomType, true);
        setFlag.invoke(api, ZenAtelier.MODID + ".discovered.namespace." + namespace, true);

        if (isAmphibianHabitatProfile(namespace, roomType)) {
            setFlag.invoke(api, ZenAtelier.MODID + ".discovered.amphibian_habitat", true);
        }
    }

    private static boolean isAmphibianHabitatProfile(String namespace, String roomType) {
        String key = (namespace + ":" + roomType).toLowerCase(java.util.Locale.ROOT);
        return key.contains("amphibia")
                || key.contains("amphib")
                || key.contains("frog")
                || key.contains("toad")
                || key.contains("terrarium")
                || key.contains("vivarium")
                || key.contains("habitat");
    }
}
