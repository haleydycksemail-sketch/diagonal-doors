package com.haley.diagonaldoors.mixin;

import com.haley.diagonaldoors.DiagonalDoors;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
abstract class BlockItemPlacementMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void diagonaldoors$sneakPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) return;
        BlockItem diagonal = DiagonalDoors.diagonalItemFor(((BlockItem)(Object)this).getBlock());
        if (diagonal != null) {
            // Keep the original context and held stack: vanilla placement handles permissions,
            // obstruction, both halves, sounds, and consuming exactly one vanilla door.
            // Diagonal items have no vanilla mapping, so this call does not recurse.
            callback.setReturnValue(diagonal.place(context));
        }
    }
}
