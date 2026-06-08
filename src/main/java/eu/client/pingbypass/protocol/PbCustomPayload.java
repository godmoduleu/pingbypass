package eu.client.pingbypass.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;

public record PbCustomPayload(byte[] data) implements CustomPayload {
    public static final Identifier CHANNEL = Identifier.of("pingbypass", "pingbypass");
    public static final Id<PbCustomPayload> ID = new Id<>(CHANNEL);

    public static final int C2S_JOIN = 0;
    public static final int S2C_PASSWORD_REQUEST = 1;
    public static final int C2S_PASSWORD = 2;
    public static final int S2C_ERROR = 7;

    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, PbCustomPayload> CODEC = new PacketCodec<>() {
        @Override
        public PbCustomPayload decode(net.minecraft.network.RegistryByteBuf buf) {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new PbCustomPayload(bytes);
        }

        @Override
        public void encode(net.minecraft.network.RegistryByteBuf buf, PbCustomPayload payload) {
            buf.writeBytes(payload.data());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public PacketByteBuf toBuf() {
        return new PacketByteBuf(Unpooled.wrappedBuffer(data));
    }

    public static PbCustomPayload fromPacket(PbPacket packet) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(packet.getPacketId());
        packet.write(buf);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        return new PbCustomPayload(bytes);
    }


    public static CustomPayloadC2SPacket createC2SPacket(PbPacket packet) {
        return new CustomPayloadC2SPacket(fromPacket(packet));
    }

    public static PbCustomPayload passwordRequest() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(S2C_PASSWORD_REQUEST);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        return new PbCustomPayload(bytes);
    }

    public static PbCustomPayload error(String message) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(S2C_ERROR);
        buf.writeString(message);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        return new PbCustomPayload(bytes);
    }
}
