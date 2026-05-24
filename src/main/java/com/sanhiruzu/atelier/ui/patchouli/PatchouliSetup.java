package com.sanhiruzu.atelier.ui.patchouli;

import com.sanhiruzu.atelier.ZenAtelier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = ZenAtelier.MODID, value = Dist.CLIENT)
public final class PatchouliSetup {
    private PatchouliSetup() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(PatchouliSetup::registerCustomComponents);
    }

    private static void registerCustomComponents() {
        if (!ModList.get().isLoaded("patchouli")) {
            return;
        }

        try {
            Class<?> patchouliApi = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = patchouliApi.getMethod("get").invoke(null);
            Class<?> apiInterface = Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI");
            java.lang.reflect.Method registerComponent = apiInterface.getMethod("registerComponent", String.class, Class.class);
            Class<?> componentClass = Class.forName("com.sanhiruzu.atelier.ui.patchouli.RoomDiscoveryPageComponent");

            registerComponent.invoke(api, ZenAtelier.MODID + ":room_discoveries", componentClass);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }
}
