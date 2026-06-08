package eu.client.pingbypass.input;

import eu.client.Pingbypass;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.MouseInputEvent;
import eu.client.pingbypass.PingBypassFlags;
import eu.client.pingbypass.protocol.PbCustomPayload;
import eu.client.pingbypass.protocol.packets.C2SInputPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;

public class ClientInputForwarder {

    @SubscribeEvent
    public void onMouseInput(MouseInputEvent event) {
        if (!PingBypassFlags.proxyForwardingActive) return;
        if (Pingbypass.PINGBYPASS_CONFIG != null && Pingbypass.PINGBYPASS_CONFIG.isServer()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;

        int button = event.getButton();
        // Only forward attack (0) and use (1) buttons
        if (button != 0 && button != 1) return;

        // Send press event
        sendInput(mc, C2SInputPacket.TYPE_MOUSE, button, C2SInputPacket.ACTION_PRESS);
    }

    private void sendInput(MinecraftClient mc, int type, int button, int action) {
        try {
            var payload = PbCustomPayload.fromPacket(new C2SInputPacket(type, button, action));
            mc.getNetworkHandler().getConnection().send(new CustomPayloadC2SPacket(payload));
        } catch (Exception e) {
            Pingbypass.LOGGER.warn("[PingBypass] Failed to send input", e);
        }
    }

    public void start() {
        Pingbypass.EVENT_HANDLER.subscribe(this);
        Pingbypass.LOGGER.info("[PingBypass] Client input forwarder started");
    }

    public void stop() {
        Pingbypass.EVENT_HANDLER.unsubscribe(this);
    }
}
