package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;

/**
 * Client → Server: Send password for authentication.
 * Packet ID: 2
 * Payload: password (String)
 */
public class C2SPasswordPacket extends PbPacket {
    public static final int ID = 2;

    private final String password;

    public C2SPasswordPacket(String password) {
        this.password = password;
    }

    public C2SPasswordPacket(PacketByteBuf buf) {
        this.password = buf.readString();
    }

    @Override
    public int getPacketId() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(password);
    }

    public String getPassword() {
        return password;
    }
}
