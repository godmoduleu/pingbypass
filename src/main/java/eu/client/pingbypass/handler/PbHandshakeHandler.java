package eu.client.pingbypass.handler;

import eu.client.pingbypass.server.ProxyServer;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.listener.ServerHandshakePacketListener;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.network.state.LoginStates;
import net.minecraft.network.state.QueryStates;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the initial Minecraft handshake packet for the PingBypass proxy.
 * Validates protocol version, enforces single-connection invariant,
 * and transitions to the appropriate next handler based on client intent.
 */
public class PbHandshakeHandler implements ServerHandshakePacketListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbHandshakeHandler.class);

    /** Protocol version for Minecraft 1.21.4 */
    private static final int PROTOCOL_VERSION = 769;

    private final ProxyServer proxyServer;
    private final ClientConnection connection;

    public PbHandshakeHandler(ProxyServer proxyServer, ClientConnection connection) {
        this.proxyServer = proxyServer;
        this.connection = connection;
    }

    @Override
    public void onHandshake(HandshakeC2SPacket packet) {
        switch (packet.intendedState()) {
            case LOGIN -> handleLogin(packet);
            case STATUS -> handleStatus();
            default -> throw new UnsupportedOperationException(
                    "Invalid intention " + packet.intendedState());
        }
    }

    private void handleLogin(HandshakeC2SPacket packet) {
        this.connection.transitionOutbound(LoginStates.S2C);

        if (packet.protocolVersion() != PROTOCOL_VERSION) {
            Text message;
            if (packet.protocolVersion() < PROTOCOL_VERSION) {
                message = Text.translatable(
                        "multiplayer.disconnect.outdated_client", "1.21.4");
            } else {
                message = Text.translatable(
                        "multiplayer.disconnect.incompatible", "1.21.4");
            }

            this.connection.send(new LoginDisconnectS2CPacket(message));
            this.connection.disconnect(message);
            LOGGER.info("Rejected connection from {}: wrong protocol version {}",
                    this.connection.getAddressAsString(false), packet.protocolVersion());
            return;
        }

        // Single-connection invariant: reject if a client is already connected
        if (hasActivePlayConnection()) {
            Text message = Text.literal("This PingBypass server is already in use!");
            this.connection.send(new LoginDisconnectS2CPacket(message));
            this.connection.disconnect(message);
            LOGGER.info("Rejected connection from {}: another client is already connected",
                    this.connection.getAddressAsString(false));
            return;
        }

        // Transition to login handler
        this.connection.transitionInbound(LoginStates.C2S,
                new PbLoginHandler(this.proxyServer, this.connection));
        LOGGER.info("Client {} starting login",
                this.connection.getAddressAsString(false));
    }

    private void handleStatus() {
        this.connection.transitionOutbound(QueryStates.S2C);
        this.connection.transitionInbound(QueryStates.C2S,
                new PbStatusHandler(this.proxyServer, this.connection));
        LOGGER.debug("Received STATUS handshake, serving proxy status");
    }

    /**
     * Checks whether there is already an active play-state connection
     * by inspecting the proxy server's connections list.
     */
    private boolean hasActivePlayConnection() {
        for (ClientConnection conn : this.proxyServer.getConnections()) {
            if (conn == this.connection) {
                continue;
            }
            if (conn.isOpen() && conn.getPacketListener() != null
                    && conn.getPacketListener().getPhase() == NetworkPhase.PLAY) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDisconnected(DisconnectionInfo info) {
        // Nothing to clean up during handshake
    }

    @Override
    public boolean isConnectionOpen() {
        return this.connection.isOpen();
    }
}
