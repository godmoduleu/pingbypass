package eu.client.mixins;

import io.netty.channel.ChannelHandlerContext;
import eu.client.Pingbypass;
import eu.client.events.impl.ClientDisconnectEvent;
import eu.client.events.impl.PacketReceiveEvent;
import eu.client.events.impl.PacketSendEvent;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("HEAD"), cancellable = true)
    private void send$HEAD(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo info) {
        PacketSendEvent event = new PacketSendEvent(packet);
        Pingbypass.EVENT_HANDLER.post(event);
        if (event.isCancelled())
            info.cancel();
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("TAIL"), cancellable = true)
    private void send$TAIL(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo info) {
        Pingbypass.EVENT_HANDLER.post(new PacketSendEvent.Post(packet));
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo info) {
        PacketReceiveEvent event = new PacketReceiveEvent(packet, (ClientConnection) (Object) this);
        Pingbypass.EVENT_HANDLER.post(event);
        if (packet instanceof BundleS2CPacket bundleS2CPacket) {
            for (Packet<?> subPacket : bundleS2CPacket.getPackets()) {
                Pingbypass.EVENT_HANDLER.post(new PacketReceiveEvent(subPacket, (ClientConnection) (Object) this));
            }
        }
        if (event.isCancelled())
            info.cancel();
    }

    @Inject(method = "disconnect(Lnet/minecraft/network/DisconnectionInfo;)V", at = @At("HEAD"))
    private void disconnect(DisconnectionInfo disconnectionInfo, CallbackInfo info) {
        Pingbypass.EVENT_HANDLER.post(new ClientDisconnectEvent());
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void exceptionCaught(ChannelHandlerContext context, Throwable ex, CallbackInfo info) {
    }
}
