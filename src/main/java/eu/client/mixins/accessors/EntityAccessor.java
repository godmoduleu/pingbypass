package eu.client.mixins.accessors;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Invoker("movementInputToVelocity")
    static Vec3d invokeMovementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        throw new AssertionError();
    }
}
