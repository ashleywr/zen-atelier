package com.sanhiruzu.atelier.ui;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ZenAtelier.MODID, value = Dist.CLIENT)
public class StructureCharmOverlay {

    private static String lastDisplayedStructure = null;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        LocalPlayer player = minecraft.player;

        if (!hasStructureCharm(player)) {
            lastDisplayedStructure = null;
            return;
        }

        String structure = StructureCharmHudHandler.getCurrentStructure();
        String modId = StructureCharmHudHandler.getCurrentStructureModId();

        if (structure != null && !structure.equals(lastDisplayedStructure)) {
            lastDisplayedStructure = structure;

            String message = structure + " (" + (modId != null ? modId : "minecraft") + ")";
            player.displayClientMessage(
                    Component.literal(message).withStyle(ChatFormatting.AQUA),
                    true
            );
        }
    }

    private static boolean hasStructureCharm(LocalPlayer player) {
        return StructureCharmHudHandler.hasCharmInSlot(player, 3) ||
                StructureCharmHudHandler.hasCharmInSlot(player, 40) ||
                StructureCharmHudHandler.hasCuriosCharm(player);
    }
}
