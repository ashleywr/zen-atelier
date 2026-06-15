package com.sanhiruzu.atelier.event;

import com.sanhiruzu.atelier.network.ToggleDebugPayload;
import com.sanhiruzu.atelier.synthesis.world.CauldronExtractionService;
import com.sanhiruzu.atelier.synthesis.world.PlayerExtractionKnowledge;
import com.sanhiruzu.atelier.synthesis.world.PlayerSynthesisKnowledge;
import com.sanhiruzu.atelier.ui.network.SynthesisCatalogSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "zen_atelier")
public class AtelierEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    SynthesisCatalogSyncPayload.current()
            );
            PlayerExtractionKnowledge.sync(serverPlayer);
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new ToggleDebugPayload(serverPlayer.getPersistentData().getBoolean("spaceregion_debug"))
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerExtractionKnowledge.copy(event.getOriginal(), event.getEntity());
        PlayerSynthesisKnowledge.copy(event.getOriginal(), event.getEntity());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerExtractionKnowledge.sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SynthesisCatalogSyncPayload synthesisPayload = SynthesisCatalogSyncPayload.current();
        event.getRelevantPlayers().forEach(player -> {
            PacketDistributor.sendToPlayer(player, synthesisPayload);
        });
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            CauldronExtractionService.tick(level);
        }
    }
}
