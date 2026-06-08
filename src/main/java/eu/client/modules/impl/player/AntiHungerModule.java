package eu.client.modules.impl.player;

import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketSendEvent;
import eu.client.events.impl.UpdateMovementEvent;
import eu.client.mixins.accessors.PlayerMoveC2SPacketAccessor;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@RegisterModule(name = "AntiHunger", description = "Reduces the amount of hunger consumption.", category = Module.Category.PLAYER)
public class AntiHungerModule extends Module {
    public BooleanSetting ground = new BooleanSetting("Ground", "Modifies movement packets to decrease hunger consumption.", true);
    public BooleanSetting sprint = new BooleanSetting("Sprint", "Spoofs sprinting packets to decrease hunger consumption.", true);

    private boolean lastOnGround = false;
    private boolean ignore = false;

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (ignore && event.getPacket() instanceof PlayerMoveC2SPacket) {
            ignore = false;
            return;
        }

        if (mc.player.hasVehicle() || mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) return;

        if (event.getPacket() instanceof PlayerMoveC2SPacket packet && ground.getValue()) {
            if (mc.player.isOnGround() && mc.player.fallDistance <= 0.0 && !mc.interactionManager.isBreakingBlock()) {
                ((PlayerMoveC2SPacketAccessor) packet).setOnGround(false);
            }
        }

        if (event.getPacket() instanceof ClientCommandC2SPacket packet && sprint.getValue()) {
            if (packet.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
                event.setCancelled(true);
            }
        }
    }

    @SubscribeEvent
    public void onUpdateMovement(UpdateMovementEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isOnGround() && !lastOnGround && ground.getValue()) ignore = true;
        lastOnGround = mc.player.isOnGround();
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        lastOnGround = mc.player.isOnGround();
    }
}
