package com.haley.diagonaldoors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A diagonal leaf with a fixed hinge; vanilla DoorBlock owns vertical halves and redstone. */
public final class AngledDoorBlock extends DoorBlock {
    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    static {
        for (int turns = 0; turns < 4; turns++) for (int right = 0; right < 2; right++) for (int open = 0; open < 2; open++) {
            VoxelShape shape = Shapes.empty();
            for (var b : DoorGeometry.panel(turns, right == 1, open == 1).boxes()) {
                shape = Shapes.or(shape, box(b.minX(), 0, b.minZ(), b.maxX(), 16, b.maxZ()));
            }
            SHAPES[turns * 4 + right * 2 + open] = shape.optimize();
        }
    }
    public AngledDoorBlock(BlockSetType setType, Properties properties) { super(setType, properties); }
    public static int turns(Direction facing) {
        return switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
    }
    private static BlockPos along(BlockPos pos, Direction facing, int sign) {
        int x = 1, z = -1;
        for (int i = 0; i < turns(facing); i++) { int previousX = x; x = -z; z = previousX; }
        return pos.offset(sign * x, 0, sign * z);
    }
    private static boolean right(BlockState state) { return state.getValue(HINGE) == DoorHingeSide.RIGHT; }
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[turns(state.getValue(FACING)) * 4 + (right(state) ? 2 : 0) + (state.getValue(OPEN) ? 1 : 0)];
    }
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) return state;
        int index = Math.floorMod((mirror == Mirror.FRONT_BACK ? 1 : 3) - turns(state.getValue(FACING)), 4);
        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        return state.setValue(FACING, directions[index]).cycle(HINGE);
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        BlockPos pos = context.getClickedPos();
        Direction facing = state.getValue(FACING);
        // Two diagonally adjacent cells form a continuous doorway. The hinges belong at its outer ends.
        for (int sign : new int[]{1, -1}) {
            BlockState neighbor = context.getLevel().getBlockState(along(pos, facing, sign));
            BlockPos neighborPos=along(pos,facing,sign);
            BlockPos existingMate=aligned(state,neighbor) ? mate(context.getLevel(),neighborPos,neighbor) : null;
            if (aligned(state,neighbor) && neighbor.getValue(HALF)==DoubleBlockHalf.LOWER
                    && (existingMate==null || existingMate.equals(pos))) {
                return state.setValue(HINGE, sign == 1 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT)
                        .setValue(OPEN, neighbor.getValue(OPEN) || state.getValue(POWERED))
                        .setValue(POWERED, neighbor.getValue(POWERED) || state.getValue(POWERED));
            }
        }
        // Choose the closest endpoint of the diagonal panel rather than vanilla's cardinal edge.
        double x = context.getClickLocation().x - pos.getX() - 0.5;
        double z = context.getClickLocation().z - pos.getZ() - 0.5;
        for (int i = 0; i < turns(facing); i++) { double oldX = x; x = z; z = -oldX; }
        return state.setValue(HINGE, x - z > 0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
    }
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockPos otherPos=along(pos,state.getValue(FACING),right(state) ? -1 : 1);
        BlockState other=level.getBlockState(otherPos);
        if (aligned(state,other) && other.getValue(HALF)==DoubleBlockHalf.LOWER) {
            BlockPos previousMate=mate(level,otherPos,other);
            if (previousMate==null || previousMate.equals(pos)) {
                DoorHingeSide outerHinge=along(otherPos,other.getValue(FACING),1).equals(pos) ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
                level.setBlock(otherPos,other.setValue(HINGE,outerHinge),Block.UPDATE_CLIENTS);
            }
        }
        neighborChanged(level.getBlockState(pos), level, pos, net.minecraft.world.level.block.Blocks.AIR, pos, false);
    }
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        super.onRemove(state, level, pos, replacement, moving);
        if (!level.isClientSide && !replacement.is(this) && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            BlockPos matePos = mate(level, pos, state);
            if (matePos != null) neighborChanged(level.getBlockState(matePos), level, matePos,
                    net.minecraft.world.level.block.Blocks.AIR, pos, false);
        }
    }
    private boolean aligned(BlockState state, BlockState other) {
        return other.is(this) && Math.floorMod(turns(state.getValue(FACING))-turns(other.getValue(FACING)),2)==0;
    }
    private BlockPos mate(Level level, BlockPos lower, BlockState state) {
        BlockPos candidate=along(lower,state.getValue(FACING),right(state) ? -1 : 1);
        BlockState other=level.getBlockState(candidate);
        return aligned(state,other) && other.getValue(HALF)==DoubleBlockHalf.LOWER
                && along(candidate,other.getValue(FACING),right(other) ? -1 : 1).equals(lower) ? candidate : null;
    }
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos sourcePos, boolean moving) {
        if (level.isClientSide || source == this) return;
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockState current = level.getBlockState(lower);
        if (!current.is(this)) return;
        BlockPos matePos = mate(level, lower, current);
        boolean powered = level.hasNeighborSignal(lower) || level.hasNeighborSignal(lower.above())
                || (matePos != null && (level.hasNeighborSignal(matePos) || level.hasNeighborSignal(matePos.above())));
        applyPower(level, lower, powered);
        if (matePos != null) applyPower(level, matePos, powered);
    }
    private void applyPower(Level level, BlockPos lower, boolean powered) {
        BlockState current = level.getBlockState(lower);
        if (current.getValue(POWERED) == powered) return;
        setOpen(null, level, current, lower, powered);
        // Let vanilla's vertical updateShape synchronize the other half. Writing a stale upper
        // state here races queued neighbor updates and can revert OPEN on both halves.
        level.setBlock(lower, current.setValue(POWERED, powered).setValue(OPEN, powered), Block.UPDATE_CLIENTS);
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        InteractionResult result = super.useWithoutItem(state, level, pos, player, hit);
        if (result.consumesAction()) {
            BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            BlockState current = level.getBlockState(lower);
            BlockPos matePos = mate(level, lower, current);
            if (matePos != null) setOpen(player, level, level.getBlockState(matePos), matePos, current.getValue(OPEN));
        }
        return result;
    }
}
