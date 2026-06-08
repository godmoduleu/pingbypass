package eu.client.utils.minecraft;

import eu.client.events.impl.PlayerMoveEvent;
import eu.client.mixins.accessors.Vec3dAccessor;
import eu.client.utils.IMinecraft;
import eu.client.utils.rotations.RotationUtils;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2d;

public class MovementUtils implements IMinecraft {
    public static double DEFAULT_SPEED = 0.2873;

    public static Vector2d forward(double speed) {
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward == 0.0f && sideways == 0.0f) return new Vector2d(0, 0);
        if (forward != 0.0f) {
            if (sideways >= 1.0f) {
                yaw += ((forward > 0.0f) ? -45 : 45);
                sideways = 0.0f;
            } else if (sideways <= -1.0f) {
                yaw += ((forward > 0.0f) ? 45 : -45);
                sideways = 0.0f;
            }

            if (forward > 0.0f) forward = 1.0f;
            else if (forward < 0.0f) forward = -1.0f;
        }

        double motionX = Math.cos(Math.toRadians(yaw + 90.0f));
        double motionZ = Math.sin(Math.toRadians(yaw + 90.0f));

        return new Vector2d(forward * speed * motionX + sideways * speed * motionZ, forward * speed * motionZ - sideways * speed * motionX);
    }

    public static double[] straightForward(double speed) {
        return new double[]{speed * Math.cos(Math.toRadians(mc.player.getYaw() + 90.0f)), speed * Math.sin(Math.toRadians(mc.player.getYaw() + 90.0f))};
    }

    public static double getPotionSpeed(double speed) {
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) speed *= 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) speed /= 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);

        return speed;
    }

    public static double getPotionJump(double jump) {
        if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) jump += (mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1f;

        return jump;
    }

    public static boolean isMoving() {
        return mc.player.sidewaysSpeed != 0.0f || mc.player.forwardSpeed != 0.0f;
    }

    public static void moveTowards(PlayerMoveEvent event, Vec3d vec3d, double speed) {
        double angle = Math.toRadians(RotationUtils.getRotations(vec3d)[0]);
        double x = -Math.sin(angle) * speed;
        double z = Math.cos(angle) * speed;
        double[] difference = new double[] {vec3d.x - mc.player.getX(), vec3d.z - mc.player.getZ()};

        event.setMovement(new Vec3d(Math.abs(x) < Math.abs(difference[0]) ? x : difference[0], event.getMovement().getY(), event.getMovement().getZ()));
        event.setMovement(new Vec3d(event.getMovement().getX(), event.getMovement().getY(), Math.abs(z) < Math.abs(difference[1]) ? z : difference[1]));
        ((Vec3dAccessor) mc.player.getVelocity()).setX(0);
        ((Vec3dAccessor) mc.player.getVelocity()).setZ(0);
        event.setCancelled(true);
    }
}