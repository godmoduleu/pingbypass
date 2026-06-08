package eu.client.pingbypass.handler;

import com.mojang.authlib.GameProfile;
import eu.client.EUClient;
import eu.client.pingbypass.server.ProxyServer;
import eu.client.pingbypass.server.RegistryCache;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.listener.ServerConfigurationPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.*;
import net.minecraft.network.packet.c2s.config.ReadyC2SPacket;
import net.minecraft.network.packet.c2s.config.SelectKnownPacksC2SPacket;
import net.minecraft.network.packet.s2c.config.ReadyS2CPacket;
import net.minecraft.network.packet.s2c.config.SelectKnownPacksS2CPacket;
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.registry.DynamicRegistryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles the CONFIGURATION phase for the PingBypass proxy.
 * Uses cached registry data from RegistryCache (loaded at startup via SaveLoading)
 * to send proper registries and tags to the connecting client.
 */
public class PbConfigurationHandler implements ServerConfigurationPacketListener, TickablePacketListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbConfigurationHandler.class);

    private final ProxyServer proxyServer;
    private final ClientConnection connection;
    private final GameProfile profile;
    private boolean knownPacksSent;

    public PbConfigurationHandler(ProxyServer proxyServer, ClientConnection connection, GameProfile profile) {
        this.proxyServer = proxyServer;
        this.connection = connection;
        this.profile = profile;
    }

    @Override
    public void tick() {
        if (!knownPacksSent) {
            knownPacksSent = true;
            RegistryCache cache = proxyServer.getRegistryCache();
            if (cache.isLoaded()) {
                connection.send(new SelectKnownPacksS2CPacket(cache.getKnownPacks()));
            } else {
                // Fallback: send empty known packs
                connection.send(new SelectKnownPacksS2CPacket(List.of()));
            }
            LOGGER.info("Sent SelectKnownPacks to {}", profile.getName());
        }
    }

    @Override
    public void onSelectKnownPacks(SelectKnownPacksC2SPacket packet) {
        LOGGER.info("Client {} responded with known packs, sending registries", profile.getName());

        RegistryCache cache = proxyServer.getRegistryCache();
        if (cache.isLoaded()) {
            // Replay all cached registry + tag packets
            for (Packet<?> p : cache.getRegistryPackets()) {
                connection.send(p);
            }
        }

        connection.send(ReadyS2CPacket.INSTANCE);
        LOGGER.info("Sent {} registry packets and FINISH_CONFIGURATION to {}",
                cache.isLoaded() ? cache.getRegistryPackets().size() : 0, profile.getName());
    }

    @Override
    public void onReady(ReadyC2SPacket packet) {
        LOGGER.info("Client {} acknowledged FINISH_CONFIGURATION, transitioning to PLAY", profile.getName());

        RegistryCache cache = proxyServer.getRegistryCache();
        DynamicRegistryManager registryManager = cache.isLoaded()
                ? cache.getRegistryManager() : DynamicRegistryManager.EMPTY;

        this.connection.transitionOutbound(
                PlayStateFactories.S2C.bind(RegistryByteBuf.makeFactory(registryManager)));

        if (EUClient.PINGBYPASS_CONFIG.hasPassword()) {
            this.connection.transitionInbound(
                    PlayStateFactories.C2S.bind(RegistryByteBuf.makeFactory(DynamicRegistryManager.EMPTY)),
                    new PbPasswordHandler(this.proxyServer, this.connection, this.profile, registryManager));
            LOGGER.info("Transitioning {} to password verification", profile.getName());
        } else {
            this.connection.transitionInbound(
                    PlayStateFactories.C2S.bind(RegistryByteBuf.makeFactory(DynamicRegistryManager.EMPTY)),
                    new PbWaitingHandler(this.proxyServer, this.connection, this.profile, registryManager));
            LOGGER.info("Transitioning {} to waiting state", profile.getName());
        }
    }

    @Override public void onDisconnected(DisconnectionInfo info) {
        LOGGER.info("Client {} disconnected during configuration: {}", profile.getName(), info.reason());
    }
    @Override public boolean isConnectionOpen() { return this.connection.isOpen(); }
    @Override public void onClientOptions(ClientOptionsC2SPacket p) {}
    @Override public void onKeepAlive(KeepAliveC2SPacket p) {}
    @Override public void onPong(CommonPongC2SPacket p) {}
    @Override public void onResourcePackStatus(ResourcePackStatusC2SPacket p) {}
    @Override public void onCookieResponse(CookieResponseC2SPacket p) {}
    @Override public void onCustomPayload(CustomPayloadC2SPacket p) {}
}
