package com.haley.diagonaldoors.client;

import com.haley.diagonaldoors.AngledDoorBlock;
import com.haley.diagonaldoors.DiagonalDoors;
import com.haley.diagonaldoors.DoorGeometry;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Opt-in development check runs against Minecraft's actual baked models and vanilla textures. */
@EventBusSubscriber(modid = DiagonalDoors.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DevelopmentModelChecks {
    @SubscribeEvent
    public static void modelsBaked(ModelEvent.BakingCompleted event) {
        if (!Boolean.getBoolean("diagonaldoors.validateModels")) return;
        int count = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof AngledDoorBlock)) continue;
            for (var state : block.getStateDefinition().getPossibleStates()) {
                var model = event.getModels().get(BlockModelShaper.stateToModelLocation(state));
                if (model == null || model == event.getModelManager().getMissingModel()) throw new IllegalStateException("Missing door model: "+state);
                var quads = model.getQuads(state,null,RandomSource.create(0));
                if (quads.size() != 14) throw new IllegalStateException("Expected fourteen panel faces: "+state+", got "+quads.size());
                double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
                int coreFaces=0;
                var panel=DoorGeometry.panel(AngledDoorBlock.turns(state.getValue(DoorBlock.FACING)),
                        state.getValue(DoorBlock.HINGE)==DoorHingeSide.RIGHT,state.getValue(DoorBlock.OPEN));
                for (var quad : quads) {
                    if (quad.getSprite().contents().name().getPath().equals("missingno")) throw new IllegalStateException("Missing texture: "+state);
                    int[] vertices = quad.getVertices();
                    int stride = vertices.length/4;
                    double minY=1,maxY=0,minU=Double.POSITIVE_INFINITY,maxU=Double.NEGATIVE_INFINITY;
                    for (int i=0;i<4;i++) {
                        double y=Float.intBitsToFloat(vertices[i*stride+1]),u=Float.intBitsToFloat(vertices[i*stride+4]);
                        minY=Math.min(minY,y);maxY=Math.max(maxY,y);minU=Math.min(minU,u);maxU=Math.max(maxU,u);
                    }
                    if (maxY-minY>0.99 && maxU-minU>(quad.getSprite().getU1()-quad.getSprite().getU0())*0.9) {
                        double lowX=0,lowZ=0,highX=0,highZ=0;
                        for (int i=0;i<4;i++) {
                            double x=Float.intBitsToFloat(vertices[i*stride]),z=Float.intBitsToFloat(vertices[i*stride+2]);
                            if (Float.intBitsToFloat(vertices[i*stride+4])<(minU+maxU)/2) { lowX+=x/2;lowZ+=z/2; }
                            else { highX+=x/2;highZ+=z/2; }
                        }
                        double width=Math.hypot(lowX-highX,lowZ-highZ)*16;
                        if (Math.abs(width-16)>1e-4) throw new IllegalStateException("Vanilla panel texture was stretched: "+state);
                        double tipDirection=(lowX-highX)*(panel.tip().x()-panel.hinge().x())+(lowZ-highZ)*(panel.tip().z()-panel.hinge().z());
                        if (tipDirection<=0) throw new IllegalStateException("Handle edge points toward hinge: "+state);
                        coreFaces++;
                    }
                    for (int i=0;i<4;i++) {
                        float x=Float.intBitsToFloat(vertices[i*stride]),z=Float.intBitsToFloat(vertices[i*stride+2]);
                        minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
                    }
                }
                if (coreFaces!=2) throw new IllegalStateException("Missing full-width vanilla front/back: "+state);
                var boxes=DoorGeometry.panel(AngledDoorBlock.turns(state.getValue(DoorBlock.FACING)),
                        state.getValue(DoorBlock.HINGE)==DoorHingeSide.RIGHT,state.getValue(DoorBlock.OPEN)).boxes();
                double expectedMinX=boxes.stream().mapToDouble(DoorGeometry.Box::minX).min().orElseThrow()/16;
                double expectedMaxX=boxes.stream().mapToDouble(DoorGeometry.Box::maxX).max().orElseThrow()/16;
                double expectedMinZ=boxes.stream().mapToDouble(DoorGeometry.Box::minZ).min().orElseThrow()/16;
                double expectedMaxZ=boxes.stream().mapToDouble(DoorGeometry.Box::maxZ).max().orElseThrow()/16;
                if (Math.abs(minX-expectedMinX)>1e-5 || Math.abs(maxX-expectedMaxX)>1e-5
                        || Math.abs(minZ-expectedMinZ)>1e-5 || Math.abs(maxZ-expectedMaxZ)>1e-5)
                    throw new IllegalStateException("Baked geometry disagrees with collision: "+state);
                count++;
            }
        }
        LogUtils.getLogger().info("DIAGONAL_DOORS_MODEL_CHECK_PASSED: {} states, 14 faces each, vanilla texture scale and handle orientation verified, collision bounds aligned",count);
        if (Boolean.getBoolean("diagonaldoors.exitAfterModelChecks")) net.minecraft.client.Minecraft.getInstance().stop();
    }
}
