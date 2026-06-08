package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;

/**
 * Client → Server: Toggle a proxy module on/off.
 * Packet ID: 3
 * Payload: moduleName (String), enabled (Boolean)
 */
public class C2SModuleTogglePacket extends PbPacket {
    public static final int ID = 3;

    private final String moduleName;
    private final boolean enabled;

    public C2SModuleTogglePacket(String moduleName, boolean enabled) {
        this.moduleName = moduleName;
        this.enabled = enabled;
    }

    public C2SModuleTogglePacket(PacketByteBuf buf) {
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
