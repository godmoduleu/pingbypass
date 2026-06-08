package eu.client.utils.minecraft;

import eu.client.mixins.accessors.ClientWorldAccessor;
import eu.client.utils.IMinecraft;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.NetworkStateBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.*;
import net.minecraft.network.packet.s2c.common.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.state.PlayStateFactories;

public class NetworkUtils implements IMinecraft {
    public static void sendIgnoredPacket(Packet<?> packet) {
        mc.getNetworkHandler().getConnection().send(packet, null, true);
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        try (PendingUpdateManager pendingUpdateManager = ((ClientWorldAccessor)mc.world).invokeGetPendingUpdateManager().incrementSequence();){
            Packet<ServerPlayPacketListener> packet = packetCreator.predict(pendingUpdateManager.getSequence());
            mc.getNetworkHandler().sendPacket(packet);
        }
    }
}
