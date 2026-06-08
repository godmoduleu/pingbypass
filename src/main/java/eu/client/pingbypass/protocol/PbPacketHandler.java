package eu.client.pingbypass.protocol;

import net.minecraft.network.ClientConnection;

/**
 * Functional interface for handling a deserialized PbPacket.
 *
 * @param <T> the specific packet type this handler processes
 */
@FunctionalInterface
public interface PbPacketHandler<T extends PbPacket> {
    void handle(T packet, ClientConnection connection);
}
