package eu.client.pingbypass.protocol;

import eu.client.pingbypass.protocol.packets.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PbPacket serialization — each packet type write/read round-trip
 * with known values, plus malformed packet handling in PbProtocolHandler.
 *
 * <p><b>Validates: Requirements 9.5</b></p>
 */
class PbPacketSerializationTest {

    // ---- Packet ID 0: C2SJoinPacket ----

    @Test
    void c2sJoinPacket_roundTrip() {
        C2SJoinPacket original = new C2SJoinPacket("mc.hypixel.net", 25565);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        C2SJoinPacket read = new C2SJoinPacket(buf);
        assertEquals("mc.hypixel.net", read.getServerIp());
        assertEquals(25565, read.getServerPort());
        assertEquals(C2SJoinPacket.ID, read.getPacketId());
        buf.release();
    }

    @Test
    void c2sJoinPacket_emptyIp() {
        C2SJoinPacket original = new C2SJoinPacket("", 1);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        C2SJoinPacket read = new C2SJoinPacket(buf);
        assertEquals("", read.getServerIp());
        assertEquals(1, read.getServerPort());
        buf.release();
    }

    // ---- Packet ID 1: S2CPasswordRequestPacket ----

    @Test
    void s2cPasswordRequestPacket_roundTrip() {
        S2CPasswordRequestPacket original = new S2CPasswordRequestPacket();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        // Buffer should be empty (no payload)
        assertEquals(0, buf.readableBytes());
        S2CPasswordRequestPacket read = new S2CPasswordRequestPacket(buf);
        assertEquals(S2CPasswordRequestPacket.ID, read.getPacketId());
        buf.release();
    }

    // ---- Packet ID 2: C2SPasswordPacket ----

    @Test
    void c2sPasswordPacket_roundTrip() {
        C2SPasswordPacket original = new C2SPasswordPacket("s3cretP@ss!");
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        C2SPasswordPacket read = new C2SPasswordPacket(buf);
        assertEquals("s3cretP@ss!", read.getPassword());
        assertEquals(C2SPasswordPacket.ID, read.getPacketId());
        buf.release();
    }

    @Test
    void c2sPasswordPacket_emptyPassword() {
        C2SPasswordPacket original = new C2SPasswordPacket("");
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        C2SPasswordPacket read = new C2SPasswordPacket(buf);
        assertEquals("", read.getPassword());
        buf.release();
    }

    // ---- Packet ID 3: C2SModuleTogglePacket ----

    @Test
    void c2sModuleTogglePacket_roundTrip_enabled() {
        C2SModuleTogglePacket original = new C2SModuleTogglePacket("AutoCrystal", true);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        C2SModuleTogglePacket read = new C2SModuleTogglePacket(buf);
        assertEquals("AutoCrystal", read.getModuleName());
        assertTrue(read.isEnabled());
        assertEquals(C2SModuleTogglePacket.ID, read.getPacketId());
        buf.release();
    }

    @Test
    void c2sModuleTogglePacket_roundTrip_disabled() {
        C2SModuleTogglePacket original = new C2SModuleTogglePacket("AutoTotem", false);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        C2SModuleTogglePacket read = new C2SModuleTogglePacket(buf);
        assertEquals("AutoTotem", read.getModuleName());
        assertFalse(read.isEnabled());
        buf.release();
    }

    // ---- Packet ID 4: C2SSettingChangePacket ----

    @Test
    void c2sSettingChangePacket_roundTrip() {
        C2SSettingChangePacket original = new C2SSettingChangePacket("AutoCrystal", "PlaceRange", "4.5");
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        C2SSettingChangePacket read = new C2SSettingChangePacket(buf);
        assertEquals("AutoCrystal", read.getModuleName());
        assertEquals("PlaceRange", read.getSettingName());
        assertEquals("4.5", read.getValue());
        assertEquals(C2SSettingChangePacket.ID, read.getPacketId());
        buf.release();
    }

    // ---- Packet ID 5: S2CModuleStatePacket ----

    @Test
    void s2cModuleStatePacket_roundTrip() {
        S2CModuleStatePacket original = new S2CModuleStatePacket("Surround", true);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        S2CModuleStatePacket read = new S2CModuleStatePacket(buf);
        assertEquals("Surround", read.getModuleName());
        assertTrue(read.isEnabled());
        assertEquals(S2CModuleStatePacket.ID, read.getPacketId());
        buf.release();
    }

    // ---- Packet ID 6: S2CSettingStatePacket ----

    @Test
    void s2cSettingStatePacket_roundTrip() {
        S2CSettingStatePacket original = new S2CSettingStatePacket("AutoTotem", "Health", "6.0");
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        S2CSettingStatePacket read = new S2CSettingStatePacket(buf);
        assertEquals("AutoTotem", read.getModuleName());
        assertEquals("Health", read.getSettingName());
        assertEquals("6.0", read.getValue());
        assertEquals(S2CSettingStatePacket.ID, read.getPacketId());
        buf.release();
    }

    // ---- Packet ID 7: S2CErrorPacket ----

    @Test
    void s2cErrorPacket_roundTrip() {
        S2CErrorPacket original = new S2CErrorPacket("Connection refused");
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        S2CErrorPacket read = new S2CErrorPacket(buf);
        assertEquals("Connection refused", read.getMessage());
        assertEquals(S2CErrorPacket.ID, read.getPacketId());
        buf.release();
    }

    // ---- Packet ID 8: S2CServerNamePacket ----

    @Test
    void s2cServerNamePacket_roundTrip() {
        S2CServerNamePacket original = new S2CServerNamePacket("play.example.com");
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        S2CServerNamePacket read = new S2CServerNamePacket(buf);
        assertEquals("play.example.com", read.getServerIp());
        assertEquals(S2CServerNamePacket.ID, read.getPacketId());
        buf.release();
    }

    // ---- Packet ID 9: C2SStayPacket ----

    @Test
    void c2sStayPacket_roundTrip_true() {
        C2SStayPacket original = new C2SStayPacket(true);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        C2SStayPacket read = new C2SStayPacket(buf);
        assertTrue(read.isStay());
        assertEquals(C2SStayPacket.ID, read.getPacketId());
        buf.release();
    }

    @Test
    void c2sStayPacket_roundTrip_false() {
        C2SStayPacket original = new C2SStayPacket(false);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        C2SStayPacket read = new C2SStayPacket(buf);
        assertFalse(read.isStay());
        buf.release();
    }

    // ---- Packet IDs are unique and correct ----

    @Test
    void packetIds_areCorrect() {
        assertEquals(0, C2SJoinPacket.ID);
        assertEquals(1, S2CPasswordRequestPacket.ID);
        assertEquals(2, C2SPasswordPacket.ID);
        assertEquals(3, C2SModuleTogglePacket.ID);
        assertEquals(4, C2SSettingChangePacket.ID);
        assertEquals(5, S2CModuleStatePacket.ID);
        assertEquals(6, S2CSettingStatePacket.ID);
        assertEquals(7, S2CErrorPacket.ID);
        assertEquals(8, S2CServerNamePacket.ID);
        assertEquals(9, C2SStayPacket.ID);
    }

    // ---- PbProtocolHandler: truncated buffer ----

    @Test
    void protocolHandler_truncatedBuffer_doesNotCrash() {
        PbProtocolHandler handler = new PbProtocolHandler();
        // Register a dummy handler for packet ID 0 so the factory is present
        handler.registerHandler(C2SJoinPacket.ID, (packet, conn) -> {});

        // Write packet ID 0 but provide an incomplete payload (missing serverPort)
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(C2SJoinPacket.ID);
        buf.writeString("mc.example.com");
        // Missing: buf.writeVarInt(25565) — the port is truncated

        // Should log a warning but NOT throw
        assertDoesNotThrow(() -> handler.handle(buf, null));
        buf.release();
    }

    @Test
    void protocolHandler_emptyBuffer_doesNotCrash() {
        PbProtocolHandler handler = new PbProtocolHandler();

        // Completely empty buffer — can't even read the packet ID
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        assertDoesNotThrow(() -> handler.handle(buf, null));
        buf.release();
    }

    // ---- PbProtocolHandler: invalid/unknown packet ID ----

    @Test
    void protocolHandler_unknownPacketId_doesNotCrash() {
        PbProtocolHandler handler = new PbProtocolHandler();

        // Write an unregistered packet ID (999)
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(999);

        assertDoesNotThrow(() -> handler.handle(buf, null));
        buf.release();
    }
}
