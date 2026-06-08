package eu.client.pingbypass.protocol.packets;

import eu.client.pingbypass.protocol.PbPacket;
import net.minecraft.network.PacketByteBuf;

/**
 * Client → Proxy: Forward mouse/keyboard input.
 * The proxy replays these inputs through its own game loop,
 * generating properly sequenced packets to the real server.
 *
 * Packet ID: 10
 * Payload: inputType (VarInt), button (VarInt), action (VarInt)
 */
public class C2SInputPacket extends PbPacket {
    public static final int ID = 10;

    public static final int TYPE_MOUSE = 0;
    public static final int TYPE_KEY = 1;

    public static final int ACTION_PRESS = 1;
    public static final int ACTION_RELEASE = 0;

    private final int inputType;
    private final int button;
    private final int action;

    public C2SInputPacket(int inputType, int button, int action) {
        this.inputType = inputType;
        this.button = button;
        this.action = action;
    }

    public C2SInputPacket(PacketByteBuf buf) {
        this.inputType = buf.readVarInt();
        this.button = buf.readVarInt();
        this.action = buf.readVarInt();
    }

    @Override public int getPacketId() { return ID; }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(inputType);
        buf.writeVarInt(button);
        buf.writeVarInt(action);
    }

    public int getInputType() { return inputType; }
    public int getButton() { return button; }
    public int getAction() { return action; }
}
