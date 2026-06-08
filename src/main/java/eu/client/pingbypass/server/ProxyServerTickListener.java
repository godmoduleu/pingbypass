package eu.client.pingbypass.server;

import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketSendEvent;
import eu.client.events.impl.TickEvent;
import eu.client.pingbypass.PingBypassFlags;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;

/**
 * Subscribes to the EUClient TickEvent and forwards ticks to the ProxyServer.
 * This ensures queued packets on proxy connections are processed each client tick.
 * Also suppresses duplicate packets that the proxy's own tick loop would send.
 */
public class ProxyServerTickListener {
    private final ProxyServer proxyServer;

    public ProxyServerTickListener(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (proxyServer.isAlive()) {
            proxyServer.tick();
        }
    }

    // ThreadLocal flag: when true, packets are allowed through (module-authorized)
    private static final ThreadLocal<Boolean> ALLOW_SEND = ThreadLocal.withInitial(() -> false);

    /** Call this to temporarily allow packets through the filter (for module use) */
    public static void allowSend(Runnable action) {
        ALLOW_SEND.set(true);
        try {
            action.run();
        } finally {
            ALLOW_SEND.set(false);
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (!PingBypassFlags.proxyForwardingActive) return;
        if (ALLOW_SEND.get()) return; // Module-authorized, let through

        // Block specific known-duplicate packet types that the proxy's own
        // tick loop sends automatically. The client's versions are forwarded
        // by PbPlayHandler.
        var packet = event.getPacket();
        if (packet instanceof ClientTickEndC2SPacket
                || packet instanceof net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket
                || packet instanceof net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket
                || packet instanceof net.minecraft.network.packet.c2s.play.BoatPaddleStateC2SPacket
                || packet instanceof net.minecraft.network.packet.c2s.common.ClientOptionsC2SPacket
                || packet instanceof net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket
                || packet instanceof net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
                || packet instanceof net.minecraft.network.packet.c2s.play.PlayerLoadedC2SPacket) {
            event.setCancelled(true);
        }
    }
}
