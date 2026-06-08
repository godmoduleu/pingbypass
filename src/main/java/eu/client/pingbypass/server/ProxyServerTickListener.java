package eu.client.pingbypass.server;

import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketSendEvent;
import eu.client.events.impl.TickEvent;
import eu.client.pingbypass.PingBypassFlags;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;


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

    private static final ThreadLocal<Boolean> ALLOW_SEND = ThreadLocal.withInitial(() -> false);

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
