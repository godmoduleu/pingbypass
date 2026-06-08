package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;

public class S2CModuleStatePacket extends PbPacket {
    public static final int ID = 5;

    private final String moduleName;
    private final boolean enabled;

    public S2CModuleStatePacket(String moduleName, boolean enabled) {
        this.moduleName = moduleName;
        this.enabled = enabled;
    }

    public S2CModuleStatePacket(PacketByteBuf buf) {
        this.moduleName = buf.readString();
        this.enabled = buf.readBoolean();
    }

    @Override
    public int getPacketId() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(moduleName);
        buf.writeBoolean(enabled);
    }

    public String getModuleName() {
        return moduleName;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
