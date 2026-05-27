package com.sanhiruzu.atelier.ui.patchouli;

import com.sanhiruzu.atelier.ZenAtelier;
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
    }
}
