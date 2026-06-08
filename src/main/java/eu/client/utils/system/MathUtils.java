package eu.client.utils.system;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class MathUtils {
    public static double random(double max, double min) {
        return Math.random() * (max - min) + min;
    }

    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    public static Vec3d getVec(BlockPos pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Box getBox(Vec3d vec3d) {
        return new Box(vec3d.x, vec3d.y, vec3d.z, vec3d.x + 1, vec3d.y + 1, vec3d.z + 1);
    }

    public static Vec3d scale(Vec3d vec, float x) {
        return vec.multiply(x);
    }

    public static double interpolate(double value, double newValue, double interpolation) {
        return (value + (newValue - value) * interpolation);
    }

    public static boolean inRange(double x, double value, double range) {
        return x > value-range && x < value+range;
    }

    public static float wrapAngle(float x) {
        x = x % 360;
        if (x < 0) {
            x += 360;
        }
        return x;
    }
}
