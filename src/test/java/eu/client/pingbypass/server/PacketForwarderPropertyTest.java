package eu.client.pingbypass.server;

import eu.client.pingbypass.protocol.PbCustomPayload;
import net.jqwik.api.*;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Property-based test for bidirectional packet forwarding.
 *
 * <p><b>Validates: Requirements 5.1, 5.2</b></p>
 *
 * <p>Feature: pingbypass-proxy, Property 4: Bidirectional packet forwarding</p>
 *
 * <p>For any serverbound Minecraft packet that is NOT a CustomPayloadC2SPacket on
 * the euclient:pingbypass channel, the PacketForwarder SHALL forward it to the real
 * server's ClientConnection. Conversely, for any clientbound Minecraft packet received
 * from the real server, the PacketForwarder SHALL forward it to the client's
 * ClientConnection.</p>
 *
 * <p>Note: Special-case packets (PlayerMove, ClickSlot, UpdateSelectedSlot) are excluded
 * from this property test because they require MinecraftClient.getInstance() which is
 * unavailable in unit tests. Those are covered by unit tests in Task 7.5.</p>
 */
class PacketForwarderPropertyTest {

    /**
     * Represents the type of packet in our model.
     */
    enum PacketKind {
        /** A regular serverbound packet (not custom payload) */
        REGULAR_SERVERBOUND,
        /** A CustomPayloadC2SPacket on the euclient:pingbypass channel */
        PINGBYPASS_CUSTOM_PAYLOAD,
        /** A CustomPayloadC2SPacket on some other channel */
        OTHER_CUSTOM_PAYLOAD,
        /** A clientbound packet from the real server */
        CLIENTBOUND
    }

    @Provide
    Arbitrary<List<PacketKind>> packetSequences() {
        return Arbitraries.of(PacketKind.values())
                .list()
                .ofMinSize(1)
                .ofMaxSize(50);
    }

    /**
     * <b>Property 4: Bidirectional packet forwarding</b>
     *
     * <p>For any serverbound Minecraft packet that is NOT a CustomPayloadC2SPacket on
     * the euclient:pingbypass channel, the PacketForwarder SHALL forward it to the real
     * server's ClientConnection. Conversely, for any clientbound Minecraft packet
     * received from the real server, the PacketForwarder SHALL forward it to the
     * client's ClientConnection.</p>
     *
     * <p><b>Validates: Requirements 5.1, 5.2</b></p>
     */
    @Property(tries = 100)
    void bidirectionalPacketForwarding(
            @ForAll("packetSequences") List<PacketKind> packetKinds
    ) {
        // Set up mock connections
        ClientConnection proxyToClient = mock(ClientConnection.class);
        ClientConnection proxyToServer = mock(ClientConnection.class);
        when(proxyToServer.isOpen()).thenReturn(true);
        when(proxyToClient.isOpen()).thenReturn(true);

        PacketForwarder forwarder = new PacketForwarder(proxyToClient, proxyToServer);

        // Track expected call counts
        int expectedServerSends = 0;
        int expectedClientSends = 0;

        // Collect packets for verification
        List<Packet<?>> serverSentPackets = new ArrayList<>();
        List<Packet<?>> clientSentPackets = new ArrayList<>();

        for (PacketKind kind : packetKinds) {
            switch (kind) {
                case REGULAR_SERVERBOUND -> {
                    // A regular serverbound packet should be forwarded to the server
                    @SuppressWarnings("unchecked")
                    Packet<?> packet = mock(Packet.class);
                    forwarder.forwardToServer(packet);
                    serverSentPackets.add(packet);
                    expectedServerSends++;
                }
                case PINGBYPASS_CUSTOM_PAYLOAD -> {
                    // A CustomPayloadC2SPacket on euclient:pingbypass should NOT be forwarded
                    CustomPayloadC2SPacket packet = mock(CustomPayloadC2SPacket.class);
                    CustomPayload payload = mock(CustomPayload.class);
                    CustomPayload.Id<?> payloadId = mock(CustomPayload.Id.class);
                    when(packet.payload()).thenReturn(payload);
                    when(payload.getId()).thenReturn((CustomPayload.Id) payloadId);
                    when(payloadId.id()).thenReturn(PbCustomPayload.CHANNEL);
                    forwarder.forwardToServer(packet);
                    // Should NOT increment expectedServerSends — filtered out
                }
                case OTHER_CUSTOM_PAYLOAD -> {
                    // A CustomPayloadC2SPacket on a different channel should be forwarded
                    CustomPayloadC2SPacket packet = mock(CustomPayloadC2SPacket.class);
                    CustomPayload payload = mock(CustomPayload.class);
                    CustomPayload.Id<?> payloadId = mock(CustomPayload.Id.class);
                    when(packet.payload()).thenReturn(payload);
                    when(payload.getId()).thenReturn((CustomPayload.Id) payloadId);
                    when(payloadId.id()).thenReturn(Identifier.of("minecraft", "brand"));
                    forwarder.forwardToServer(packet);
                    serverSentPackets.add(packet);
                    expectedServerSends++;
                }
                case CLIENTBOUND -> {
                    // A clientbound packet should be forwarded to the client
                    @SuppressWarnings("unchecked")
                    Packet<?> packet = mock(Packet.class);
                    forwarder.forwardToClient(packet);
                    clientSentPackets.add(packet);
                    expectedClientSends++;
                }
            }
        }

        // Verify: all regular serverbound packets were forwarded to the server
        for (Packet<?> packet : serverSentPackets) {
            verify(proxyToServer).send(packet);
        }

        // Verify: all clientbound packets were forwarded to the client
        for (Packet<?> packet : clientSentPackets) {
            verify(proxyToClient).send(packet);
        }

        // Verify: total send counts match expectations
        verify(proxyToServer, times(expectedServerSends)).send(any(Packet.class));
        verify(proxyToClient, times(expectedClientSends)).send(any(Packet.class));
    }
}
