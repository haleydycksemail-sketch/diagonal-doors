package com.haley.diagonaldoors.mixin;

import com.haley.diagonaldoors.DoorInteraction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerDoorInteractionMixin {
    @Shadow public ServerPlayer player;

    @ModifyConstant(method="handleUseItemOn", constant=@Constant(doubleValue=1.0000001))
    private double diagonaldoors$allowVerifiedLeafHit(double original, ServerboundUseItemOnPacket packet) {
        // Only a point on a reachable, unoccluded diagonal leaf gets the larger coordinate bound.
        // The surrounding vanilla reach, permissions, border, spectator and item checks still run.
        return DoorInteraction.isExtendedLeafHit(player,packet.getHitResult()) ? 2.0000001 : original;
    }
}
