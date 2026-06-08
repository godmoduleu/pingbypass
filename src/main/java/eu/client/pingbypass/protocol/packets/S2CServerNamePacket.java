package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;

public class S2CServerNamePacket extends PbPacket {
    public static final int ID = 8;

    private final String serverIp;

    public S2CServerNamePacket(String serverIp) {
        this.serverIp = serverIp;
    }

    public S2CServerNamePacket(PacketByteBuf buf) {
        this.serverIp = buf.readString();
    }

    @Override
    public int getPacketId() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(serverIp);
    }

    public String getServerIp() {
        return serverIp;
    }
}
