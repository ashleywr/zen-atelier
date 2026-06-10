package com.sanhiruzu.atelier.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

// JER (Just Enough Resources) assumes every entity it scans is a LivingEntity and casts
// unconditionally in MobTableBuilder. Frostiful's FreezingWindEntity is a non-living
// special-effect entity, so the cast throws ClassCastException and JEI logs a broken-recipe
// ERROR. Fix: if the cast would fail, report "no spawn egg" so JEI skips the entry cleanly.
@Mixin(targets = "jeresources.entry.MobEntry", remap = false)
public class JERMobEntryMixin {

    @WrapMethod(method = "hasSpawnEgg")
    private boolean zenAtelier$safeHasSpawnEgg(Operation<Boolean> original) {
        try {
            return original.call();
        } catch (ClassCastException e) {
            return false;
        }
    }
}
