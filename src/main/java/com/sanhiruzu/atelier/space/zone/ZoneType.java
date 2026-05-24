package com.sanhiruzu.atelier.space.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public abstract class ZoneType {
    public abstract ResourceLocation getId();

    public abstract boolean matches(RoomData room, Level level, BlockPos pos);

    public abstract float qualityMultiplier();

    // Below this enclosure score, a matched room is flagged as degraded (e.g. "outdoor bedroom").
    // Subclasses override per culture/use case — a clean room demands ~1.0, a relaxed bedroom 0.4.
    public float minimumEnclosureScore() {
        return 0.5f;
    }
}
