package com.haley.diagonaldoors;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Vanilla DDA visits the ray's cells, but an open leaf also occupies a neighboring cell. */
public final class DoorPicking {
    private DoorPicking() {}
    public static HitResult extend(Entity entity, double reach, float partialTick, HitResult original) {
        Vec3 from = entity.getEyePosition(partialTick);
        Vec3 to = from.add(entity.getViewVector(partialTick).scale(reach));
        return extend(entity, from, to, original);
    }
    public static HitResult pick(Entity entity, Vec3 from, Vec3 to) {
        var original=entity.level().clip(new ClipContext(from,to,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,entity));
        return extend(entity,from,to,original);
    }
    private static HitResult extend(Entity entity, Vec3 from, Vec3 to, HitResult original) {
        HitResult[] nearest = {original};
        double[] distance = {original.getType() == HitResult.Type.MISS ? from.distanceToSqr(to) : from.distanceToSqr(original.getLocation())};
        Set<BlockPos> checked = new HashSet<>();
        BlockGetter.traverseBlocks(from, to, entity, (camera, cell) -> {
            for (BlockPos candidate : BlockPos.betweenClosed(cell.offset(-1,-1,-1),cell.offset(1,1,1))) {
                BlockPos pos = candidate.immutable();
                if (!checked.add(pos)) continue;
                var state = camera.level().getBlockState(pos);
                if (!(state.getBlock() instanceof AngledDoorBlock)) continue;
                BlockHitResult hit = state.getShape(camera.level(),pos,CollisionContext.of(camera)).clip(from,to,pos);
                if (hit != null && from.distanceToSqr(hit.getLocation()) < distance[0]) {
                    nearest[0] = hit;
                    distance[0] = from.distanceToSqr(hit.getLocation());
                }
            }
            return null;
        }, camera -> null);
        return nearest[0];
    }
}
