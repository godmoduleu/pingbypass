package eu.client.pingbypass.server;

import eu.client.pingbypass.protocol.PbCustomPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.screen.ScreenHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketForwarder {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketForwarder.class);

    private final ClientConnection proxyToClient;
    private final ClientConnection proxyToServer;

    public PacketForwarder(ClientConnection proxyToClient, ClientConnection proxyToServer) {
        this.proxyToClient = proxyToClient;
        this.proxyToServer = proxyToServer;
    }

    public void forwardToServer(Packet<?> packet) {
        try {
            if (packet instanceof CustomPayloadC2SPacket customPayload) {
                CustomPayload payload = customPayload.payload();
                if (PbCustomPayload.CHANNEL.equals(payload.getId().id())) {
                    handlePingBypassPayload(customPayload);
                    return;
                }
            }

            if (packet instanceof PlayerMoveC2SPacket) {
                MinecraftClient.getInstance().execute(() -> {
                    if (proxyToServer.isOpen()) {
                        proxyToServer.send(packet);
                    }
                });
                return;
            }

            if (packet instanceof ClickSlotC2SPacket clickSlot) {
                handleClickSlot(clickSlot);
                return;
            }

            if (packet instanceof UpdateSelectedSlotC2SPacket selectedSlot) {
                handleSelectedSlot(selectedSlot);
                return;
            }

            proxyToServer.send(packet);
        } catch (Exception e) {
            LOGGER.error("Error forwarding packet to server: {}", packet.getClass().getSimpleName(), e);
        }
    }

    public void forwardToClient(Packet<?> packet) {
        try {
            proxyToClient.send(packet);
        } catch (Exception e) {
            LOGGER.error("Error forwarding packet to client: {}", packet.getClass().getSimpleName(), e);
        }
    }

    private void handleClickSlot(ClickSlotC2SPacket clickSlot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player == null || !proxyToServer.isOpen()) {
                return;
            }

            try {
                ScreenHandler handler = clickSlot.getSyncId() == mc.player.currentScreenHandler.syncId
                        ? mc.player.currentScreenHandler
                        : mc.player.playerScreenHandler;

                int freshRevision = handler.getRevision();

                ClickSlotC2SPacket corrected = new ClickSlotC2SPacket(
                        clickSlot.getSyncId(),
                        freshRevision,
                        clickSlot.getSlot(),
                        clickSlot.getButton(),
                        clickSlot.getActionType(),
                        clickSlot.getStack(),
                        clickSlot.getModifiedStacks()
                );

                proxyToServer.send(corrected);
            } catch (Exception e) {
                LOGGER.error("Error handling ClickSlotC2SPacket", e);
                // Fall back to forwarding the original packet
                proxyToServer.send(clickSlot);
            }
        });
    }

    private void handleSelectedSlot(UpdateSelectedSlotC2SPacket selectedSlot) {
        proxyToServer.send(selectedSlot);

        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.getInventory().selectedSlot = selectedSlot.getSelectedSlot();
            }
        });
    }

    private void handlePingBypassPayload(CustomPayloadC2SPacket packet) {
        CustomPayload payload = packet.payload();
        if (payload instanceof PbCustomPayload pbPayload) {
            // TODO: Dispatch to PbProtocolHandler once created in Task 8.2
            LOGGER.debug("Received PingBypass protocol packet, dispatching to protocol handler");
        }
    }

    public ClientConnection getProxyToClient() {
        return proxyToClient;
    }

    public ClientConnection getProxyToServer() {
        return proxyToServer;
    }
}
