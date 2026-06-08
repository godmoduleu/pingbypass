package eu.client.modules.impl.movement;

import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketSendEvent;
import eu.client.mixins.accessors.PlayerMoveC2SPacketAccessor;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.utils.minecraft.NetworkUtils;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@RegisterModule(name = "Disabler", description = "Disables Grim.", category = Module.Category.MOVEMENT)
public class DisablerModule extends Module {
    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacket() instanceof PlayerMoveC2SPacket packet) {
            if (packet.changesLook()) {
                if (packet.changesPosition()) NetworkUtils.sendIgnoredPacket(new PlayerMoveC2SPacket.Full(packet.getX(mc.player.getX()), packet.getY(mc.player.getY()), packet.getZ(mc.player.getZ()), packet.getYaw(mc.player.getYaw()) + 11813401, packet.getPitch(mc.player.getPitch()), packet.isOnGround(), packet.horizontalCollision()));
                else NetworkUtils.sendIgnoredPacket(new PlayerMoveC2SPacket.LookAndOnGround(packet.getYaw(mc.player.getYaw()) + 11813401, packet.getPitch(mc.player.getPitch()), packet.isOnGround(), packet.horizontalCollision()));

                ((PlayerMoveC2SPacketAccessor) packet).setYaw(packet.getYaw(mc.player.getYaw()) + 11813400);
            } else {
                if (packet.changesPosition()) {
                    NetworkUtils.sendIgnoredPacket(new PlayerMoveC2SPacket.Full(packet.getX(mc.player.getX()), packet.getY(mc.player.getY()), packet.getZ(mc.player.getZ()), packet.getYaw(mc.player.getYaw()) + 11813401, packet.getPitch(mc.player.getPitch()), packet.isOnGround(), packet.horizontalCollision()));
                    NetworkUtils.sendIgnoredPacket(new PlayerMoveC2SPacket.Full(packet.getX(mc.player.getX()), packet.getY(mc.player.getY()), packet.getZ(mc.player.getZ()), packet.getYaw(mc.player.getYaw()) + 11813400, packet.getPitch(mc.player.getPitch()), packet.isOnGround(), packet.horizontalCollision()));
                    event.setCancelled(true);
                } else {
                    NetworkUtils.sendIgnoredPacket(new PlayerMoveC2SPacket.LookAndOnGround(packet.getYaw(mc.player.getYaw()) + 11813401, packet.getPitch(mc.player.getPitch()), packet.isOnGround(), packet.horizontalCollision()));
                    NetworkUtils.sendIgnoredPacket(new PlayerMoveC2SPacket.LookAndOnGround(packet.getYaw(mc.player.getYaw()) + 11813400, packet.getPitch(mc.player.getPitch()), packet.isOnGround(), packet.horizontalCollision()));
                    event.setCancelled(true);
                }
            }
        }
    }
}
