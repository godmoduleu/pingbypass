package eu.client.modules.impl.miscellaneous;

import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketSendEvent;
import eu.client.mixins.accessors.CustomPayloadC2SPacketAccessor;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.StringSetting;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;

@RegisterModule(name = "Handshake", description = "Spoofs your client handshake to make the server think that you are playing on a different client.", category = Module.Category.MISCELLANEOUS)
public class HandshakeModule extends Module {
    public StringSetting brand = new StringSetting("Brand", "The brand that the server will think you are playing on.", "vanilla");

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof CustomPayloadC2SPacket packet) {
            if (!packet.payload().getId().id().equals(BrandCustomPayload.ID.id())) return;
            ((CustomPayloadC2SPacketAccessor) (Object) packet).setPayload(new BrandCustomPayload(brand.getValue()));
        }
    }
}
