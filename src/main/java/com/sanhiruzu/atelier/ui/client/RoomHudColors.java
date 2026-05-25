package com.sanhiruzu.atelier.ui.client;

import java.util.Locale;

public final class RoomHudColors {
    public static final int DEFAULT = 0xF0B8C8;
    public static final int DEGRADED = 0xD9902F;

    private RoomHudColors() {
    }

    public static int forTypeInfo(String typeInfo, boolean degraded) {
        if (degraded) return DEGRADED;
        if (typeInfo == null) return DEFAULT;
        String normalized = typeInfo.toLowerCase(Locale.ROOT);
        if (normalized.contains("enchanting")) return 0x9370DB;
        if (normalized.contains("greenhouse")) return 0x7ED979;
        if (normalized.contains("bedroom")) return 0xF0B8C8;
        if (normalized.contains("kitchen")) return 0xF2A65A;
        if (normalized.contains("smithy")) return 0xD48A5F;
        if (normalized.contains("storage")) return 0xB9A272;
        if (normalized.contains("library")) return 0xB997E8;
        if (normalized.contains("atelier")) return 0x75C6D8;
        if (normalized.contains("partial")) return 0xFFD36E;
        return DEFAULT;
    }
}
