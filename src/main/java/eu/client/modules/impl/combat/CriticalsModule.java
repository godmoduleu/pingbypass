package eu.client.modules.impl.combat;

import eu.client.events.SubscribeEvent;
import eu.client.events.impl.AttackEntityEvent;
import eu.client.mixins.accessors.ClientPlayerEntityAccessor;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.ModeSetting;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@RegisterModule(name = "Criticals", description = "Makes every one of your hits a critical.", category = Module.Category.COMBAT, proxyEnhanced = true)
public class CriticalsModule extends Module {
    public ModeSetting mode = new ModeSetting("Mode", "The method that will be used to achieve the critical hits.", "Packet", new String[]{"Packet", "Strict", "Grim"});

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (shouldRunOnProxy()) return;
        if (mc.player == null || mc.world == null) return;
        if (event.getTarget() == null || event.getTarget() instanceof EndCrystalEntity) return;

        if (mc.player.isOnGround() || mc.player.getAbilities().flying || mode.getValue().equalsIgnoreCase("Grim") && !mc.player.isInLava() && !mc.player.isSubmergedInWater()) {
            switch (mode.getValue()) {
                case "Packet" -> {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.05, mc.player.getZ(), false, mc.player.horizontalCollision));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, mc.player.horizontalCollision));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.03, mc.player.getZ(), false, mc.player.horizontalCollision));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, mc.player.horizontalCollision));
                }
                case "Strict" -> {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.11, mc.player.getZ(), false, mc.player.horizontalCollision));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.1100013579, mc.player.getZ(), false, mc.player.horizontalCollision));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0000013579, mc.player.getZ(), false, mc.player.horizontalCollision));
                }
                case "Grim" -> {
                    if (!mc.player.isOnGround()) {
                        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() - 0.000001, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), false, mc.player.horizontalCollision));
                    }
                }
            }

            ((ClientPlayerEntityAccessor) mc.player).setLastOnGround(false);
            mc.player.addCritParticles(event.getTarget());
        }
    }

    @Override
    public String getMetaData() {
        return mode.getValue();
    }
}
