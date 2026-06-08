package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;

/**
 * Client → Server: Change a setting on a proxy module.
 * Packet ID: 4
 * Payload: moduleName (String), settingName (String), value (String)
 */
public class C2SSettingChangePacket extends PbPacket {
    public static final int ID = 4;

    private final String moduleName;
    private final String settingName;
    private final String value;

    public C2SSettingChangePacket(String moduleName, String settingName, String value) {
        this.moduleName = moduleName;
        this.settingName = settingName;
        this.value = value;
    }

    public C2SSettingChangePacket(PacketByteBuf buf) {
        this.moduleName = buf.readString();
        this.settingName = buf.readString();
        this.value = buf.readString();
    }

    @Override
    public int getPacketId() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(moduleName);
        buf.writeString(settingName);
        buf.writeString(value);
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getSettingName() {
        return settingName;
    }

    public String getValue() {
        return value;
    }
}
