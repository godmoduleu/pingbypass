package eu.client.utils.animations;

import net.minecraft.util.math.MathHelper;

public class Easing {
    public static float ease(float x, Method method) {
        return switch (method) {
            case EASE_IN_SINE -> easeInSine(x);
            case EASE_OUT_SINE -> easeOutSine(x);
            case EASE_IN_CUBIC -> easeInCubic(x);
            case EASE_OUT_CUBIC -> easeOutCubic(x);
            case EASE_IN_QUAD -> easeInQuad(x);
            case EASE_OUT_QUAD -> easeOutQuad(x);
            case EASE_IN_QUART -> easeInQuart(x);
            case EASE_OUT_QUART -> easeOutQuart(x);
            case EASE_IN_ELASTIC -> easeInElastic(x);
            case EASE_OUT_ELASTIC -> easeOutElastic(x);
            case EASE_IN_OUT_ELASTIC -> easeInOutElastic(x);
            default -> linear(x);
        };
    }

    public static float toDelta(long start, int length) {
        return MathHelper.clamp(toDelta(start)/(float) length, 0.0f, 1.0f);
    }

    public static long toDelta(long start) {
        return System.currentTimeMillis() - start;
    }

    private static float linear(float x) {
        return x;
    }

    private static float easeInSine(float x) {
        return (float) (1 - Math.cos((x * Math.PI) / 2));
    }

    private static float easeOutSine(float x) {
        return (float) Math.sin((x * Math.PI) / 2);
    }

    private static float easeInCubic(float x) {
        return x * x * x;
    }

    private static float easeOutCubic(float x) {
        return (float) (1 - Math.pow(1 - x, 3));
    }

    private static float easeInQuad(float x) {
        return x * x;
    }

    private static float easeOutQuad(float x) {
        return 1 - (1 - x) * (1 - x);
    }

    private static float easeInQuart(float x) {
        return x * x * x * x;
    }

    private static float easeOutQuart(float x) {
        return (float) (1 - Math.pow(1 - x, 4));
    }

    private static float easeInElastic(float x) {
        float c4 = (float) (2 * Math.PI) / 3;
        return x == 0 ? 0 : (float) (x == 1 ? 1 : -Math.pow(2, 10 * x - 10) * Math.sin((x * 10 - 10.75) * c4));
    }

    private static float easeOutElastic(float x) {
        float c4 = (float) (2 * Math.PI) / 3;
        return x == 0 ? 0 : (float) (x == 1 ? 1 : Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1);
    }

    private static float easeInOutElastic(float x) {
        float c5 = (float) (2 * Math.PI) / 4.5f;
        return x == 0 ? 0 : (float) (x == 1 ? 1 : x < 0.5 ? -(Math.pow(2, 20 * x - 10) * Math.sin((20 * x - 11.125) * c5)) / 2 : (Math.pow(2, -20 * x + 10) * Math.sin((20 * x - 11.125) * c5)) / 2 + 1);
    }

    public enum Method {
        LINEAR,
        EASE_IN_SINE,
        EASE_OUT_SINE,
        EASE_IN_CUBIC,
        EASE_OUT_CUBIC,
        EASE_IN_QUAD,
        EASE_OUT_QUAD,
        EASE_IN_QUART,
        EASE_OUT_QUART,
        EASE_IN_ELASTIC,
        EASE_OUT_ELASTIC,
        EASE_IN_OUT_ELASTIC
    }
}
