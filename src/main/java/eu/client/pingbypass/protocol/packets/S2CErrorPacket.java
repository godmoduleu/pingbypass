package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;

public class S2CErrorPacket extends PbPacket {
    public static final int ID = 7;

    private final String message;

    public S2CErrorPacket(String message) {
        this.message = message;
    }

    public S2CErrorPacket(PacketByteBuf buf) {
        this.message = buf.readString();
    }

    @Override
    public int getPacketId() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(message);
    }

    public String getMessage() {
        return message;
    }
}
