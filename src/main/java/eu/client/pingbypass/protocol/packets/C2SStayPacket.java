package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;

/**
 * Client → Server: Tell the proxy whether to stay connected when the client disconnects.
 * Packet ID: 9
 * Payload: stay (Boolean)
 */
public class C2SStayPacket extends PbPacket {
    public static final int ID = 9;

    private final boolean stay;

    public C2SStayPacket(boolean stay) {
        this.stay = stay;
    }

    public C2SStayPacket(PacketByteBuf buf) {
        this.stay = buf.readBoolean();
    }

    @Override
    public int getPacketId() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(stay);
    }

    public boolean isStay() {
        return stay;
    }
}
