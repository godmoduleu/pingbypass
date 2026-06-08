package eu.client.mixins;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.handler.NetworkStateTransitionHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents terminal packets from triggering pipeline state transitions
 * on CLIENT-SIDE connections during PingBypass proxy connections.
 * Our transitionInbound/transitionOutbound mixins handle pipeline swaps
 * directly, so onEncoded/onDecoded are redundant on the client side.
 */
@Mixin(NetworkStateTransitionHandler.class)
public interface NetworkStateTransitionHandlerMixin {

    @Inject(method = "onEncoded", at = @At("HEAD"), cancellable = true)
    private static void onEncoded(ChannelHandlerContext context, Packet<?> packet, CallbackInfo info) {
        if (!eu.client.pingbypass.PingBypassFlags.suppressEncoderErrors) return;
        // Only suppress on client-side connections
        if (context.pipeline().get("packet_handler") instanceof ClientConnection cc
                && cc.getSide() == NetworkSide.CLIENTBOUND) {
            info.cancel();
        }
    }

    @Inject(method = "onDecoded", at = @At("HEAD"), cancellable = true)
    private static void onDecoded(ChannelHandlerContext context, Packet<?> packet, CallbackInfo info) {
        if (!eu.client.pingbypass.PingBypassFlags.suppressEncoderErrors) return;
        // Only suppress on client-side connections
        if (context.pipeline().get("packet_handler") instanceof ClientConnection cc
                && cc.getSide() == NetworkSide.CLIENTBOUND) {
            info.cancel();
        }
    }
}
