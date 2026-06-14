package com.sanhiruzu.atelier.space;

import com.sanhiruzu.atelier.network.SyncChunkClassificationPayload;
import com.sanhiruzu.atelier.space.commit.SpaceRegionIndex;
import com.sanhiruzu.atelier.space.commit.ZoneCommitter;
import com.sanhiruzu.atelier.space.commit.ZoneStore;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class ClassificationTickHandler {
    private static final Map<ServerLevel, ClassificationScheduler> SCHEDULERS = new HashMap<>();
    private static final Map<String, ZoneStore> ZONE_STORES = new HashMap<>();
    private static final Map<String, SpaceRegionIndex> REGION_INDICES = new HashMap<>();
    private static final Map<String, ZoneCommitter> COMMITTERS = new HashMap<>();

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
        String key = dimensionKey(level);
        ClassificationScheduler removed = SCHEDULERS.remove(level);
        if (removed != null) removed.shutdown();
        ZONE_STORES.remove(key);
        REGION_INDICES.remove(key);
        COMMITTERS.remove(key);
    }

    public static ZoneStore getZoneStore(ServerLevel level) {
        return ZONE_STORES.computeIfAbsent(dimensionKey(level), k -> new ZoneStore());
    }

    public static SpaceRegionIndex getRegionIndex(ServerLevel level) {
        return REGION_INDICES.computeIfAbsent(dimensionKey(level), k -> new SpaceRegionIndex());
    }

    public static ZoneCommitter getCommitter(ServerLevel level) {
        return COMMITTERS.computeIfAbsent(dimensionKey(level), k -> new ZoneCommitter(
                getZoneStore(level),
                getRegionIndex(level),
                (chunkPos, data) -> sendChunkClassificationSync(level, chunkPos, data)
        ));
    }

    private static String dimensionKey(ServerLevel level) {
        return level.dimension().location().toString();
    }

    private static void sendChunkClassificationSync(ServerLevel level, ChunkPos pos, ChunkClassificationData data) {
        SyncChunkClassificationPayload payload = new SyncChunkClassificationPayload(pos.x, pos.z, data);
        PacketDistributor.sendToPlayersTrackingChunk(level, pos, payload);
    }
}
