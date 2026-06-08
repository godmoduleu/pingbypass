package eu.client.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import eu.client.events.Event;
import net.minecraft.network.packet.Packet;

@Getter @AllArgsConstructor
public class PacketSendEvent extends Event {
    private final Packet<?> packet;

    @Getter @AllArgsConstructor
    public static class Post extends Event {
        private final Packet<?> packet;
    }
}
