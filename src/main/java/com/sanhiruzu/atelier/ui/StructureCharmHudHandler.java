package com.sanhiruzu.atelier.ui;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ZenAtelier.MODID, value = Dist.CLIENT)
public class StructureCharmHudHandler {

    private static String lastStructure = null;
    private static String lastStructureModId = null;
    private static int lastCheckTick = -1;

    public static String getCurrentStructure() {
        return lastStructure;
    }

    public static String getCurrentStructureModId() {
        return lastStructureModId;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            lastStructure = null;
            lastStructureModId = null;
            return;
        }

        LocalPlayer player = minecraft.player;
        if (!hasStructureCharm(player)) {
            lastStructure = null;
            lastStructureModId = null;
            return;
        }

        int currentTick = (int) (minecraft.level.getGameTime() % Integer.MAX_VALUE);
        if (currentTick == lastCheckTick || currentTick % 20 != 0) {
            return;
        }
        lastCheckTick = currentTick;

        findClosestStructure(player);
    }

    private static void findClosestStructure(LocalPlayer player) {
        BlockPos playerPos = player.blockPosition();

        if (player.level() == null) {
            return;
        }

        try {
            var level = player.level();
            var chunkSource = level.getChunkSource();
            ChunkAccess chunkAccess = chunkSource.getChunk(playerPos.getX() >> 4, playerPos.getZ() >> 4, false);

            if (chunkAccess == null) {
                lastStructure = null;
                lastStructureModId = null;
                return;
            }

            var structureStarts = chunkAccess.getAllStarts();
            double closestDistance = Double.MAX_VALUE;
            String closestStructure = null;
            String closestModId = null;

            for (var structureStart : structureStarts.values()) {
                if (structureStart != null && structureStart.isValid()) {
                    var bounds = structureStart.getBoundingBox();
                    double centerX = (bounds.minX() + bounds.maxX()) / 2.0;
                    double centerZ = (bounds.minZ() + bounds.maxZ()) / 2.0;
                    BlockPos center = new BlockPos((int) centerX, playerPos.getY(), (int) centerZ);

                    double distance = playerPos.distSqr(center);

                    if (distance < closestDistance) {
                        closestDistance = distance;
                        try {
                            var structureType = structureStart.getStructure();
                            if (structureType != null) {
                                closestStructure = structureType.toString();
                                closestModId = "minecraft";
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            lastStructure = closestStructure;
            lastStructureModId = closestModId;
        } catch (Exception e) {
            lastStructure = null;
            lastStructureModId = null;
        }
    }

    private static boolean hasStructureCharm(LocalPlayer player) {
        return hasCharmInSlot(player, 3) ||
                hasCharmInSlot(player, 40) ||
                hasCuriosCharm(player);
    }

    public static boolean hasCharmInSlot(LocalPlayer player, int slot) {
        try {
            if (slot >= 0 && slot < player.containerMenu.slots.size()) {
                var itemStack = player.containerMenu.getSlot(slot).getItem();
                return itemStack.is(ZenAtelier.STRUCTURE_CHARM.asItem());
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean hasCuriosCharm(LocalPlayer player) {
        try {
            Class<?> curiosClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            var method = curiosClass.getMethod("getCuriosInventory", net.minecraft.world.entity.player.Player.class);
            var optionalInventory = method.invoke(null, player);

            if (optionalInventory != null && optionalInventory.getClass().getMethod("isPresent").invoke(optionalInventory).equals(true)) {
                var inventory = optionalInventory.getClass().getMethod("get").invoke(optionalInventory);
                var stacksForSlot = inventory.getClass().getMethod("getStacksForSlot", String.class)
                        .invoke(inventory, "charm");

                if (stacksForSlot != null) {
                    var iterator = stacksForSlot.getClass().getMethod("iterator").invoke(stacksForSlot);
                    java.util.Iterator<?> iter = (java.util.Iterator<?>) iterator;
                    while (iter.hasNext()) {
                        var itemStack = iter.next();
                        var getItem = itemStack.getClass().getMethod("getItem");
                        if (getItem.invoke(itemStack).equals(ZenAtelier.STRUCTURE_CHARM.asItem())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
