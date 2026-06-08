package eu.client.pingbypass.protocol;

import net.minecraft.network.PacketByteBuf;

public abstract class PbPacket {

    public abstract int getPacketId();

    public abstract void write(PacketByteBuf buf);
}
