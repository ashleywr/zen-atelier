package com.sanhiruzu.atelier.event;

import com.sanhiruzu.atelier.network.ToggleDebugPayload;
import com.sanhiruzu.atelier.space.SpaceQuery;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.ui.network.DiscoveryDataSyncPayload;
import com.sanhiruzu.atelier.zone.bonus.RoomPresenceEffects;
import com.sanhiruzu.atelier.zone.discovery.PlayerRoomDiscovery;
import com.sanhiruzu.atelier.zone.discovery.RoomDiscoveryHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "zen_atelier")
public class AtelierEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new DiscoveryDataSyncPayload(PlayerRoomDiscovery.getAllBestScores(serverPlayer))
            );
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new ToggleDebugPayload(serverPlayer.getPersistentData().getBoolean("spaceregion_debug"))
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ZoneData zone = SpaceQuery.getRoomAt(serverLevel, player.blockPosition());
        if (!(zone instanceof RoomData room)) {
            return;
        }

        RoomPresenceEffects.applyPresenceEffects(player, room);
        RoomDiscoveryHandler.handleDiscovery(player, room);
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            Player player = event.getEntity();
            ZoneData zone = SpaceQuery.getRoomAt(serverLevel, player.blockPosition());

            if (!(zone instanceof RoomData room) || room.getZoneTypeId() == null) {
                return;
            }

            String profileId = room.getZoneTypeId().toString();
            if (!profileId.equals("zen_atelier:bedroom")) {
                return;
            }

            int sleepQuality = Math.round(room.getQuality() * 100);
            if (sleepQuality < 35) {
                ((ServerPlayer) player).addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
                player.displayClientMessage(Component.translatable("message.zen_atelier.poor_sleep"), true);
            } else if (sleepQuality < 55) {
                player.displayClientMessage(Component.translatable("message.zen_atelier.restless_sleep"), true);
            } else {
                // Apply boost: Regeneration II for 30 seconds and Saturation for 1 second
                ((ServerPlayer) player).addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
                ((ServerPlayer) player).addEffect(new MobEffectInstance(MobEffects.SATURATION, 20, 0));
                player.displayClientMessage(Component.translatable("message.zen_atelier.well_rested"), true);
            }
            player.displayClientMessage(Component.translatable("message.zen_atelier.journal_tip"), true);
        }
    }
}
