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
                if (quads.size() != 6) throw new IllegalStateException("Expected six panel faces: "+state+", got "+quads.size());
                double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
                for (var quad : quads) {
                    if (quad.getSprite().contents().name().getPath().equals("missingno")) throw new IllegalStateException("Missing texture: "+state);
                    int[] vertices = quad.getVertices();
                    int stride = vertices.length/4;
                    for (int i=0;i<4;i++) {
                        float x=Float.intBitsToFloat(vertices[i*stride]),z=Float.intBitsToFloat(vertices[i*stride+2]);
                        minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
                    }
                }
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
        LogUtils.getLogger().info("DIAGONAL_DOORS_MODEL_CHECK_PASSED: {} states, six faces each, textures present, collision bounds aligned",count);
        if (Boolean.getBoolean("diagonaldoors.exitAfterModelChecks")) net.minecraft.client.Minecraft.getInstance().stop();
    }
}
