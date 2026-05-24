package com.sanhiruzu.atelier.mixin;

import com.sanhiruzu.atelier.zone.bonus.AtelierBrewingBonus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityMixin {
    @Shadow
    int brewTime;

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void zenAtelier$accelerateAtelierBrewing(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity, CallbackInfo ci) {
        BrewingStandBlockEntityMixin self = (BrewingStandBlockEntityMixin) (Object) blockEntity;
        if (self.brewTime <= 1) {
            return;
        }
        int extraTicks = AtelierBrewingBonus.extraCountdownTicks(level, pos);
        if (extraTicks > 0) {
            self.brewTime = Math.max(1, self.brewTime - extraTicks);
        }
    }
}
