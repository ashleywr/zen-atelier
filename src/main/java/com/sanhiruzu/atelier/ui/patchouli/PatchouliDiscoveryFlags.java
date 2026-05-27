package com.sanhiruzu.atelier.ui.patchouli;

import com.sanhiruzu.atelier.ZenAtelier;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public final class PatchouliDiscoveryFlags {
    private static final List<String> ROOM_TYPES = List.of(
            "atelier",
            "bedroom",
            "church",
            "enchanting_room",
            "farm_pen",
            "fletchery",
            "gardener_shed",
            "greenhouse",
            "kitchen",
            "library",
            "loom_room",
            "map_room",
            "masonry",
            "smithy",
            "storage_room",
            "tannery",
            "terrarium",
            "workshop"
    );

    private PatchouliDiscoveryFlags() {
    }

    public static List<String> roomTypes() {
        return ROOM_TYPES;
    }

    public static String roomFlag(String roomType) {
        return ZenAtelier.MODID + ":room/" + roomType.toLowerCase(Locale.ROOT);
    }

    public static String roomFlagForProfile(String profileId) {
        int separator = profileId.indexOf(':');
        String roomType = separator >= 0 ? profileId.substring(separator + 1) : profileId;
        return roomFlag(roomType);
    }

    public static void initializeKnownFlags() {
        withPatchouliFlagSetter((api, setFlag) -> {
            for (String roomType : ROOM_TYPES) {
                setFlag.invoke(api, roomFlag(roomType), false);
            }
        });
    }

    public static void syncDiscoveredFlags(Iterable<String> profileIds) {
        withPatchouliFlagSetter((api, setFlag) -> {
            for (String roomType : ROOM_TYPES) {
                setFlag.invoke(api, roomFlag(roomType), false);
            }
            for (String profileId : profileIds) {
                setFlag.invoke(api, roomFlagForProfile(profileId), true);
                setLegacyDiscoveryFlags(api, setFlag, profileId);
            }
        });
    }

    private static void setLegacyDiscoveryFlags(Object api, Method setFlag, String profileId)
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
        String key = (namespace + ":" + roomType).toLowerCase(Locale.ROOT);
        return key.contains("amphibia")
                || key.contains("amphib")
                || key.contains("frog")
                || key.contains("toad")
                || key.contains("terrarium")
                || key.contains("vivarium")
                || key.contains("habitat");
    }

    private static void withPatchouliFlagSetter(PatchouliFlagOperation operation) {
        try {
            Class<?> patchouliApi = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = patchouliApi.getMethod("get").invoke(null);
            Class<?> apiInterface = Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI");
            Method setFlag = apiInterface.getMethod("setConfigFlag", String.class, boolean.class);
            operation.apply(api, setFlag);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @FunctionalInterface
    private interface PatchouliFlagOperation {
        void apply(Object api, Method setFlag) throws ReflectiveOperationException;
    }
}
