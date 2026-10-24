package com.haley.diagonaldoors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Server-side regression tests, activated by the development GameTest run only. */
@GameTestHolder(DiagonalDoors.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorGameTests {
    private static final String[] WOODS = "oak spruce birch jungle acacia dark_oak mangrove cherry bamboo crimson warped".split(" ");
    private static AngledDoorBlock door(String wood) {
        return (AngledDoorBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("diagonaldoors", "diagonal_"+wood+"_door"));
    }
    private static BlockState state(AngledDoorBlock block, Direction facing, DoorHingeSide hinge) {
        return block.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, hinge);
    }
    private static void put(GameTestHelper h, BlockPos p, BlockState s) {
        h.setBlock(p.below(), Blocks.STONE);
        h.setBlock(p, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        h.setBlock(p.above(), s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }
    @GameTest(template = "empty")
    public static void allWoodsHaveBothHalvesAndOpenCollision(GameTestHelper h) {
        BlockPos p = new BlockPos(5,1,5);
        for (String wood : WOODS) for (Direction facing : Direction.Plane.HORIZONTAL) for (DoorHingeSide hinge : DoorHingeSide.values()) {
            var block = door(wood);
            put(h,p,state(block,facing,hinge));
            var level = h.getLevel();
            BlockPos abs = h.absolutePos(p);
            var closed = level.getBlockState(abs);
            var oldShape = closed.getCollisionShape(level,abs);
            block.setOpen(null,level,closed,abs,true);
            var opened = level.getBlockState(abs);
            h.assertTrue(opened.getValue(DoorBlock.OPEN), "Lower half did not open: "+wood);
            h.assertTrue(level.getBlockState(abs.above()).getValue(DoorBlock.OPEN), "Upper half did not open");
            h.assertTrue(!oldShape.equals(opened.getCollisionShape(level,abs)), "Collision did not move");
            h.assertTrue(opened.hasLargeCollisionShape(), "Extended leaf collision must be queried from adjacent cells");
            block.setOpen(null,level,opened,abs,false);
            h.assertTrue(!level.getBlockState(abs.above()).getValue(DoorBlock.OPEN), "Upper half did not close");
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void itemsPlaceAndPairDiagonally(GameTestHelper h) {
        var level = h.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        player.setYRot(180); // NORTH
        player.getAbilities().instabuild = true;
        var block = door("cherry");
        BlockPos a = new BlockPos(4,1,5), b = a.offset(1,0,-1);
        for (BlockPos p : new BlockPos[]{a,b}) {
            h.setBlock(p.below(),Blocks.STONE);
            BlockPos abs = h.absolutePos(p);
            ItemStack stack = new ItemStack(block.asItem());
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            // First click near the southwest hinge. The second door must pair with it automatically.
            var hit = new BlockHitResult(new Vec3(abs.getX()+0.1,abs.getY(),abs.getZ()+0.9), Direction.UP,abs.below(),false);
            var ctx = new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,stack,hit);
            h.assertTrue(((BlockItem)stack.getItem()).place(ctx).consumesAction(), "Door item placement failed");
            h.assertTrue(level.getBlockState(abs.above()).is(block), "Item did not place upper half");
        }
        h.assertTrue(h.getBlockState(a).getValue(DoorBlock.HINGE)==DoorHingeSide.LEFT, "First hinge incorrect");
        h.assertTrue(h.getBlockState(b).getValue(DoorBlock.HINGE)==DoorHingeSide.RIGHT, "Diagonal mate did not select outer hinge");
        BlockPos abs = h.absolutePos(a);
        var hit = new BlockHitResult(Vec3.atCenterOf(abs.above()),Direction.UP,abs.above(),false);
        block.useWithoutItem(level.getBlockState(abs.above()),level,abs.above(),player,hit);
        h.assertTrue(h.getBlockState(a).getValue(DoorBlock.OPEN) && h.getBlockState(b).getValue(DoorBlock.OPEN), "Click did not open both doors");
        block.useWithoutItem(level.getBlockState(abs),level,abs,player,hit);
        h.assertTrue(!h.getBlockState(a).getValue(DoorBlock.OPEN) && !h.getBlockState(b).getValue(DoorBlock.OPEN), "Click did not close both doors");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void eitherHalfOfPairReceivesRedstone(GameTestHelper h) {
        var block = door("oak");
        BlockPos a = new BlockPos(4,1,5), b = a.offset(1,0,-1);
        put(h,a,state(block,Direction.NORTH,DoorHingeSide.LEFT));
        put(h,b,state(block,Direction.NORTH,DoorHingeSide.RIGHT));
        h.setBlock(a.west(),Blocks.REDSTONE_BLOCK);
        h.assertTrue(h.getBlockState(a).getValue(DoorBlock.OPEN) && h.getBlockState(b).getValue(DoorBlock.OPEN), "Pair did not open under power: a="+h.getBlockState(a)+", b="+h.getBlockState(b)+", signal="+h.getLevel().hasNeighborSignal(h.absolutePos(a)));
        h.setBlock(b.east().above(),Blocks.REDSTONE_BLOCK);
        h.setBlock(a.west(),Blocks.AIR);
        h.assertTrue(h.getBlockState(a).getValue(DoorBlock.OPEN), "Pair closed while mate remained powered");
        h.setBlock(b.east().above(),Blocks.AIR);
        h.assertTrue(!h.getBlockState(a).getValue(DoorBlock.OPEN) && !h.getBlockState(b).getValue(DoorBlock.OPEN), "Pair did not close when power removed");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void removingSupportRemovesBothHalves(GameTestHelper h) {
        BlockPos p = new BlockPos(5,1,5);
        put(h,p,state(door("oak"),Direction.NORTH,DoorHingeSide.LEFT));
        h.setBlock(p.below(),Blocks.AIR);
        h.assertTrue(h.getBlockState(p).isAir() && h.getBlockState(p.above()).isAir(), "Unsupported door left an orphan half");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void extendedOpenLeafCanBeClickedOutsideItsCell(GameTestHelper h) {
        var block = door("oak");
        BlockPos p = new BlockPos(5,1,5);
        put(h,p,state(block,Direction.NORTH,DoorHingeSide.LEFT).setValue(DoorBlock.OPEN,true));
        BlockPos abs = h.absolutePos(p);
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        player.setPos(abs.getX()+2, abs.getY()+0.5-player.getEyeHeight(), abs.getZ()+1.5);
        player.setYRot(90);
        player.setYHeadRot(90);
        player.setXRot(0);
        var hit = player.pick(3,1,false);
        h.assertTrue(hit instanceof BlockHitResult b && b.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && b.getBlockPos().equals(abs), "Open leaf outside owning cell was not selectable: hit="+hit+", eye="+player.getEyePosition(1)+", view="+player.getViewVector(1)+", state="+h.getBlockState(p));
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void allDoorsDropOnceAndKeepVanillaTags(GameTestHelper h) {
        BlockPos p = h.absolutePos(new BlockPos(5,1,5));
        for (String wood : WOODS) {
            var block = door(wood);
            var lower = state(block,Direction.NORTH,DoorHingeSide.LEFT);
            var upper = lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER);
            var drops = net.minecraft.world.level.block.Block.getDrops(lower,h.getLevel(),p,null);
            h.assertTrue(drops.size()==1 && drops.getFirst().is(block.asItem()) && drops.getFirst().getCount()==1,
                    "Incorrect lower-half loot: "+wood);
            h.assertTrue(net.minecraft.world.level.block.Block.getDrops(upper,h.getLevel(),p,null).isEmpty(), "Upper half duplicated loot: "+wood);
            h.assertTrue(lower.is(net.minecraft.tags.BlockTags.WOODEN_DOORS)
                    && lower.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE), "Missing vanilla door/axe tags: "+wood);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void openLeavesCollideFromAdjacentCellsAndMirrorCorrectly(GameTestHelper h) {
        var block=door("cherry");
        BlockPos p=new BlockPos(5,1,5), abs=h.absolutePos(p);
        for (Direction facing : Direction.Plane.HORIZONTAL) for (DoorHingeSide hinge : DoorHingeSide.values()) {
            var opened=state(block,facing,hinge).setValue(DoorBlock.OPEN,true);
            put(h,p,opened);
            var panel=DoorGeometry.panel(AngledDoorBlock.turns(facing),hinge==DoorHingeSide.RIGHT,true);
            double x=abs.getX()+(panel.hinge().x()*0.2+panel.tip().x()*0.8)/16;
            double z=abs.getZ()+(panel.hinge().z()*0.2+panel.tip().z()*0.8)/16;
            var query=new net.minecraft.world.phys.AABB(x-0.01,abs.getY()+0.4,z-0.01,x+0.01,abs.getY()+0.6,z+0.01);
            h.assertTrue(h.getLevel().getBlockCollisions(null,query).iterator().hasNext(), "Extended leaf had no entity collision: "+opened);
            for (var mirror : new net.minecraft.world.level.block.Mirror[]{net.minecraft.world.level.block.Mirror.FRONT_BACK,net.minecraft.world.level.block.Mirror.LEFT_RIGHT}) {
                var mirrored=opened.mirror(mirror);
                var transformed=DoorGeometry.panel(AngledDoorBlock.turns(mirrored.getValue(DoorBlock.FACING)),mirrored.getValue(DoorBlock.HINGE)==DoorHingeSide.RIGHT,true);
                double expectedX=mirror==net.minecraft.world.level.block.Mirror.FRONT_BACK ? 16-panel.hinge().x() : panel.hinge().x();
                double expectedZ=mirror==net.minecraft.world.level.block.Mirror.LEFT_RIGHT ? 16-panel.hinge().z() : panel.hinge().z();
                h.assertTrue(Math.abs(transformed.hinge().x()-expectedX)<1e-9 && Math.abs(transformed.hinge().z()-expectedZ)<1e-9,"Structure mirror moved the hinge incorrectly");
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removingPoweredMateReleasesRemainingDoor(GameTestHelper h) {
        var block=door("oak");
        BlockPos a=new BlockPos(4,1,5),b=a.offset(1,0,-1);
        put(h,a,state(block,Direction.NORTH,DoorHingeSide.LEFT));
        put(h,b,state(block,Direction.NORTH,DoorHingeSide.RIGHT));
        h.setBlock(b.east(),Blocks.REDSTONE_BLOCK);
        h.assertTrue(h.getBlockState(a).getValue(DoorBlock.POWERED),"Pair did not share power");
        h.setBlock(b,Blocks.AIR);
        h.assertTrue(!h.getBlockState(a).getValue(DoorBlock.OPEN) && !h.getBlockState(a).getValue(DoorBlock.POWERED),
                "Remaining door retained power from removed mate");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sneakingVanillaDoorsPlaceDiagonallyAndConsumeOne(GameTestHelper h) {
        var player=FakePlayerFactory.getMinecraft(h.getLevel());
        player.getAbilities().instabuild=false;
        BlockPos p=new BlockPos(5,1,5),abs=h.absolutePos(p);
        for (String wood : WOODS) for (boolean sneak : new boolean[]{false,true}) {
            h.setBlock(p.above(),Blocks.AIR);
            h.setBlock(p,Blocks.AIR);
            h.setBlock(p.below(),Blocks.STONE);
            var vanilla=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("minecraft",wood+"_door"));
            ItemStack stack=new ItemStack(vanilla.asItem(),3);
            player.setShiftKeyDown(sneak);
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var hit=new BlockHitResult(new Vec3(abs.getX()+0.1,abs.getY(),abs.getZ()+0.9),Direction.UP,abs.below(),false);
            var context=new BlockPlaceContext(h.getLevel(),player,InteractionHand.MAIN_HAND,stack,hit);
            h.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(),"Placement failed: "+wood);
            var expected=sneak ? door(wood) : vanilla;
            h.assertTrue(h.getBlockState(p).is(expected) && h.getBlockState(p.above()).is(expected),"Wrong placed door for sneak="+sneak+": "+wood);
            h.assertTrue(stack.getCount()==2,"Placement did not consume exactly one vanilla door");
        }
        player.setShiftKeyDown(false);
        player.getAbilities().instabuild=true;
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void sneakPlacementHonorsObstructionOffhandAndCreative(GameTestHelper h) {
        var player=FakePlayerFactory.getMinecraft(h.getLevel());
        player.setShiftKeyDown(true);
        BlockPos p=new BlockPos(5,1,5),abs=h.absolutePos(p);
        h.setBlock(p.below(),Blocks.STONE);
        h.setBlock(p.above(),Blocks.STONE);
        ItemStack stack=new ItemStack(Blocks.CHERRY_DOOR.asItem(),2);
        player.getAbilities().instabuild=false;
        player.setItemInHand(InteractionHand.OFF_HAND,stack);
        var hit=new BlockHitResult(new Vec3(abs.getX()+0.1,abs.getY(),abs.getZ()+0.9),Direction.UP,abs.below(),false);
        var context=new BlockPlaceContext(h.getLevel(),player,InteractionHand.OFF_HAND,stack,hit);
        h.assertTrue(!((BlockItem)stack.getItem()).place(context).consumesAction(),"Obstructed placement succeeded");
        h.assertTrue(stack.getCount()==2 && h.getBlockState(p).isAir(),"Failed placement consumed an item or left a half");
        h.setBlock(p.above(),Blocks.AIR);
        h.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(),"Offhand placement failed");
        h.assertTrue(h.getBlockState(p).is(door("cherry")) && stack.getCount()==1,"Offhand placement used wrong block or stack");
        h.setBlock(p.above(),Blocks.AIR);
        h.setBlock(p,Blocks.AIR);
        player.getAbilities().instabuild=true;
        h.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(),"Creative placement failed");
        h.assertTrue(stack.getCount()==1,"Creative placement consumed an item");
        player.setShiftKeyDown(false);
        h.succeed();
    }


    private static net.minecraft.server.network.ServerGamePacketListenerImpl realListener(GameTestHelper h, net.minecraft.server.level.ServerPlayer player) {
        return new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
    }
    private static void sendClick(net.minecraft.server.network.ServerGamePacketListenerImpl listener, BlockHitResult hit) {
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var packet=new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,hit,0);
            net.minecraft.network.protocol.game.ServerboundUseItemOnPacket.STREAM_CODEC.encode(buffer,packet);
            listener.handleUseItemOn(net.minecraft.network.protocol.game.ServerboundUseItemOnPacket.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
    @GameTest(template = "empty")
    public static void serverAcceptsClosingClicksAlongExtendedLeaf(GameTestHelper h) {
        var block=door("oak");
        var player=FakePlayerFactory.getMinecraft(h.getLevel());
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        BlockPos p=new BlockPos(5,1,5),abs=h.absolutePos(p);
        var savedConnection=player.connection;
        var listener=realListener(h,player);
        try {
            for (Direction facing : Direction.Plane.HORIZONTAL) for (DoorHingeSide hinge : DoorHingeSide.values())
                for (boolean upper : new boolean[]{false,true}) for (double t : new double[]{0.2,0.5,0.8,0.95}) {
                    var opened=state(block,facing,hinge).setValue(DoorBlock.OPEN,true);
                    put(h,p,opened);
                    var panel=DoorGeometry.panel(AngledDoorBlock.turns(facing),hinge==DoorHingeSide.RIGHT,true);
                    double x=abs.getX()+(panel.hinge().x()*(1-t)+panel.tip().x()*t)/16;
                    double z=abs.getZ()+(panel.hinge().z()*(1-t)+panel.tip().z()*t)/16;
                    double dx=panel.tip().x()-panel.hinge().x(),dz=panel.tip().z()-panel.hinge().z(),length=Math.hypot(dx,dz);
                    var normal=new Vec3(-dz/length,0,dx/length);
                    var center=new Vec3(x,abs.getY()+(upper ? 1.5 : 0.5),z);
                    var from=center.add(normal.scale(1.5));
                    player.setPos(from.x,from.y-player.getEyeHeight(),from.z);
                    BlockPos target=upper ? abs.above() : abs;
                    var hit=h.getLevel().getBlockState(target).getShape(h.getLevel(),target).clip(from,center.subtract(normal.scale(0.1)),target);
                    h.assertTrue(hit!=null,"Click fixture missed leaf");
                    sendClick(listener,hit);
                    h.assertTrue(!h.getBlockState(p).getValue(DoorBlock.OPEN),"Server rejected closing click: "+facing+"/"+hinge+" t="+t+" upper="+upper);
                }
        } finally { player.connection=savedConnection; }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void extendedClickValidationRejectsOffLeafAndOccludedHits(GameTestHelper h) {
        var block=door("oak");
        var player=FakePlayerFactory.getMinecraft(h.getLevel());
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        BlockPos p=new BlockPos(5,1,5),abs=h.absolutePos(p);
        var opened=state(block,Direction.NORTH,DoorHingeSide.LEFT).setValue(DoorBlock.OPEN,true);
        put(h,p,opened);
        var panel=DoorGeometry.panel(0,false,true);
        double x=abs.getX()+(panel.hinge().x()*0.1+panel.tip().x()*0.9)/16;
        double z=abs.getZ()+(panel.hinge().z()*0.1+panel.tip().z()*0.9)/16;
        var center=new Vec3(x,abs.getY()+0.5,z);
        var from=center.add(-1,0,1);
        player.setPos(from.x,from.y-player.getEyeHeight(),from.z);
        var hit=opened.getShape(h.getLevel(),abs).clip(from,center.add(0.1,0,-0.1),abs);
        h.assertTrue(hit!=null && DoorInteraction.isExtendedLeafHit(player,hit),"Valid fixture rejected");
        var fake=new BlockHitResult(new Vec3(abs.getX()+1.8,abs.getY()+0.5,abs.getZ()+1.8),Direction.UP,abs,false);
        h.assertTrue(!DoorInteraction.isExtendedLeafHit(player,fake),"Off-leaf hit accepted");
        BlockPos obstruction=BlockPos.containing(from.add(center).scale(0.5));
        h.getLevel().setBlock(obstruction,Blocks.STONE.defaultBlockState(),3);
        h.assertTrue(!DoorInteraction.isExtendedLeafHit(player,hit),"Occluded leaf accepted");
        h.getLevel().setBlock(obstruction,Blocks.AIR.defaultBlockState(),3);
        player.setPos(from.x+20,from.y-player.getEyeHeight(),from.z);
        h.assertTrue(!DoorInteraction.isExtendedLeafHit(player,hit),"Out-of-reach leaf accepted");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void pairingCorrectsFirstHingeInBothOrdersAndOppositeFacings(GameTestHelper h) {
        var player=FakePlayerFactory.getMinecraft(h.getLevel());
        var block=door("cherry");
        player.setShiftKeyDown(true);
        player.getAbilities().instabuild=true;
        BlockPos a=new BlockPos(5,1,5);
        for (Direction facing : Direction.Plane.HORIZONTAL) for (boolean reverse : new boolean[]{false,true})
            for (boolean opposite : new boolean[]{false,true}) {
                int dx=1,dz=-1;
                for (int i=0;i<AngledDoorBlock.turns(facing);i++) { int old=dx;dx=-dz;dz=old; }
                BlockPos b=a.offset(dx,0,dz);
                for (BlockPos clear : BlockPos.betweenClosed(a.offset(-1,0,-1),a.offset(1,1,1))) h.setBlock(clear,Blocks.AIR);
                BlockPos first=reverse ? b : a,second=reverse ? a : b;
                // Deliberately start with the wrong (inner) hinge. Placement must correct it.
                put(h,first,state(block,facing,reverse ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT));
                Direction secondFacing=opposite ? facing.getOpposite() : facing;
                player.setYRot(secondFacing.toYRot());
                h.setBlock(second.below(),Blocks.STONE);
                BlockPos absolute=h.absolutePos(second);
                ItemStack stack=new ItemStack(Blocks.CHERRY_DOOR.asItem());
                player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var hit=new BlockHitResult(Vec3.atBottomCenterOf(absolute),Direction.UP,absolute.below(),false);
                var context=new BlockPlaceContext(h.getLevel(),player,InteractionHand.MAIN_HAND,stack,hit);
                h.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(),"Pair placement failed");
                player.setShiftKeyDown(false);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                var firstState=h.getBlockState(first);
                h.assertTrue(firstState.getValue(DoorBlock.HINGE)==(reverse ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT),"First hinge not corrected: facing="+facing+", reverse="+reverse+", opposite="+opposite+", first="+firstState+", second="+h.getBlockState(second));
                block.useWithoutItem(firstState,h.getLevel(),h.absolutePos(first),player,hit);
                h.assertTrue(h.getBlockState(first).getValue(DoorBlock.OPEN) && h.getBlockState(second).getValue(DoorBlock.OPEN),"Pair did not open together");
                block.useWithoutItem(h.getBlockState(second),h.getLevel(),absolute,player,hit);
                h.assertTrue(!h.getBlockState(first).getValue(DoorBlock.OPEN) && !h.getBlockState(second).getValue(DoorBlock.OPEN),"Pair did not close together");
                player.setShiftKeyDown(true);
            }
        player.setShiftKeyDown(false);
        h.succeed();
    }
}
