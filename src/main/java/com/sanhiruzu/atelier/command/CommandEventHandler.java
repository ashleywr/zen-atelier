package com.sanhiruzu.atelier.command;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class CommandEventHandler {
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        DebugCommand.register(event.getDispatcher());
        QueryBlockCommand.register(event.getDispatcher());
        CaptureZoneCommand.register(event.getDispatcher());
        DeleteZoneCommand.register(event.getDispatcher());
        InspectZoneCommand.register(event.getDispatcher());
    }
}
