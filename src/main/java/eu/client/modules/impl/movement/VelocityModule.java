package eu.client.modules.impl.movement;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketReceiveEvent;
import eu.client.events.impl.TickEvent;
import eu.client.mixins.accessors.EntityVelocityUpdateS2CPacketAccessor;
import eu.client.mixins.accessors.Vec3dAccessor;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.CategorySetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "Velocity", description = "Modifies the amount of knockback that you receive.", category = Module.Category.MOVEMENT)
public class VelocityModule extends Module {
    public ModeSetting mode = new ModeSetting("Mode", "The method that will be used to achieve the knockback modification.", "Normal", new String[]{"Normal", "Cancel", "Grim"});
    public NumberSetting horizontal = new NumberSetting("Horizontal", "The amount of horizontal knockback that you will receive.", new ModeSetting.Visibility(mode, "Normal"), 0, 0, 100);
    public NumberSetting vertical = new NumberSetting("Vertical", "The amount of vertical knockback that you will receive.", new ModeSetting.Visibility(mode, "Normal"), 0, 0, 100);
    public BooleanSetting explosions = new BooleanSetting("Explosions", "Modifies knockback received from explosions.", true);
    public BooleanSetting pause = new BooleanSetting("Pause", "Pauses the velocity for a certain duration whenever you get rubberbanded.", new ModeSetting.Visibility(mode, "Cancel", "Grim"), true);

    public CategorySetting antiPushCategory = new CategorySetting("AntiPush", "Prevents certain things from pushing you.");
    public BooleanSetting antiPush = new BooleanSetting("AntiPush", "Entities", "Prevents other entities from pushing you.", new CategorySetting.Visibility(antiPushCategory), true);
    public BooleanSetting antiLiquidPush = new BooleanSetting("AntiLiquidPush", "Liquids", "Prevents liquids from pushing you.", new CategorySetting.Visibility(antiPushCategory), false);
    public BooleanSetting antiBlockPush = new BooleanSetting("AntiBlockPush", "Blocks", "Prevents you from being pushed outside of blocks.", new CategorySetting.Visibility(antiPushCategory), true);
    public BooleanSetting antiFishingRod = new BooleanSetting("AntiFishingRod", "FishingRods", "Prevents fishing rods from pushing you.", new CategorySetting.Visibility(antiPushCategory), false);

    private boolean cancel;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        if (!cancel) return;

        if (mode.getValue().equalsIgnoreCase("Grim") && (!pause.getValue() || EUClient.SERVER_MANAGER.getSetbackTimer().hasTimeElapsed(100L))) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), EUClient.ROTATION_MANAGER.getServerYaw(), EUClient.ROTATION_MANAGER.getServerPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mc.player.isCrawling() ? mc.player.getBlockPos() : mc.player.getBlockPos().up(), Direction.DOWN));
        }

        cancel = false;
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.player == null) return;

        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() != mc.player.getId()) return;

            switch (mode.getValue()) {
                case "Normal" -> {
                    ((EntityVelocityUpdateS2CPacketAccessor) packet).setVelocityX((int) (((packet.getVelocityX() / 8000.0 - mc.player.getVelocity().x) * (horizontal.getValue().doubleValue() / 100.0)) * 8000 + mc.player.getVelocity().x * 8000));
                    ((EntityVelocityUpdateS2CPacketAccessor) packet).setVelocityY((int) (((packet.getVelocityY() / 8000.0 - mc.player.getVelocity().y) * (vertical.getValue().doubleValue() / 100.0)) * 8000 + mc.player.getVelocity().y * 8000));
                    ((EntityVelocityUpdateS2CPacketAccessor) packet).setVelocityZ((int) (((packet.getVelocityZ() / 8000.0 - mc.player.getVelocity().z) * (horizontal.getValue().doubleValue() / 100.0)) * 8000 + mc.player.getVelocity().z * 8000));
                }
                case "Cancel" -> {
                    if (pause.getValue() && !EUClient.SERVER_MANAGER.getSetbackTimer().hasTimeElapsed(100L)) return;

                    event.setCancelled(true);
                }
                case "Grim" -> {
                    if (pause.getValue() && !EUClient.SERVER_MANAGER.getSetbackTimer().hasTimeElapsed(100L)) return;

                    event.setCancelled(true);
                    cancel = true;
                }
            }
        }

        if (event.getPacket() instanceof ExplosionS2CPacket packet && explosions.getValue()) {
            switch (mode.getValue()) {
                case "Normal" -> {
                    if (packet.playerKnockback().isPresent()) ((Vec3dAccessor) packet.playerKnockback().get()).setX((float) (packet.playerKnockback().get().getX() * (horizontal.getValue().doubleValue() / 100.0)));
                    if (packet.playerKnockback().isPresent()) ((Vec3dAccessor) packet.playerKnockback().get()).setY((float) (packet.playerKnockback().get().getY() * (vertical.getValue().doubleValue() / 100.0)));
                    if (packet.playerKnockback().isPresent()) ((Vec3dAccessor) packet.playerKnockback().get()).setZ((float) (packet.playerKnockback().get().getZ() * (horizontal.getValue().doubleValue() / 100.0)));
                }
                case "Cancel" -> {
                    if (pause.getValue() && !EUClient.SERVER_MANAGER.getSetbackTimer().hasTimeElapsed(100L)) return;

                    event.setCancelled(true);
                }
                case "Grim" -> {
                    if (pause.getValue() && !EUClient.SERVER_MANAGER.getSetbackTimer().hasTimeElapsed(100L)) return;

                    event.setCancelled(true);
                    cancel = true;
                }
            }

            if (event.isCancelled()) {
                mc.executeSync(() -> {
                    Vec3d vec3d = packet.center();
                    mc.world.playSound(vec3d.getX(), vec3d.getY(), vec3d.getZ(), packet.explosionSound().value(), SoundCategory.BLOCKS, 4.0F, (1.0F + (mc.world.random.nextFloat() - mc.world.random.nextFloat()) * 0.2F) * 0.7F, false);
                    mc.world.addParticle(packet.explosionParticle(), vec3d.getX(), vec3d.getY(), vec3d.getZ(), 1.0, 0.0, 0.0);
                });
            }
        }
    }

    @Override
    public String getMetaData() {
        if (mode.getValue().equalsIgnoreCase("Cancel")) return "0%, 0%";
        if (mode.getValue().equalsIgnoreCase("Grim")) return "Grim";
        return horizontal.getValue().intValue() + "%, " + vertical.getValue().intValue() + "%";
    }
}