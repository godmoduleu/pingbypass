package eu.client.pingbypass.server;

import io.netty.channel.ChannelPipeline;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.handler.DecoderHandler;
import net.minecraft.network.handler.EncoderHandler;
import net.minecraft.network.handler.PacketBundleHandler;
import net.minecraft.network.handler.PacketBundler;
import net.minecraft.network.handler.PacketUnbundler;
import net.minecraft.network.listener.PacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Connection subclass for the PingBypass proxy server.
 * Overrides transitionOutbound/transitionInbound to perform direct pipeline
 * manipulation instead of writing Lambdas through the pipeline, avoiding
 * conflicts with Fabric API's FabricPacketSplitter.
 */
public class PbSession extends ClientConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbSession.class);

    public PbSession() {
        super(NetworkSide.SERVERBOUND);
    }

    @Override
    public <T extends PacketListener> void transitionInbound(NetworkState<T> state, T listener) {
        if (state.side() != this.getSide()) {
            throw new IllegalStateException("Invalid inbound protocol: " + state.id());
        }

        this.packetListener = listener;
        this.prePlayStateListener = null;

        Runnable swap = () -> {
            try {
                ChannelPipeline p = this.channel.pipeline();
                if (p.get("fabric:merger") != null) p.remove("fabric:merger");
                DecoderHandler<?> dec = new DecoderHandler<>(state);
                if (p.get("decoder") != null) p.replace("decoder", "decoder", dec);
                else if (p.get("inbound_config") != null) p.replace("inbound_config", "decoder", dec);
                this.channel.config().setAutoRead(true);
                PacketBundleHandler bh = state.bundleHandler();
                if (bh != null) {
                    PacketBundler b = new PacketBundler(bh);
                    if (p.get("bundler") != null) p.replace("bundler", "bundler", b);
                    else p.addAfter("decoder", "bundler", b);
                }
            } catch (Exception e) {
                LOGGER.error("[PbSession] Failed inbound transition to {}", state.id(), e);
            }
        };

        if (this.channel.eventLoop().inEventLoop()) swap.run();
        else { try { this.channel.eventLoop().submit(swap).sync(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    }

    @Override
    public void transitionOutbound(NetworkState<?> newState) {
        if (newState.side() != this.getOppositeSide()) {
            throw new IllegalStateException("Invalid outbound protocol: " + newState.id());
        }

        this.duringLogin = newState.id() == NetworkPhase.LOGIN;

        Runnable swap = () -> {
            try {
                ChannelPipeline p = this.channel.pipeline();
                if (p.get("fabric:splitter") != null) p.remove("fabric:splitter");
                EncoderHandler<?> enc = new EncoderHandler<>(newState);
                if (p.get("encoder") != null) p.replace("encoder", "encoder", enc);
                else if (p.get("outbound_config") != null) p.replace("outbound_config", "encoder", enc);
                else p.addAfter("prepender", "encoder", enc);
                PacketBundleHandler bh = newState.bundleHandler();
                if (bh != null) {
                    PacketUnbundler u = new PacketUnbundler(bh);
                    if (p.get("unbundler") != null) p.replace("unbundler", "unbundler", u);
                    else p.addAfter("encoder", "unbundler", u);
                }
            } catch (Exception e) {
                LOGGER.error("[PbSession] Failed outbound transition to {}", newState.id(), e);
            }
        };

        if (this.channel.eventLoop().inEventLoop()) swap.run();
        else { try { this.channel.eventLoop().submit(swap).sync(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    }
}
