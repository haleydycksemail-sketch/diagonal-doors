package com.haley.diagonaldoors;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Validates extended-leaf clicks without relaxing Minecraft's normal block checks. */
public final class DoorInteraction {
    private DoorInteraction() {}
    public static boolean isExtendedLeafHit(ServerPlayer player, BlockHitResult hit) {
        var pos=hit.getBlockPos();
        var state=player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof AngledDoorBlock)) return false;
        var point=hit.getLocation();
        var local=point.subtract(pos.getX(),pos.getY(),pos.getZ());
        if (state.getShape(player.level(),pos).toAabbs().stream().noneMatch(b -> b.inflate(1e-4).contains(local))) return false;
        var eye=player.getEyePosition();
        if (eye.distanceToSqr(point)>Math.pow(player.blockInteractionRange()+1,2)) return false;
        // Recast toward the claimed point: nearer blocks and other leaves must still occlude it.
        var beyond=point.add(point.subtract(eye).normalize().scale(0.02));
        var actual=DoorPicking.pick(player,eye,beyond);
        return actual instanceof BlockHitResult b && b.getType()==HitResult.Type.BLOCK
                && b.getBlockPos().equals(pos);
    }
}
