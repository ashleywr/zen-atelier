package com.sanhiruzu.atelier.mixin.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

// Frostiful registers NeoHeartRenderEvent.Pre (a FORGE bus event) on the MOD bus, causing
// FrostifulClientMod instantiation to fail and breaking all entity model layer registrations.
// Redirect the listener to NeoForge.EVENT_BUS where it belongs.
@Mixin(targets = "com.github.thedeathlycow.frostiful.client.compat.ColorfulHeartsIntegration", remap = false)
public class ColorfulHeartsIntegrationMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(
        method = "initialize",
        at = @At(value = "INVOKE", target = "Lnet/neoforged/bus/api/IEventBus;addListener(Ljava/util/function/Consumer;)V"),
        require = 0
    )
    private static void zenAtelier$fixColorfulHeartsEventBus(IEventBus wrongBus, Consumer listener) {
        NeoForge.EVENT_BUS.addListener(listener);
    }
}
