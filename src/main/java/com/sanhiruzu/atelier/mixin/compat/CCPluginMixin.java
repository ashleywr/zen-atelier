package com.sanhiruzu.atelier.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

// C&C's CCPlugin.getRepairData streams all JEI item stacks, filters by whether the item is
// an ArmorItem whose material holder has a "caverns_and_chasms" namespace. Some mods register
// armor materials as inline holders (no registry key), making Holder.getKey() return null and
// causing a NPE that crashes C&C's entire JEI plugin. Fix: return false (not a C&C material)
// when the key is absent.
@Mixin(targets = "com.teamabnormals.caverns_and_chasms.integration.jei.CCPlugin", remap = false)
public class CCPluginMixin {

    @WrapMethod(method = "lambda$getRepairData$0")
    private static boolean zenAtelier$safeRepairDataFilter(ItemStack stack, Operation<Boolean> original) {
        try {
            return original.call(stack);
        } catch (NullPointerException e) {
            return false;
        }
    }
}
