package com.sanhiruzu.atelier.zone.discovery;

import com.sanhiruzu.atelier.advancement.AtelierCriteriaTriggers;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.ui.network.DiscoveryDataSyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RoomDiscoveryHandler {
    private RoomDiscoveryHandler() {
    }

    public static void handleDiscovery(ServerPlayer player, RoomData room) {
        if (room.getZoneTypeId() == null) {
            return;
        }

        String profileId = room.getZoneTypeId().toString();
        int quality = Math.round(room.getQuality() * 100);

        int initialCount = PlayerRoomDiscovery.getDiscoveredCount(player);
        boolean hasNewDiscoveries = false;

        PlayerRoomDiscovery.updateBestScore(player, profileId, quality);

        if (!PlayerRoomDiscovery.hasDiscovered(player, profileId)) {
            hasNewDiscoveries = true;
            PlayerRoomDiscovery.markDiscovered(player, profileId);

            int xp = xpForDiscovery(quality);
            player.giveExperiencePoints(xp);

            String roomTypeName = getRoomTypeName(room.getZoneTypeId());
            String message = Component.translatable("message.zen_atelier.room_discovered", roomTypeName).getString();
            player.displayClientMessage(Component.literal(message), false);

            AtelierCriteriaTriggers.ROOM_DISCOVERY.trigger(player, profileId);
        }

        int finalCount = PlayerRoomDiscovery.getDiscoveredCount(player);
        if (finalCount > initialCount) {
            checkProficiencyMilestone(player, finalCount);
        }

        if (hasNewDiscoveries) {
            PacketDistributor.sendToPlayer(
                    player,
                    new DiscoveryDataSyncPayload(PlayerRoomDiscovery.getAllBestScores(player))
            );
        }
    }

    private static int xpForDiscovery(int quality) {
        return Math.min(15, 3 + quality / 10);
    }

    private static void checkProficiencyMilestone(ServerPlayer player, int count) {
        String messageKey = null;

        if (count == 1) {
            messageKey = "message.zen_atelier.room_proficiency_1";
            AtelierCriteriaTriggers.MILESTONE_REACHED.trigger(player, 1);
        } else if (count == 5) {
            messageKey = "message.zen_atelier.room_proficiency_5";
            AtelierCriteriaTriggers.MILESTONE_REACHED.trigger(player, 5);
        } else if (count == 10) {
            messageKey = "message.zen_atelier.room_proficiency_10";
            AtelierCriteriaTriggers.MILESTONE_REACHED.trigger(player, 10);
        } else if (count >= 16) {
            messageKey = "message.zen_atelier.room_proficiency_all";
            AtelierCriteriaTriggers.MILESTONE_REACHED.trigger(player, 16);
        }

        if (messageKey != null) {
            player.displayClientMessage(Component.translatable(messageKey), true);
        }
    }

    private static String getRoomTypeName(ResourceLocation id) {
        String key = "room_type." + id.getNamespace() + "." + id.getPath();
        Component component = Component.translatable(key);
        String localized = component.getString();
        if (!localized.equals(key)) return localized;

        String path = id.getPath().replace('_', ' ');
        return path.isEmpty() ? id.toString() : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }
}
