package com.sanhiruzu.atelier.space;

import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

public class ClassificationTickHandler {
    private static final Map<ServerLevel, ClassificationScheduler> SCHEDULERS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        boolean checkZones = event.getServer().getTickCount() % 20 == 0;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            ClassificationScheduler scheduler = SCHEDULERS.computeIfAbsent(level, ClassificationScheduler::new);
            scheduler.tick();
            if (checkZones) {
                ZoneRegistry.get(level).checkAllPlayerZones(level);
            }
        }
    }

    public static ClassificationScheduler getScheduler(ServerLevel level) {
        return SCHEDULERS.computeIfAbsent(level, ClassificationScheduler::new);
    }

    public static void removeScheduler(ServerLevel level) {
        SCHEDULERS.remove(level);
    }
}
