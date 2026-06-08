package eu.client.pingbypass.protocol;

import net.minecraft.network.ClientConnection;

@FunctionalInterface
public interface PbPacketHandler<T extends PbPacket> {
    void handle(T packet, ClientConnection connection);
}
