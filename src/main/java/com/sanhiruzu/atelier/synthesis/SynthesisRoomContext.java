package com.sanhiruzu.atelier.synthesis;

import com.sanhiruzu.atelier.space.SpaceQuery;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public record SynthesisRoomContext(boolean inAtelier, int quality) {
    public static final SynthesisRoomContext NONE = new SynthesisRoomContext(false, 0);

    public static SynthesisRoomContext at(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return NONE;
        }

        ZoneData zone = SpaceQuery.getRoomAt(level, pos);
        if (!(zone instanceof RoomData room) || room.getZoneTypeId() == null) {
            return NONE;
        }

        boolean inAtelier = room.getZoneTypeId().toString().equals("zen_atelier:atelier");
        int quality = Math.clamp(Math.round(room.getQuality() * 100.0f), 0, 100);
        return new SynthesisRoomContext(inAtelier, quality);
    }
}
