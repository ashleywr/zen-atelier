package com.sanhiruzu.atelier.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class AtelierKeys {
    public static final KeyMapping SHOW_ZONE_BOUNDS = new KeyMapping(
            "key.zen_atelier.show_zone_bounds",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.zen_atelier"
    );

    private AtelierKeys() {
    }
}
