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

/**
 * Intercepts packets flowing through the proxy and applies necessary corrections.
 * Handles bidirectional forwarding between the client and the real Minecraft server.
 */
public class PacketForwarder {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketForwarder.class);

    private final ClientConnection proxyToClient;
    private final ClientConnection proxyToServer;

    public PacketForwarder(ClientConnection proxyToClient, ClientConnection proxyToServer) {
        this.proxyToClient = proxyToClient;
        this.proxyToServer = proxyToServer;
    }

    /**
     * Forward a serverbound packet from the client to the real server.
     * Applies special handling for certain packet types.
     */
    public void forwardToServer(Packet<?> packet) {
        try {
            // Filter custom payload packets on the euclient:pingbypass channel
            if (packet instanceof CustomPayloadC2SPacket customPayload) {
                CustomPayload payload = customPayload.payload();
                if (PbCustomPayload.CHANNEL.equals(payload.getId().id())) {
                    // Dispatch to protocol handler, don't forward to server
                    handlePingBypassPayload(customPayload);
                    return;
                }
            }

            // PlayerMoveC2SPacket: schedule on main thread for ordering
            if (packet instanceof PlayerMoveC2SPacket) {
                MinecraftClient.getInstance().execute(() -> {
                    if (proxyToServer.isOpen()) {
                        proxyToServer.send(packet);
                    }
                });
                return;
            }

            // ClickSlotC2SPacket: re-create with fresh revision from server-side container
            if (packet instanceof ClickSlotC2SPacket clickSlot) {
                handleClickSlot(clickSlot);
                return;
            }

            // UpdateSelectedSlotC2SPacket: forward and sync server-side slot
            if (packet instanceof UpdateSelectedSlotC2SPacket selectedSlot) {
                handleSelectedSlot(selectedSlot);
                return;
            }

            // Default: forward directly to server
            proxyToServer.send(packet);
        } catch (Exception e) {
            LOGGER.error("Error forwarding packet to server: {}", packet.getClass().getSimpleName(), e);
        }
    }

    /**
     * Forward a clientbound packet from the real server to the client.
     */
    public void forwardToClient(Packet<?> packet) {
        try {
            proxyToClient.send(packet);
        } catch (Exception e) {
            LOGGER.error("Error forwarding packet to client: {}", packet.getClass().getSimpleName(), e);
        }
    }

    /**
     * Handle ClickSlotC2SPacket by re-creating it with the server-side container's
     * current revision (stateId) to keep the transaction in sync.
     */
    private void handleClickSlot(ClickSlotC2SPacket clickSlot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player == null || !proxyToServer.isOpen()) {
                return;
            }

            try {
                // Find the matching container on the server-side player
                ScreenHandler handler = clickSlot.getSyncId() == mc.player.currentScreenHandler.syncId
                        ? mc.player.currentScreenHandler
                        : mc.player.playerScreenHandler;

                int freshRevision = handler.getRevision();

                // Re-create the packet with the fresh revision
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

    /**
     * Handle UpdateSelectedSlotC2SPacket by forwarding it and syncing
     * the server-side player's selected slot.
     */
    private void handleSelectedSlot(UpdateSelectedSlotC2SPacket selectedSlot) {
        // Forward the packet to the real server
        proxyToServer.send(selectedSlot);

        // Sync the server-side player's held item slot
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.getInventory().selectedSlot = selectedSlot.getSelectedSlot();
            }
        });
    }

    /**
     * Handle custom payload packets on the euclient:pingbypass channel.
     * Dispatches to PbProtocolHandler (to be created in Task 8.2).
     */
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
