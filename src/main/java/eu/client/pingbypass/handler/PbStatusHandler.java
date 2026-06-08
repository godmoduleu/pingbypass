package eu.client.pingbypass.handler;

import eu.client.EUClient;
import eu.client.pingbypass.server.ProxyServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.listener.ServerQueryPacketListener;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Handles server list ping (STATUS) requests for the PingBypass proxy.
 * Responds with the proxy's current state so the client can show
 * whether the proxy is idle or connected to a server.
 */
public class PbStatusHandler implements ServerQueryPacketListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbStatusHandler.class);

    private final ProxyServer proxyServer;
    private final ClientConnection connection;
    private boolean responseSent;

    public PbStatusHandler(ProxyServer proxyServer, ClientConnection connection) {
        this.proxyServer = proxyServer;
        this.connection = connection;
    }

    @Override
    public void onRequest(QueryRequestC2SPacket packet) {
        if (responseSent) {
            connection.disconnect(Text.literal("Status already sent"));
            return;
        }
        responseSent = true;

        // Build MOTD showing proxy state
        Text description = buildDescription();

        ServerMetadata metadata = new ServerMetadata(
                description,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false);

        connection.send(new QueryResponseS2CPacket(metadata));
    }

    @Override
    public void onQueryPing(QueryPingC2SPacket packet) {
        connection.send(new PingResultS2CPacket(packet.getStartTime()));
        connection.disconnect(Text.literal("Ping done"));
    }

    @Override
    public void onDisconnected(DisconnectionInfo info) {}

    @Override
    public boolean isConnectionOpen() {
        return connection.isOpen();
    }

    private Text buildDescription() {
        MinecraftClient mc = MinecraftClient.getInstance();
        StringBuilder motd = new StringBuilder();
        motd.append("§dEUClient PingBypass§r\n");

        if (mc.getNetworkHandler() != null && mc.player != null && mc.world != null) {
            // Proxy is connected to a server
            String serverBrand = mc.getNetworkHandler().getBrand();
            motd.append("§aConnected§r");
            if (proxyServer.getServerConnection() != null) {
                motd.append(" — ").append(mc.getCurrentServerEntry() != null
                        ? mc.getCurrentServerEntry().address : "unknown");
            }
        } else {
            motd.append("§7Idle§r — not connected to any server");
        }

        return Text.literal(motd.toString());
    }
}
