package eu.client.utils.rotations;

import lombok.Getter;
import lombok.Setter;
import eu.client.modules.Module;

@Getter @Setter
public class Rotation {
    private float yaw, pitch;
    private final Module module;
    private final int priority;
    private long time;

    public Rotation(float yaw, float pitch, int priority) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.module = null;
        this.priority = priority;
        this.time = System.currentTimeMillis();
    }

    public Rotation(float yaw, float pitch, Module module, int priority) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.module = module;
        this.priority = priority;
        this.time = System.currentTimeMillis();
    }
}