package eu.client.mixins.accessors;

import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Vec3d.class)
public interface Vec3dAccessor {
    @Accessor("x") @Mutable
    void setX(double x);

    @Accessor("y") @Mutable
    void setY(double y);

    @Accessor("z") @Mutable
    void setZ(double z);
}
