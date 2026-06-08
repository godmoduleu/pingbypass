package eu.client.pingbypass.server;

import eu.client.Pingbypass;
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
        Pingbypass.EVENT_HANDLER.subscribe(this);
        LOGGER.info("S2C forwarder started");
    }

    public void stop() {
        this.active = false;
        eu.client.pingbypass.PingBypassFlags.proxyForwardingActive = false;
        Pingbypass.EVENT_HANDLER.unsubscribe(this);
        LOGGER.info("S2C forwarder stopped");
    }

    public void lock() {
        synchronized (locked) {
            locked.set(true);
        }
    }

    public void unlockAndFlush() {
        synchronized (locked) {
            for (Packet<?> packet : queuedPackets) {
                sendToClient(packet);
            }
            queuedPackets.clear();
            locked.set(false);
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!active || !clientConnection.isOpen()) return;

        if (event.getConnection() == clientConnection) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof KeepAliveS2CPacket || packet instanceof CommonPingS2CPacket) {
            return;
        }

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

    }

    private void sendToClient(Packet<?> packet) {
        if (clientConnection.isOpen()) {
            clientConnection.send(packet);
        }
    }
}
