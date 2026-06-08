package eu.client.modules.impl.player;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketReceiveEvent;
import eu.client.mixins.accessors.PlayerPositionAccessor;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.utils.minecraft.PositionUtils;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;

@RegisterModule(name = "NoRotate", description = "Prevents the server from forcing rotations on you.", category = Module.Category.PLAYER)
public class NoRotateModule extends Module {
    public BooleanSetting inBlocks = new BooleanSetting("InBlocks", "Whether or not to stop rotations whenever inside of a block.", false);
    public BooleanSetting spoof = new BooleanSetting("Spoof", "Sends rotation packets once you have been rubberbanded.", false);

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!inBlocks.getValue() && !mc.world.getBlockState(PositionUtils.getFlooredPosition(mc.player)).isReplaceable()) return;

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket packet) {
            if (spoof.getValue()) {
                EUClient.ROTATION_MANAGER.packetRotate(packet.change().yaw(), packet.change().pitch());
                EUClient.ROTATION_MANAGER.packetRotate(mc.player.getYaw(), mc.player.getPitch());
            }

            ((PlayerPositionAccessor) (Object) packet.change()).setYaw(mc.player.getYaw());
            ((PlayerPositionAccessor) (Object) packet.change()).setPitch(mc.player.getPitch());

            packet.relatives().remove(PositionFlag.X_ROT);
            packet.relatives().remove(PositionFlag.Y_ROT);
        }
    }
}
