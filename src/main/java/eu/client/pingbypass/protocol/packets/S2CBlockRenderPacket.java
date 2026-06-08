package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

/**
 * Server → Client: Sync a block placement render position from proxy to client.
 * Packet ID: 11
 */
public class S2CBlockRenderPacket extends PbPacket {
    public static final int ID = 11;

    private final BlockPos position;

    public S2CBlockRenderPacket(BlockPos position) {
        this.position = position;
    }

    public S2CBlockRenderPacket(PacketByteBuf buf) {
        this.position = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public int getPacketId() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(position.getX());
        buf.writeInt(position.getY());
        buf.writeInt(position.getZ());
    }

    public BlockPos getPosition() {
        return position;
    }
}
