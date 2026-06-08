package eu.client.utils.animations;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;

@Getter
@Setter
public class Animation {
    private float current, prev;
    private long startTime;
    private Easing.Method easing;

    private final int duration;

    public Animation(int duration, Easing.Method easing) {
        this.prev = 0;
        this.current = 0;
        this.duration = duration;
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
    }

    public Animation(float prev, float current, int duration, Easing.Method easing) {
        this.prev = prev;
        this.current = current;
        this.duration = duration;
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
    }

    public float get() {
        return MathHelper.lerp(Easing.ease(Easing.toDelta(startTime, duration), easing), prev, current);
    }

    public float get(float current) {
        float lerp = MathHelper.lerp(Easing.ease(Easing.toDelta(startTime, duration), easing), prev, this.current);
        if (this.current != current) {
            prev = lerp;
            this.current = current;
            startTime = System.currentTimeMillis();
        }

        return lerp;
    }
}
