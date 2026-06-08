package eu.client.utils.minecraft;

import eu.client.utils.IMinecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PositionUtils implements IMinecraft {
    public static BlockPos getFlooredPosition(Entity entity) {
        return new BlockPos(entity.getBlockX(), MathHelper.floor(((entity.getY() - Math.floor(entity.getY())) > 0.8) ? (Math.floor(entity.getY()) + 1.0) : Math.floor(entity.getY())), entity.getBlockZ());
    }

    public static Box getRadius(Entity entity, double radius) {
        return new Box(MathHelper.floor(entity.getX() - radius), MathHelper.floor(entity.getY() - radius), MathHelper.floor(entity.getZ() - radius), MathHelper.floor(entity.getX() + radius), MathHelper.floor(entity.getY() + radius), MathHelper.floor(entity.getZ() + radius));
    }

    public static Box extrapolate(PlayerEntity entity, int ticks) {
        if (entity == null) return null;

        double deltaX = entity.getX() - entity.prevX;
        double deltaZ = entity.getZ() - entity.prevZ;

        double motionX = 0;
        double motionZ = 0;

        for (double i = 1; i <= ticks; i = i + 0.5) {
            if (!mc.world.canCollide(entity, entity.getBoundingBox().offset(new Vec3d(deltaX * i, 0, deltaZ * i)))) {
                motionX = deltaX * i;
                motionZ = deltaZ * i;
            } else {
                break;
            }
        }

        Vec3d vec3d = new Vec3d(motionX, 0, motionZ);
        if (vec3d == null) return null;

        return entity.getBoundingBox().offset(vec3d);
    }
}
