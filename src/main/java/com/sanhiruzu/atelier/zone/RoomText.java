package com.sanhiruzu.atelier.zone;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class RoomText {
    private RoomText() {
    }

    public static String roomTypeName(ResourceLocation id) {
        String key = "room_type." + id.getNamespace() + "." + id.getPath();
        Component component = Component.translatable(key);
        String localized = component.getString();
        if (!localized.equals(key)) return localized;

        String path = id.getPath().replace('_', ' ');
        return path.isEmpty() ? id.toString() : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }
}
