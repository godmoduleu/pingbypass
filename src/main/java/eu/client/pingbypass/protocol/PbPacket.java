package eu.client.pingbypass.protocol;

import net.minecraft.network.PacketByteBuf;

/**
 * Base class for all PingBypass custom protocol packets.
 * Each packet has a unique ID and can serialize/deserialize to a PacketByteBuf.
 * Protocol format: [packetId: VarInt][payload: PacketByteBuf]
 */
public abstract class PbPacket {

    /**
     * Returns the unique packet ID for this packet type.
     */
    public abstract int getPacketId();

    /**
     * Writes this packet's payload to the given buffer.
     * Does NOT write the packet ID — that is handled by the protocol layer.
     */
    public abstract void write(PacketByteBuf buf);
}
