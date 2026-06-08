package eu.client.pingbypass.server;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketReceiveEvent;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Forwards S2C packets from the real server to the connected client.
 * Implements a lock/unlock mechanism (like PingBypass's S2PB2CPipeline) to
 * queue packets during critical state transitions (e.g., world state replay)
 * and flush them afterward, preventing out-of-order delivery and desync.
 */
public class S2CForwarder {
    private static final Logger LOGGER = LoggerFactory.getLogger(S2CForwarder.class);

    private final ClientConnection clientConnection;
    private final List<Packet<?>> queuedPackets = new CopyOnWriteArrayList<>();
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private volatile boolean active;

    public S2CForwarder(ClientConnection clientConnection) {
        this.clientConnection = clientConnection;
        this.active = false;
    }

    public void start() {
        this.active = true;
        eu.client.pingbypass.PingBypassFlags.proxyForwardingActive = true;
        EUClient.EVENT_HANDLER.subscribe(this);
        LOGGER.info("S2C forwarder started");
    }

    public void stop() {
        this.active = false;
        eu.client.pingbypass.PingBypassFlags.proxyForwardingActive = false;
        EUClient.EVENT_HANDLER.unsubscribe(this);
        LOGGER.info("S2C forwarder stopped");
    }

    /**
     * Locks the pipeline — incoming S2C packets will be queued instead of forwarded.
     * Use this before replaying world state to prevent interleaving.
     */
    public void lock() {
        synchronized (locked) {
            locked.set(true);
        }
    }

    /**
     * Unlocks the pipeline and flushes all queued packets to the client in order.
     * Call this after world state replay is complete.
     */
    public void unlockAndFlush() {
        synchronized (locked) {
            for (Packet<?> packet : queuedPackets) {
                sendToClient(packet);
            }
            queuedPackets.clear();
            locked.set(false);
        }
    }

    /** Forward S2C packets from real server to client */
    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!active || !clientConnection.isOpen()) return;

        // Only forward from the real server connection, not from the proxy's client connection
        if (event.getConnection() == clientConnection) return;

        Packet<?> packet = event.getPacket();

        // Never forward keepalive/ping — proxy handles these itself
        if (packet instanceof KeepAliveS2CPacket || packet instanceof CommonPingS2CPacket) {
            return;
        }

        // Don't forward BundleS2CPacket as a whole — the ClientConnectionMixin
        // fires individual PacketReceiveEvent for each sub-packet inside the bundle,
        // so those will be forwarded individually. Forwarding the bundle itself would
        // cause duplicates.
        if (packet instanceof net.minecraft.network.packet.s2c.play.BundleS2CPacket) {
            return;
        }

        synchronized (locked) {
            if (locked.get()) {
                queuedPackets.add(packet);
            } else {
                sendToClient(packet);
            }
        }

        // Do NOT cancel — let the proxy process all S2C packets for full state sync.
    }

    private void sendToClient(Packet<?> packet) {
        if (clientConnection.isOpen()) {
            clientConnection.send(packet);
        }
    }
}
