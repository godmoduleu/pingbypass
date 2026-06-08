package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;

/**
 * Client → Server: Request the proxy to join a Minecraft server.
 * Packet ID: 0
 * Payload: serverIp (String), serverPort (VarInt)
 */
public class C2SJoinPacket extends PbPacket {
    public static final int ID = 0;

    private final String serverIp;
    private final int serverPort;

    public C2SJoinPacket(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public C2SJoinPacket(PacketByteBuf buf) {
        this.serverIp = buf.readString();
        this.serverPort = buf.readVarInt();
    }

    @Override
    public int getPacketId() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(serverIp);
        buf.writeVarInt(serverPort);
    }

    public String getServerIp() {
        return serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }
}
