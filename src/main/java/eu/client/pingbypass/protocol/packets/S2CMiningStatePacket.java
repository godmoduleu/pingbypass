package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class S2CMiningStatePacket extends PbPacket {
    public static final int ID = 12;

    private final boolean hasPrimary;
    @Nullable private final BlockPos primaryPos;
    private final float primaryProgress;

    private final boolean hasSecondary;
    @Nullable private final BlockPos secondaryPos;
    private final float secondaryProgress;

    public S2CMiningStatePacket(@Nullable BlockPos primaryPos, float primaryProgress,
                                @Nullable BlockPos secondaryPos, float secondaryProgress) {
        this.hasPrimary = primaryPos != null;
        this.primaryPos = primaryPos;
        this.primaryProgress = primaryProgress;
        this.hasSecondary = secondaryPos != null;
        this.secondaryPos = secondaryPos;
        this.secondaryProgress = secondaryProgress;
    }

    public S2CMiningStatePacket(PacketByteBuf buf) {
        this.hasPrimary = buf.readBoolean();
        if (hasPrimary) {
            this.primaryPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            this.primaryProgress = buf.readFloat();
        } else {
            this.primaryPos = null;
            this.primaryProgress = 0;
        }
        this.hasSecondary = buf.readBoolean();
        if (hasSecondary) {
            this.secondaryPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            this.secondaryProgress = buf.readFloat();
        } else {
            this.secondaryPos = null;
            this.secondaryProgress = 0;
        }
    }

    @Override
    public int getPacketId() { return ID; }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(hasPrimary);
        if (hasPrimary) {
            buf.writeInt(primaryPos.getX());
            buf.writeInt(primaryPos.getY());
            buf.writeInt(primaryPos.getZ());
            buf.writeFloat(primaryProgress);
        }
        buf.writeBoolean(hasSecondary);
        if (hasSecondary) {
            buf.writeInt(secondaryPos.getX());
            buf.writeInt(secondaryPos.getY());
            buf.writeInt(secondaryPos.getZ());
            buf.writeFloat(secondaryProgress);
        }
    }

    public boolean hasPrimary() { return hasPrimary; }
    @Nullable public BlockPos getPrimaryPos() { return primaryPos; }
    public float getPrimaryProgress() { return primaryProgress; }
    public boolean hasSecondary() { return hasSecondary; }
    @Nullable public BlockPos getSecondaryPos() { return secondaryPos; }
    public float getSecondaryProgress() { return secondaryProgress; }
}
