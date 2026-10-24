package com.haley.diagonaldoors.mixin;

import com.haley.diagonaldoors.DoorPicking;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class EntityPickMixin {
    @Inject(method = "pick", at = @At("RETURN"), cancellable = true)
    private void diagonaldoors$pickExtendedLeaf(double reach, float partialTick, boolean fluids,
                                                CallbackInfoReturnable<HitResult> callback) {
        callback.setReturnValue(DoorPicking.extend((Entity)(Object)this, reach, partialTick, callback.getReturnValue()));
    }
}
