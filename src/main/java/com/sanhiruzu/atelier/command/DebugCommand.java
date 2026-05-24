package com.sanhiruzu.atelier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DebugCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("zen-atelier")
                .then(Commands.literal("debug")
                        .executes(DebugCommand::toggleDebug)
                        .then(Commands.literal("toggle")
                                .executes(DebugCommand::toggleDebug)
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
}
