package eu.client.modules.impl.movement;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerMoveEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.utils.minecraft.MovementUtils;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2d;

@RegisterModule(name = "Accelerate", description = "Gives you more precise movement instantly.", category = Module.Category.MOVEMENT)
public class AccelerateModule extends Module {
    public BooleanSetting air = new BooleanSetting("Air", "Increases your speed while off ground.", true);
    public BooleanSetting speedInWater = new BooleanSetting("SpeedInWater", "Increases your speed while in water.", false);

    @SubscribeEvent
    public void onPlayerMove(PlayerMoveEvent event) {
        if(getNull() || (EUClient.MODULE_MANAGER.getModule(HoleSnapModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(HoleSnapModule.class).hole != null) || EUClient.MODULE_MANAGER.getModule(SpeedModule.class).isToggled()) return;

        if (mc.player.fallDistance >= 5.0f || mc.player.isSneaking() || mc.player.isClimbing() || mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.COBWEB || mc.player.getAbilities().flying || mc.player.isGliding())
            return;

        if(!mc.player.isOnGround() && !air.getValue()) return;

        if((mc.player.isTouchingWater() || mc.player.isInLava()) && !speedInWater.getValue()) return;

        Vector2d velocity = MovementUtils.forward(MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED));
        event.setMovement(new Vec3d(velocity.x, event.getMovement().getY(), event.getMovement().getZ()));
        event.setMovement(new Vec3d(event.getMovement().getX(), event.getMovement().getY(), velocity.y));
        event.setCancelled(true);
    }
}
