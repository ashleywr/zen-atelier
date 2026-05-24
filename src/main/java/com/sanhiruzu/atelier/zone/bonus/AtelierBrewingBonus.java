package com.sanhiruzu.atelier.zone.bonus;

import com.sanhiruzu.atelier.space.SpaceQuery;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class AtelierBrewingBonus {
    public static final String ATELIER_PROFILE = "zen_atelier:atelier";

    private AtelierBrewingBonus() {
    }

    public static int extraCountdownTicks(Level level, BlockPos pos) {
        if (level == null || level.isClientSide) {
            return 0;
        }

        ZoneData zone = SpaceQuery.getRoomAt(level, pos);
        if (!(zone instanceof RoomData room) || room.getZoneTypeId() == null) {
            return 0;
        }

        if (!room.getZoneTypeId().toString().equals(ATELIER_PROFILE)) {
            return 0;
        }

        int score = Math.max(0, Math.min(100, Math.round(room.getQuality() * 100)));
        int interval = 5 - Math.min(4, score / 25);
        return level.getGameTime() % interval == 0L ? 1 : 0;
    }
}
