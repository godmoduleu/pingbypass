package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;

/**
 * Server → Client: Request the client to send a password.
 * Packet ID: 1
 * Payload: (empty)
 */
public class S2CPasswordRequestPacket extends PbPacket {
    public static final int ID = 1;

    public S2CPasswordRequestPacket() {
    }

    public S2CPasswordRequestPacket(PacketByteBuf buf) {
        // No payload to read
    }

    @Override
    public int getPacketId() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        // No payload to write
    }
}
