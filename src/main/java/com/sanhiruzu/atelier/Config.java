package com.sanhiruzu.atelier;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_MINECOLONIES_COLONIST_EFFECTS = BUILDER
            .comment("Whether Atelier should apply room-quality mob effects directly to MineColonies colonists. Disabled by default because some MineColonies releases react poorly to external effect refreshes.")
            .define("enableMineColoniesColonistEffects", false);

    public static final ModConfigSpec.BooleanValue DISABLE_ZONE_SCANNING = BUILDER
            .comment("Disables all background zone/chunk scanning. Zones will not be detected or updated. Useful for testing other systems (e.g. synthesis) without zone classification overhead.")
            .define("disableZoneScanning", false);

    static final ModConfigSpec SPEC = BUILDER.build();
}
