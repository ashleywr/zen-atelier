package com.sanhiruzu.atelier.mixin.compat;

import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ThreadLocalRandom;

// Spawn mod calls Level.random (bound to server thread by C2ME's CheckedThreadLocalRandom)
// from C2ME worker threads during chunk generation. Redirect to ThreadLocalRandom.current()
// which is genuinely thread-local and safe from any thread.
@Mixin(targets = "com.ninni.spawn.server.data.AnimalVariantManager", remap = false)
public abstract class SpawnAnimalVariantManagerMixin {
    @Redirect(
            method = "chooseWeightedVariant",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", remap = false)
    )
    private static int zenAtelier$fixC2meThreadLocalRandom(RandomSource random, int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }
}
