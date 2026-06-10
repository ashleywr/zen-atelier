package com.sanhiruzu.atelier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.sanhiruzu.atelier.space.ClassificationTickHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@SuppressWarnings("SameReturnValue")
public class DebugCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("zen-atelier")
                .then(Commands.literal("debug")
                        .executes(DebugCommand::toggleDebug)
                        .then(Commands.literal("toggle")
                                .executes(DebugCommand::toggleDebug)
                        )
                        .then(Commands.literal("scheduler-status")
                                .executes(DebugCommand::schedulerStatus)
                        )
                )
        );
    }

    private static int toggleDebug(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        boolean isEnabled = !player.getPersistentData().getBoolean("spaceregion_debug");
        player.getPersistentData().putBoolean("spaceregion_debug", isEnabled);
        player.connection.send(new com.sanhiruzu.atelier.network.ToggleDebugPayload(isEnabled));

        player.displayClientMessage(
                Component.literal(isEnabled
                        ? "§aAtelier Debug: ON§r"
                        : "§cAtelier Debug: OFF§r"),
                true
        );

        return 1;
    }

    private static int schedulerStatus(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        var scheduler = ClassificationTickHandler.getScheduler(player.serverLevel());
        int queueSize = scheduler.getQueueSize();
        int deferredCount = scheduler.getDeferredCount();

        var status = Component.literal("Zone Scheduler Status:\n")
                .append(Component.literal("§eQueued chunks: §f" + queueSize + "\n"))
                .append(Component.literal("§eDeferred chunks: §f" + deferredCount + "\n"));

        if (queueSize > 50) {
            status.append(Component.literal("§c⚠ Critical backlog detected!"));
        } else if (queueSize > 20) {
            status.append(Component.literal("§d⚠ Backlog detected - processing paused"));
        }

        player.displayClientMessage(status, false);
        return 1;
    }
}
