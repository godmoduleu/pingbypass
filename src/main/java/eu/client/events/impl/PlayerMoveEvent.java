package eu.client.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import eu.client.events.Event;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

@AllArgsConstructor @Getter @Setter
public class PlayerMoveEvent extends Event {
    private final MovementType movementType;
    private Vec3d movement;

    public double getX() {
        return movement.getX();
    }

    public void setX(double x) {
        this.movement = new Vec3d(x, movement.getY(), movement.getZ());
    }

    public double getY() {
        return movement.getY();
    }

    public void setY(double y) {
        this.movement = new Vec3d(movement.getX(), y, movement.getZ());
    }

    public double getZ() {
        return movement.getZ();
    }

    public void setZ(double z) {
        this.movement = new Vec3d(movement.getX(), movement.getY(), z);
    }
}