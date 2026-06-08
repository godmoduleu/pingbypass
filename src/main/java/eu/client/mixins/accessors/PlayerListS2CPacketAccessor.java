package eu.client.mixins.accessors;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.EnumSet;
import java.util.List;

@Mixin(PlayerListS2CPacket.class)
public interface PlayerListS2CPacketAccessor {
    @Accessor("actions")
    @Mutable
    void setActions(EnumSet<PlayerListS2CPacket.Action> actions);

    @Accessor("entries")
    @Mutable
    void setEntries(List<PlayerListS2CPacket.Entry> entries);
}
