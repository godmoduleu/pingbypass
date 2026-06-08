package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Server → Client: Sync the AutoCrystal render position from proxy to client.
 * Packet ID: 10
 * Payload: hasPosition (Boolean), [x (Int), y (Int), z (Int)] if hasPosition
 */
public class S2CRenderPositionPacket extends PbPacket {
    public static final int ID = 10;

    @Nullable
    private final BlockPos position;

    public S2CRenderPositionPacket(@Nullable BlockPos position) {
        this.position = position;
    }

    public S2CRenderPositionPacket(PacketByteBuf buf) {
        if (buf.readBoolean()) {
            this.position = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        } else {
            this.position = null;
        }
    }

    @Override
    public int getPacketId() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(position != null);
        if (position != null) {
            buf.writeInt(position.getX());
            buf.writeInt(position.getY());
            buf.writeInt(position.getZ());
        }
    }

    @Nullable
    public BlockPos getPosition() {
        return position;
    }
}
