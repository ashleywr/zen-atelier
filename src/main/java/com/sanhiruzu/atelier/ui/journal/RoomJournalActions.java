package com.sanhiruzu.atelier.ui.journal;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.List;

public final class RoomJournalActions {
    private static final ResourceLocation PATCHOULI_BOOK_ID = ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "room_journal");
    private static final double MAX_INSPECT_DISTANCE_SQ = 64.0D;

    private RoomJournalActions() {
    }

    public static void openOrDescribe(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && tryOpenPatchouli(serverPlayer)) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer && tryOpenVanillaBook(serverPlayer, hand)) {
            return;
        }

        player.sendSystemMessage(Component.literal("=== Room Journal ==="));
        player.sendSystemMessage(Component.literal("Rooms remember purpose through anchors, furniture, light, comfort, and clear use."));
        player.sendSystemMessage(Component.literal("Shift+Click a block to inspect the room you're in."));
        player.sendSystemMessage(Component.literal("Visit discovered rooms to learn their secrets."));
    }

    public static void inspectRoom(Player player, BlockPos pos) {
        // Room journal inspection disabled pending the atmosphere substrate.
        player.sendSystemMessage(Component.literal("Room Journal: Room inspection is not available yet."));
    }

    private static boolean tryOpenPatchouli(ServerPlayer player) {
        if (!ModList.get().isLoaded("patchouli")) {
            return false;
        }

        try {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = apiClass.getMethod("get").invoke(null);
            Class<?> apiInterface = Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI");
            Method openBook = apiInterface.getMethod("openBookGUI", ServerPlayer.class, ResourceLocation.class);
            openBook.invoke(api, player, PATCHOULI_BOOK_ID);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static boolean tryOpenVanillaBook(ServerPlayer player, InteractionHand hand) {
        try {
            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            List<Filterable<Component>> pages = List.of(
                    Filterable.passThrough(Component.literal("Rooms remember purpose through anchors, furniture, light, comfort, and clear use.")),
                    Filterable.passThrough(Component.literal("Use the journal carefully on a block to inspect the current classifier state.")),
                    Filterable.passThrough(Component.literal("The room profile backend is intentionally stubbed while Atelier's zone logic is rebuilt."))
            );
            WrittenBookContent content = new WrittenBookContent(
                    Filterable.passThrough("Room Journal"),
                    "Zen Atelier",
                    0,
                    pages,
                    true
            );
            book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
            player.openItemGui(book, hand);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
