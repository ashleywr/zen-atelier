package com.sanhiruzu.atelier.ui.patchouli;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = ZenAtelier.MODID, value = Dist.CLIENT)
public final class PatchouliSetup {
    private PatchouliSetup() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(PatchouliSetup::registerComponents);
    }

    private static void registerComponents() {
        try {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = apiClass.getMethod("get").invoke(null);
            Class<?> apiInterface = Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI");
            java.lang.reflect.Method register = apiInterface.getMethod(
                    "registerCustomComponent", ResourceLocation.class, Class.class);
            register.invoke(api,
                    ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "room_discovery_page"),
                    RoomDiscoveryPageComponent.class);
            register.invoke(api,
                    ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "room_stat_card"),
                    RoomStatCardComponent.class);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
